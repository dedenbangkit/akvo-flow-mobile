/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.ExceptionReportingService;
import org.akvo.flow.service.LocationService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.service.TimeCheckService;
import org.akvo.flow.ui.fragment.DatapointsFragment;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.ui.view.NavigationDrawer;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

public class SurveyActivity extends ActionBarActivity implements RecordListListener,
        NavigationDrawer.OnUserSelectedListener, NavigationDrawer.OnSurveySelectedListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();
    
    // Argument to be passed to list/map fragments
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";

    public static final String FRAGMENT_DATAPOINTS = "datapoints_fragment";

    private SurveyDbAdapter mDatabase;
    private SurveyGroup mSurveyGroup;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationDrawer mDrawer;
    private CharSequence mDrawerTitle, mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = (NavigationDrawer) findViewById(R.id.left_drawer);

        mTitle = mDrawerTitle = getTitle();

        // Init navigation drawer
        mDrawer.init(getSupportLoaderManager(), mDatabase, this, this);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Automatically select the survey and user
        SurveyGroup sg = mDatabase.getSurveyGroup(FlowApp.getApp().getSurveyGroupId());
        if (sg != null) {
            onSurveySelected(sg);
        }

        showDatapointsFragment();

        startServices();
    }

    // TODO: Add login, survey, datapoints mode selection
    private void showDatapointsFragment() {
        Fragment f = DatapointsFragment.instantiate(mSurveyGroup);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, f, FRAGMENT_DATAPOINTS).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDrawer.load();
        registerReceiver(mSurveysSyncReceiver,
                new IntentFilter(getString(R.string.action_surveys_sync)));
    }
    
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mSurveysSyncReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    private void startServices() {
        if (!StatusUtil.hasExternalStorage()) {
            ViewUtil.showConfirmDialog(R.string.checksd, R.string.sdmissing, this,
                    false,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SurveyActivity.this.finish();
                        }
                    },
                    null);
        } else {
            startService(new Intent(this, SurveyDownloadService.class));
            startService(new Intent(this, LocationService.class));
            startService(new Intent(this, BootstrapService.class));
            startService(new Intent(this, ExceptionReportingService.class));
            startService(new Intent(this, DataSyncService.class));
            startService(new Intent(this, ApkUpdateService.class));
            startService(new Intent(this, TimeCheckService.class));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String surveyedLocaleId = intent.getDataString();
            onRecordSelected(surveyedLocaleId);
        }
    }

    @Override
    public void onSurveySelected(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        setTitle(mSurveyGroup.getName());

        FlowApp.getApp().setSurveyGroupId(mSurveyGroup.getId());

        DatapointsFragment f = (DatapointsFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_DATAPOINTS);
        if (f != null) {
            f.refresh(mSurveyGroup);
        }
        supportInvalidateOptionsMenu();
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onUserSelected(User user) {
        FlowApp.getApp().setUser(user);
        mDatabase.savePreference(ConstantUtil.LAST_USER_SETTING_KEY,
                String.valueOf(user.getId()));// Save the last id for future sessions
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If the nav drawer is open, don't inflate the menu items.
        if (!mDrawerLayout.isDrawerOpen(mDrawer) && mSurveyGroup != null) {
            //getMenuInflater().inflate(R.menu.survey_activity, menu);
            return super.onCreateOptionsMenu(menu);
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRecordSelected(String surveyedLocaleId) {
        // Ensure user is logged in
        if (FlowApp.getApp().getUser() == null) {
            Toast.makeText(SurveyActivity.this, R.string.mustselectuser,
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Start SurveysActivity, sending SurveyGroup + Record
        SurveyedLocale record = mDatabase.getSurveyedLocale(surveyedLocaleId);
        Intent intent = new Intent(this, RecordActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable(RecordActivity.EXTRA_SURVEY_GROUP, mSurveyGroup);
        extras.putString(RecordActivity.EXTRA_RECORD_ID, record.getId());
        intent.putExtras(extras);
        startActivity(intent);
    }

    /**
     * BroadcastReceiver to notify of surveys synchronisation. This should be
     * fired from SurveyDownloadService.
     */
    private BroadcastReceiver mSurveysSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Surveys have been synchronised. Refreshing data...");
            mDrawer.load();
        }
    };
}
