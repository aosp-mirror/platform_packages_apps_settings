/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.uwb;

import static android.uwb.UwbManager.AdapterStateCallback.STATE_CHANGED_REASON_SYSTEM_POLICY;
import static android.uwb.UwbManager.AdapterStateCallback.STATE_CHANGED_REASON_SYSTEM_REGULATION;
import static android.uwb.UwbManager.AdapterStateCallback.STATE_DISABLED;
import static android.uwb.UwbManager.AdapterStateCallback.STATE_ENABLED_ACTIVE;
import static android.uwb.UwbManager.AdapterStateCallback.STATE_ENABLED_INACTIVE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.test.TestLooper;
import android.uwb.UwbManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Unit tests for UWB preference toggle. */
@RunWith(RobolectricTestRunner.class)
public class UwbPreferenceControllerTest {
    private static final String TEST_SUMMARY = "uwb";
    private static final String TEST_AIRPLANE_SUMMARY = "apm_uwb";
    private static final String TEST_NO_UWB_REGULATORY_SUMMARY = "regulatory_uwb";
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    private UwbPreferenceController mController;
    private ArgumentCaptor<UwbManager.AdapterStateCallback> mAdapterStateCallbackArgumentCaptor =
            ArgumentCaptor.forClass(UwbManager.AdapterStateCallback.class);
    private ArgumentCaptor<BroadcastReceiver> mBroadcastReceiverArgumentCaptor =
            ArgumentCaptor.forClass(BroadcastReceiver.class);
    private TestLooper mTestLooper;
    @Mock
    private UwbManager mUwbManager;
    @Mock
    private UwbUtils mUwbUtils;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Resources mResources;

    @Before
    public void setUp() throws Exception {
        mTestLooper = new TestLooper();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager)
                .hasSystemFeature(PackageManager.FEATURE_UWB);
        when(mResources.getString(R.string.uwb_settings_summary))
                .thenReturn(TEST_SUMMARY);
        when(mResources.getString(R.string.uwb_settings_summary_airplane_mode))
                .thenReturn(TEST_AIRPLANE_SUMMARY);
        when(mResources.getString(R.string.uwb_settings_summary_no_uwb_regulatory))
                .thenReturn(TEST_NO_UWB_REGULATORY_SUMMARY);
        when(mContext.getMainLooper()).thenReturn(mTestLooper.getLooper());
        when(mContext.getSystemService(UwbManager.class)).thenReturn(mUwbManager);
        when(mContext.getResources()).thenReturn(mResources);
        when(mUwbUtils.isAirplaneModeOn(any())).thenReturn(false);
        doReturn(STATE_ENABLED_ACTIVE).when(mUwbManager).getAdapterState();
        mController = new UwbPreferenceController(mContext, "uwb_settings", mUwbUtils);
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    private void startControllerAndCaptureCallbacks() {
        mController.onStart();
        verify(mContext).registerReceiver(
                mBroadcastReceiverArgumentCaptor.capture(), any(), any(), any());
        verify(mUwbManager).registerAdapterStateCallback(
                any(), mAdapterStateCallbackArgumentCaptor.capture());
    }

    @Test
    public void getAvailabilityStatus_uwbDisabled_shouldReturnDisabled() throws Exception {
        when(mUwbUtils.isAirplaneModeOn(any())).thenReturn(true);
        startControllerAndCaptureCallbacks();
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_uwbShown_shouldReturnAvailable() throws Exception {
        when(mUwbUtils.isAirplaneModeOn(any())).thenReturn(false);
        startControllerAndCaptureCallbacks();
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_uwbNotShown_shouldReturnUnsupported() {
        doReturn(false).when(mPackageManager)
                .hasSystemFeature(PackageManager.FEATURE_UWB);

        mController.onStart();
        verify(mContext, never()).registerReceiver(any(), any(), any(), any());
        verify(mUwbManager, never()).registerAdapterStateCallback(any(), any());
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_uwbEnabled_shouldReturnTrue() {
        doReturn(STATE_ENABLED_ACTIVE).when(mUwbManager).getAdapterState();

        startControllerAndCaptureCallbacks();
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_uwbDisabled_shouldReturnFalse() {
        doReturn(STATE_DISABLED).when(mUwbManager).getAdapterState();

        startControllerAndCaptureCallbacks();
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_uwbDisabled_shouldEnableUwb() {
        clearInvocations(mUwbManager);

        startControllerAndCaptureCallbacks();
        mController.setChecked(true);

        verify(mUwbManager).setUwbEnabled(true);
        verify(mUwbManager, never()).setUwbEnabled(false);
    }

    @Test
    public void setChecked_uwbEnabled_shouldDisableUwb() {
        clearInvocations(mUwbManager);

        startControllerAndCaptureCallbacks();
        mController.setChecked(false);

        verify(mUwbManager).setUwbEnabled(false);
        verify(mUwbManager, never()).setUwbEnabled(true);
    }

    @Test
    public void updateStateAndSummary_uwbDisabledAndEnabled() {
        startControllerAndCaptureCallbacks();
        clearInvocations(mUwbManager, mPreference);

        mAdapterStateCallbackArgumentCaptor.getValue().onStateChanged(
                STATE_DISABLED, STATE_CHANGED_REASON_SYSTEM_POLICY);

        verify(mPreference).setEnabled(true);
        assertThat(mController.isChecked()).isFalse();
        verify(mPreference, times(2)).setSummary(TEST_SUMMARY);

        mAdapterStateCallbackArgumentCaptor.getValue().onStateChanged(
                STATE_ENABLED_INACTIVE, STATE_CHANGED_REASON_SYSTEM_POLICY);

        verify(mPreference, times(2)).setEnabled(true);
        assertThat(mController.isChecked()).isTrue();
        verify(mPreference, times(4)).setSummary(TEST_SUMMARY);
    }

    @Test
    public void updateStateAndSummary_apmEnabledAndDisabled() {
        startControllerAndCaptureCallbacks();
        clearInvocations(mUwbManager, mPreference);

        when(mUwbUtils.isAirplaneModeOn(any())).thenReturn(true);
        mBroadcastReceiverArgumentCaptor.getValue().onReceive(
                mock(Context.class), mock(Intent.class));

        verify(mPreference).setEnabled(false);
        verify(mPreference, times(2)).setSummary(TEST_AIRPLANE_SUMMARY);

        when(mUwbUtils.isAirplaneModeOn(any())).thenReturn(false);
        mBroadcastReceiverArgumentCaptor.getValue().onReceive(
                mock(Context.class), mock(Intent.class));

        verify(mPreference).setEnabled(true);
        verify(mPreference, times(2)).setSummary(TEST_SUMMARY);
    }

    @Test
    public void updateStateAndSummary_uwbDisabledDueToRegulatory() {
        startControllerAndCaptureCallbacks();
        clearInvocations(mUwbManager, mPreference);

        mAdapterStateCallbackArgumentCaptor.getValue().onStateChanged(
                STATE_DISABLED, STATE_CHANGED_REASON_SYSTEM_REGULATION);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
        verify(mPreference, times(2)).setSummary(TEST_NO_UWB_REGULATORY_SUMMARY);
    }
}

