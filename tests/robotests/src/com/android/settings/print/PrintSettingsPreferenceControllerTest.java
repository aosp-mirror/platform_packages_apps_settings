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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PrintSettingsPreferenceControllerTest {

    @Mock
    private PrintManager mPrintManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private RestrictedPreference mPreference;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private PrintSettingPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mPreference = spy(new RestrictedPreference(mContext));
        mController = new PrintSettingPreferenceController(mContext);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ReflectionHelpers.setField(mController, "mPrintManager", mPrintManager);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
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
    public void onStartStop_printManagerIsNull_doNothing() {
        ReflectionHelpers.setField(mController, "mPrintManager", null);

        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        verify(mPrintManager, never()).addPrintJobStateChangeListener(mController);
        verify(mPrintManager, never()).removePrintJobStateChangeListener(mController);
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

    @Test
    public void updateState_shouldCheckRestriction() {
        mController.updateState(mPreference);
        verify(mPreference).checkRestrictionAndSetDisabled(UserManager.DISALLOW_PRINTING);
    }

    @Test
    public void getAvailabilityStatus_hasFeature_returnsAvailable() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_PRINTING))
                .thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noFeature_returnsUnsupported() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_PRINTING))
                .thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_printManagerIsNull_returnsUnsupported() {
        ReflectionHelpers.setField(mController, "mPrintManager", null);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_PRINTING))
                .thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
