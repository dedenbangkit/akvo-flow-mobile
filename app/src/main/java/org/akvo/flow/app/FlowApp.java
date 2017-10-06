/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
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
import android.content.res.Configuration;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.UserColumns;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.User;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerApplicationComponent;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.util.logging.LoggingHelper;

import java.util.Locale;

import javax.inject.Inject;

public class FlowApp extends Application {
    private static FlowApp app;// Singleton

    //TODO: use shared pref?
    private Locale mLocale;

    private User mUser;
    private Prefs prefs;

    private ApplicationComponent applicationComponent;

    @Inject
    LoggingHelper loggingHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeInjector();
        prefs = new Prefs(getApplicationContext());
        initLogging();
        init();
        startUpdateService();
        app = this;
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

    public static FlowApp getApp() {
        return app;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // This config will contain system locale. We need a workaround
        // to enable our custom locale again. Note that this approach
        // is not very 'clean', but Android makes it really hard to
        // customize an application wide locale.
        String languageCode = loadLocalePref();
        if (!TextUtils.isEmpty(languageCode)) {
            mLocale = new Locale(languageCode);
        }
        if (mLocale != null && !mLocale.getLanguage().equalsIgnoreCase(
                newConfig.locale.getLanguage())) {
            // Re-enable our custom locale, using this newConfig reference
            newConfig.locale = mLocale;
            Locale.setDefault(mLocale);
            getBaseContext().getResources().updateConfiguration(newConfig, null);
        }
    }

    private void init() {
        loadLastUser();
    }

    public void setUser(User user) {
        mUser = user;
        prefs.setLong(Prefs.KEY_USER_ID, mUser != null ? mUser.getId() : -1);
    }

    public User getUser() {
        return mUser;
    }

    public String getAppLanguageCode() {
        return mLocale.getLanguage();
    }

    public String getAppDisplayLanguage() {
        String lang = mLocale.getDisplayLanguage();
        if (!TextUtils.isEmpty(lang)) {
            // Ensure the first letter is upper case
            char[] strArray = lang.toCharArray();
            strArray[0] = Character.toUpperCase(strArray[0]);
            lang = new String(strArray);
        }
        return lang;
    }

    /**
     * Checks if the user preference to persist logged-in users is set and, if
     * so, loads the last logged-in user from the DB
     */
    private void loadLastUser() {
        SurveyDbAdapter database = new SurveyDbAdapter(FlowApp.this);
        database.open();

        // Consider the app set up if the DB contains users. This is relevant for v2.2.0 app upgrades
        if (!prefs.getBoolean(Prefs.KEY_SETUP, false)) {
            prefs.setBoolean(Prefs.KEY_SETUP, database.getUsers().getCount() > 0);
        }

        long id = prefs.getLong(Prefs.KEY_USER_ID, -1);
        if (id != -1) {
            Cursor cur = database.getUser(id);
            if (cur.moveToFirst()) {
                String userName = cur.getString(cur.getColumnIndexOrThrow(UserColumns.NAME));
                mUser = new User(id, userName);
                cur.close();
            }
        }

        database.close();
    }

    public void setAppLanguage(String language) {
        // Override system locale
        mLocale = new Locale(language);
        Locale.setDefault(mLocale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = mLocale;
        getBaseContext().getResources().updateConfiguration(config, null);
    }

    @Nullable
    private String loadLocalePref() {
        return prefs.getString(Prefs.KEY_LOCALE, null);
    }
}
