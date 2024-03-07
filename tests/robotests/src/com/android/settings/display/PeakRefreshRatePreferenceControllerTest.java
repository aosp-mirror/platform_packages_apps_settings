/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.display;

import static com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class PeakRefreshRatePreferenceControllerTest {

    private Context mContext;
    private PeakRefreshRatePreferenceController mController;
    private SwitchPreference mPreference;

    @Mock
    private PeakRefreshRatePreferenceController.DeviceConfigDisplaySettings
            mDeviceConfigDisplaySettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new PeakRefreshRatePreferenceController(mContext, "key");
        mController.injectDeviceConfigDisplaySettings(mDeviceConfigDisplaySettings);
        mPreference = new SwitchPreference(RuntimeEnvironment.application);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_withConfigNoShow_returnUnsupported() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_refreshRateLargerThanDefault_returnAvailable() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE + 1;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_refreshRateEqualToDefault_returnUnsupported() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setChecked_enableSmoothDisplay_setRefreshRateToInfinity() {
        mController.mPeakRefreshRate = 88f;
        mController.setChecked(true);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, DEFAULT_REFRESH_RATE))
                .isPositiveInfinity();
    }

    @Test
    public void setChecked_disableSmoothDisplay_setDefaultRefreshRate() {
        mController.mPeakRefreshRate = 88f;
        mController.setChecked(false);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, DEFAULT_REFRESH_RATE))
                .isEqualTo(DEFAULT_REFRESH_RATE);
    }

    @Test
    public void isChecked_enableSmoothDisplay_returnTrue() {
        enableSmoothDisplayPreference();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disableSmoothDisplay_returnFalse() {
        disableSmoothDisplayPreference();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_default_returnTrue() {
        mController.mPeakRefreshRate = 88f;
        when(mDeviceConfigDisplaySettings.getDefaultPeakRefreshRate())
                .thenReturn(mController.mPeakRefreshRate);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_default_returnFalse() {
        mController.mPeakRefreshRate = 88f;
        when(mDeviceConfigDisplaySettings.getDefaultPeakRefreshRate()).thenReturn(60f);

        assertThat(mController.isChecked()).isFalse();
    }

    private void enableSmoothDisplayPreference() {
        mController.mPeakRefreshRate = 88f;

        Settings.System.putFloat(
                mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE,
                mController.mPeakRefreshRate);
    }

    private void disableSmoothDisplayPreference() {
        mController.mPeakRefreshRate = 88f;

        Settings.System.putFloat(
                mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE,
                DEFAULT_REFRESH_RATE);
    }
}
