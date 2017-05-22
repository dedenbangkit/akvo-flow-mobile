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

package org.akvo.flow.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.SurveyInstanceStatus;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.adapter.RecordTabsAdapter;
import org.akvo.flow.ui.fragment.FormListFragment.SurveyListListener;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

public class RecordActivity extends BackActivity implements SurveyListListener {

    private static final int REQUEST_FORM = 0;

    private User mUser;
    private SurveyedLocale mRecord;
    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;

    @Inject
    Navigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_activity);
        initializeInjector();
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        RecordTabsAdapter recordTabsAdapter = new RecordTabsAdapter(getSupportFragmentManager(),
                getResources().getStringArray(R.array.record_tabs));
        viewPager.setAdapter(recordTabsAdapter);
        mDatabase = new SurveyDbAdapter(this);

        mSurveyGroup = (SurveyGroup) getIntent().getSerializableExtra(
                ConstantUtil.SURVEY_GROUP_EXTRA);
        setTitle(mSurveyGroup.getName());

        setupToolBar();
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    protected ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();

        mUser = FlowApp.getApp().getUser();
        // Record might have changed while answering a registration survey
        String recordId = getIntent().getStringExtra(ConstantUtil.RECORD_ID_EXTRA);
        mRecord = mDatabase.getSurveyedLocale(recordId);
        displayRecord();
    }

    private void displayRecord() {
        if (mRecord != null) {
            setTitle(mRecord.getDisplayName(this));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FORM && resultCode == RESULT_OK) {
            finish();
        }
    }

    @Override
    public void onSurveyClick(final String formId) {
        if (BootstrapService.isProcessing) {
            Toast.makeText(this, R.string.pleasewaitforbootstrap, Toast.LENGTH_LONG).show();
            return;
        }
        Survey survey = mDatabase.getSurvey(formId);
        if (!survey.isHelpDownloaded()) {
            Toast.makeText(this, R.string.error_missing_cascade, Toast.LENGTH_LONG).show();
            return;
        }

        // Check if there are saved (non-submitted) responses for this Survey, and take the 1st one
        String surveyedLocaleId = getRecordId();
        long[] instances = mDatabase.getFormInstances(surveyedLocaleId, formId,
                SurveyInstanceStatus.SAVED);
        long formInstanceId = instances.length > 0 ?
                instances[0] :
                mDatabase.createSurveyRespondent(formId, survey.getVersion(), mUser,
                        surveyedLocaleId);

        navigator.navigateToFormActivity(this, surveyedLocaleId, formId,
                formInstanceId, false, mSurveyGroup);
    }

    private String getRecordId() {
        return mRecord == null ? "" : mRecord.getId();
    }

    // ==================================== //
    // =========== Options Menu =========== //
    // ==================================== //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.datapoint_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_map:
                String recordId = getRecordId();
                startActivity(new Intent(this, MapActivity.class)
                        .putExtra(ConstantUtil.SURVEYED_LOCALE_ID_EXTRA, recordId));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
