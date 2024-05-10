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

package com.android.settings.development;

import static com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE;
import static com.android.settings.development.ForcePeakRefreshRatePreferenceController.NO_CONFIG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
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
public class ForcePeakRefreshRatePreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private ForcePeakRefreshRatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ForcePeakRefreshRatePreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldEnableForcePeak() {
        mController.mPeakRefreshRate = 88f;

        mController.onPreferenceChange(mPreference, true);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, NO_CONFIG)).isPositiveInfinity();
    }

    @Test
    public void onPreferenceChange_preferenceUnchecked_shouldDisableForcePeak() {
        mController.mPeakRefreshRate = 88f;

        mController.onPreferenceChange(mPreference, false);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, NO_CONFIG)).isEqualTo(NO_CONFIG);
    }

    @Test
    public void updateState_enableForcePeak_shouldCheckedToggle() {
        mController.mPeakRefreshRate = 88f;
        mController.forcePeakRefreshRate(true);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_disableForcePeak_shouldUncheckedToggle() {
        mController.mPeakRefreshRate = 88f;
        mController.forcePeakRefreshRate(false);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_withConfigNoShow_returnUnsupported() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_refreshRateLargerThanDefault_returnTrue() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE + 1;

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_refreshRateEqualToDefault_returnFalse() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE;

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, -1f)).isEqualTo(NO_CONFIG);
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();
    }
}
