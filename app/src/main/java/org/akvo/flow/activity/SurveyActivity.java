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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
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
import org.akvo.flow.ui.fragment.DrawerFragment;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.Prefs;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

public class SurveyActivity extends ActionBarActivity implements RecordListListener,
        DrawerFragment.UserListener, DrawerFragment.SurveyListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();

    private static final int REQUEST_ADD_USER = 0;

    // Argument to be passed to list/map fragments
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";

    public static final String FRAGMENT_DATAPOINTS = "datapoints_fragment";

    private SurveyDbAdapter mDatabase;
    private SurveyGroup mSurveyGroup;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerFragment mDrawer;
    private CharSequence mDrawerTitle, mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mTitle = mDrawerTitle = getString(R.string.app_name);

        // Init navigation drawer
        mDrawer = (DrawerFragment)getSupportFragmentManager().findFragmentByTag("f");
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mDrawer.setModeSurveys();
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
        getSupportActionBar().setIcon(R.drawable.ic_menu_white_48dp);

        // Automatically select the survey
        SurveyGroup sg = mDatabase.getSurveyGroup(FlowApp.getApp().getSurveyGroupId());
        if (sg != null) {
            onSurveySelected(sg);
        } else {
            mDrawerLayout.openDrawer(Gravity.START);
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
                DatapointsFragment.instantiate(mSurveyGroup), FRAGMENT_DATAPOINTS).commit();

        if (!Prefs.getBoolean(this, Prefs.KEY_SETUP, false)) {
            startActivityForResult(new Intent(this, AddUserActivity.class)
                            .putExtra(AddUserActivity.EXTRA_FIRST_RUN, true),
                    REQUEST_ADD_USER);
        }

        startServices();
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ADD_USER:
                mDrawer.setModeSurveys();

                if (resultCode == RESULT_OK) {
                    Prefs.setBoolean(this, Prefs.KEY_SETUP, true);
                } else if (!Prefs.getBoolean(this, Prefs.KEY_SETUP, false)) {
                    finish();
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
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
    public void onNewUser() {
        startActivityForResult(new Intent(this, AddUserActivity.class), REQUEST_ADD_USER);
    }

    @Override
    public void onUserSelected(User user) {
        FlowApp.getApp().setUser(user);
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showItems = !mDrawerLayout.isDrawerOpen(Gravity.START) && mSurveyGroup != null;
        for (int i=0; i<menu.size(); i++) {
            menu.getItem(i).setVisible(showItems);
        }
        return super.onPrepareOptionsMenu(menu);
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
        final User user = FlowApp.getApp().getUser();
        // Ensure user is logged in
        if (user == null) {
            Toast.makeText(SurveyActivity.this, R.string.mustselectuser,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Non-monitored surveys display the form directly
        if (!mSurveyGroup.isMonitored()) {
            final String formId = mSurveyGroup.getRegisterSurveyId();
            if (!mDatabase.getSurvey(formId).isHelpDownloaded()) {
                Toast.makeText(this, R.string.error_missing_cascade, Toast.LENGTH_LONG).show();
                return;
            }

            long formInstanceId;
            boolean readOnly = false;
            Cursor c = mDatabase.getSurveyInstances(surveyedLocaleId);
            if (c.moveToFirst()) {
                formInstanceId = c.getLong(c.getColumnIndexOrThrow(SurveyDbAdapter.SurveyInstanceColumns._ID));
                int status = c.getInt(c.getColumnIndexOrThrow(SurveyDbAdapter.SurveyInstanceColumns.STATUS));
                readOnly = status != SurveyDbAdapter.SurveyInstanceStatus.SAVED;
            } else {
                formInstanceId = mDatabase.createSurveyRespondent(formId, user, surveyedLocaleId);
            }
            c.close();

            Intent i = new Intent(this, FormActivity.class);
            i.putExtra(ConstantUtil.USER_ID_KEY, user.getId());
            i.putExtra(ConstantUtil.SURVEY_ID_KEY, formId);
            i.putExtra(ConstantUtil.SURVEY_GROUP, mSurveyGroup);
            i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, surveyedLocaleId);
            i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, formInstanceId);
            i.putExtra(ConstantUtil.READONLY_KEY, readOnly);
            startActivity(i);
        } else {
            // Display form list and history
            Intent intent = new Intent(this, RecordActivity.class);
            Bundle extras = new Bundle();
            extras.putSerializable(RecordActivity.EXTRA_SURVEY_GROUP, mSurveyGroup);
            extras.putString(RecordActivity.EXTRA_RECORD_ID, surveyedLocaleId);
            intent.putExtras(extras);
            startActivity(intent);
        }
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
