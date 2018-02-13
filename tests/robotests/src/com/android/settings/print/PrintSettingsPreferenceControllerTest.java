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

import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.PrintManagerWrapper;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PrintSettingsPreferenceControllerTest {

    @Mock
    private PrintManagerWrapper mPrintManager;
    private Context mContext;
    private Preference mPreference;
    private PrintSettingPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mController = new PrintSettingPreferenceController(mContext);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ReflectionHelpers.setField(mController, "mPrintManager", mPrintManager);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void onStartStop_shouldRegisterPrintStateListener() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        verify(mPrintManager).addPrintJobStateChanegListener(mController);
        verify(mPrintManager).removePrintJobStateChangeListener(mController);
    }

    @Test
    public void updateState_hasActiveJob_shouldSetSummaryToNumberOfJobs() {
        final List<PrintJob> printJobs = new ArrayList<>();
        final PrintJob job = mock(PrintJob.class, Mockito.RETURNS_DEEP_STUBS);
        printJobs.add(job);
        when(job.getInfo().getState()).thenReturn(PrintJobInfo.STATE_STARTED);
        when(mPrintManager.getPrintJobs()).thenReturn(printJobs);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getResources()
                        .getQuantityString(R.plurals.print_jobs_summary, 1, 1));
    }

    @Test
    public void updateState_shouldSetSummaryToNumberOfPrintServices() {
        final List<PrintServiceInfo> printServices = mock(List.class);
        when(printServices.isEmpty()).thenReturn(false);
        when(printServices.size()).thenReturn(2);
        // 2 services
        when(mPrintManager.getPrintServices(PrintManager.ENABLED_SERVICES))
                .thenReturn(printServices);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getResources()
                        .getQuantityString(R.plurals.print_settings_summary, 2, 2));

        // No service
        when(mPrintManager.getPrintServices(PrintManager.ENABLED_SERVICES)).thenReturn(null);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.print_settings_summary_no_service));
    }
}
