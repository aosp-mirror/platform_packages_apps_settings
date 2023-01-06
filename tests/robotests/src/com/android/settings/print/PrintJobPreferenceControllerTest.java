/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.print;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PrintJobPreferenceControllerTest {
    private static final String PREF_KEY = "print_job_preference";

    @Mock
    private PrintManager mPrintManager;
    @Mock
    private PrintJob mPrintJob;
    @Mock
    private PrintJobInfo mPrintJobInfo;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private PrintJobPreferenceController mController;
    private Preference mPreference;
    private String mTestLabel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = new Preference(mContext);
        mTestLabel = "PrintTest";
        when(mContext.getSystemService(Context.PRINT_SERVICE)).thenReturn(mPrintManager);
        when(mPrintManager.getGlobalPrintManagerForUser(anyInt())).thenReturn(mPrintManager);
        when(mPrintManager.getPrintJob(anyObject())).thenReturn(mPrintJob);
        when(mPrintJob.getInfo()).thenReturn(mPrintJobInfo);
        mController = new PrintJobPreferenceController(mContext, PREF_KEY);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPrintJobInfo.getLabel()).thenReturn(mTestLabel);
        mController.displayPreference(mScreen);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void onStartStop_shouldRegisterPrintStateListener() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        verify(mPrintManager).addPrintJobStateChangeListener(mController);
        verify(mPrintManager).removePrintJobStateChangeListener(mController);
    }

    @Test
    public void updateUi_jobState_STATE_CREATED() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_CREATED);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_configuring_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }

    @Test
    public void updateUi_jobState_STATE_QUEUED() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_QUEUED);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_printing_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }

    @Test
    public void updateUi_jobState_STATE_STARTED() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_STARTED);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_printing_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }

    @Test
    public void updateUi_jobState_STATE_QUEUED_and_jobInfo_CANCELLING() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_QUEUED);
        when(mPrintJobInfo.isCancelling()).thenReturn(true);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_cancelling_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }

    @Test
    public void updateUi_jobState_STATE_STARTED_and_jobInfo_CANCELLING() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_STARTED);
        when(mPrintJobInfo.isCancelling()).thenReturn(true);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_cancelling_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }

    @Test
    public void updateUi_jobState_STATE_FAILED() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_FAILED);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_failed_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }

    @Test
    public void updateUi_jobState_STATE_BLOCKED() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_BLOCKED);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_blocked_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }

    @Test
    public void updateUi_jobState_STATE_BLOCKED_and_jobInfo_CANCELLING() {
        when(mPrintJobInfo.getState()).thenReturn(PrintJobInfo.STATE_BLOCKED);
        when(mPrintJobInfo.isCancelling()).thenReturn(true);

        mController.onStart();
        String title = mContext.getString(
                R.string.print_cancelling_state_title_template, mTestLabel);

        assertThat(mPreference.getTitle()).isEqualTo(title);
    }
}
