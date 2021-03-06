/*
 *  Copyright (C) 2013-2018 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.leakcanary.LeakCanary;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.interactor.setup.SaveSetup;
import org.akvo.flow.domain.interactor.setup.SetUpParams;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerApplicationComponent;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.service.FileChangeTrackingService;
import org.akvo.flow.util.logging.LoggingHelper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class FlowApp extends Application {

    @Inject
    LoggingHelper loggingHelper;

    @Inject
    Prefs prefs;

    @Inject
    @Named("getSelectedUser")
    UseCase getSelectedUser;

    private ApplicationComponent applicationComponent;

    @Inject
    @Named("saveSetup")
    UseCase saveSetup;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        if (BuildConfig.DEBUG) {
            MultiDex.install(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        installLeakCanary();
        initializeInjector();
        initFabric();
        initLogging();
        updateLocale();
        startUpdateService();
        startBootstrapFolderTracker();
        updateLoggingInfo();
        saveConfig();
    }

    private void installLeakCanary() {
        LeakCanary.install(this);
    }

    private void initFabric() {
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
                .disabled(true)
                .build();
        Fabric.with(this, new Crashlytics.Builder().core(crashlyticsCore).build());
    }

    private void saveConfig() {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveSetup.PARAM_SETUP,
                new SetUpParams(BuildConfig.API_KEY, BuildConfig.AWS_ACCESS_KEY_ID,
                        BuildConfig.AWS_BUCKET, BuildConfig.AWS_SECRET_KEY,
                        BuildConfig.INSTANCE_URL, BuildConfig.SERVER_BASE,
                        BuildConfig.SIGNING_KEY));
        saveSetup.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

        }, params);
    }

    private void startBootstrapFolderTracker() {
        FileChangeTrackingService.scheduleVerifier(this);
    }

    private void startUpdateService() {
        ApkUpdateService.scheduleFirstTask(this);
    }

    private void initializeInjector() {
        this.applicationComponent =
                DaggerApplicationComponent.builder().applicationModule(new ApplicationModule(this))
                        .build();
        this.applicationComponent.inject(this);
    }

    public ApplicationComponent getApplicationComponent() {
        return this.applicationComponent;
    }

    private void initLogging() {
        loggingHelper.init();
    }

    private void updateLoggingInfo() {
        getSelectedUser.execute(new DefaultObserver<User>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onNext(User user) {
                String deviceId = prefs.getString(Prefs.KEY_DEVICE_IDENTIFIER, null);
                loggingHelper.initLoginData(user.getName(), deviceId);
            }
        }, null);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // This config will contain system locale. We need a workaround
        // to enable our custom locale again. Note that this approach
        // is not very 'clean', but Android makes it really hard to
        // customize an application wide locale.
        Locale savedLocale = getSavedLocale();
        if (localeNeedsUpdating(savedLocale, newConfig.locale)) {
            // Re-enable our custom locale, using this newConfig reference
            Locale.setDefault(savedLocale);
            updateConfiguration(savedLocale, newConfig);
        }
    }

    private void updateLocale() {
        Locale savedLocale = getSavedLocale();
        Locale currentLocale = Locale.getDefault();
        if (localeNeedsUpdating(savedLocale, currentLocale)) {
            Locale.setDefault(savedLocale);
            updateConfiguration(savedLocale, new Configuration());
        }
    }

    private boolean localeNeedsUpdating(Locale savedLocale, Locale currentLocale) {
        return savedLocale != null && currentLocale != null && !currentLocale.getLanguage()
                .equalsIgnoreCase(savedLocale.getLanguage());
    }

    private void updateConfiguration(Locale savedLocale, Configuration config) {
        config.locale = savedLocale;
        getBaseContext().getResources().updateConfiguration(config, null);
    }

    @Nullable
    private Locale getSavedLocale() {
        String languageCode = loadLocalePref();
        Locale savedLocale = null;
        if (!TextUtils.isEmpty(languageCode)) {
            savedLocale = new Locale(languageCode);
        }
        return savedLocale;
    }

    @Nullable
    private String loadLocalePref() {
        return prefs.getString(Prefs.KEY_LOCALE, null);
    }
}
