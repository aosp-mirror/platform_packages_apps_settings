/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.Vibrator;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VibrationPreferenceControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";
    private static final int OFF = 0;
    private static final int ON = 1;

    @Mock private Vibrator mVibrator;
    @Mock private PreferenceScreen mScreen;

    private Context mContext;
    private Resources mResources;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(mVibrator);
        when(mVibrator.hasVibrator()).thenReturn(true);
    }

    @Test
    public void verifyConstants() {
        VibrationPreferenceController controller = createPreferenceController();
        assertThat(controller.getPreferenceKey()).isEqualTo(PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_noVibrator_returnUnsupportedOnDevice() {
        when(mVibrator.hasVibrator()).thenReturn(false);
        VibrationPreferenceController controller = createPreferenceController();

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_withVibrator_returnAvailable() {
        when(mVibrator.hasVibrator()).thenReturn(true);
        VibrationPreferenceController controller = createPreferenceController();

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_vibrateSettingNotSet_returnsOnText() {
        Settings.System.putString(mContext.getContentResolver(), Settings.System.VIBRATE_ON,
                /* value= */ null);
        VibrationPreferenceController controller = createPreferenceController();
        controller.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_vibration_settings_state_on));
    }

    @Test
    public void getSummary_vibrateSettingOn_returnsOnText() {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.VIBRATE_ON, ON);
        VibrationPreferenceController controller = createPreferenceController();
        controller.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_vibration_settings_state_on));
    }

    @Test
    public void getSummary_vibrateSettingOff_returnsOffText() {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.VIBRATE_ON, OFF);
        VibrationPreferenceController controller = createPreferenceController();
        controller.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_vibration_settings_state_off));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SEPARATE_ACCESSIBILITY_VIBRATION_SETTINGS_FRAGMENTS)
    public void handlePreferenceTreeClick_oneIntensityLevel_opensVibrationSettings() {
        when(mResources.getInteger(R.integer.config_vibration_supported_intensity_levels))
                .thenReturn(1);
        VibrationPreferenceController controller = spy(createPreferenceController());

        doNothing().when(controller).launchVibrationSettingsFragment(any());
        controller.handlePreferenceTreeClick(mPreference);

        verify(controller).launchVibrationSettingsFragment(eq(VibrationSettings.class));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SEPARATE_ACCESSIBILITY_VIBRATION_SETTINGS_FRAGMENTS)
    public void handlePreferenceTreeClick_multipleIntensityLevels_opensVibrationIntensity() {
        when(mResources.getInteger(R.integer.config_vibration_supported_intensity_levels))
                .thenReturn(2);
        VibrationPreferenceController controller = spy(createPreferenceController());

        doNothing().when(controller).launchVibrationSettingsFragment(any());
        controller.handlePreferenceTreeClick(mPreference);

        verify(controller).launchVibrationSettingsFragment(
                eq(VibrationIntensitySettingsFragment.class));
    }

    private VibrationPreferenceController createPreferenceController() {
        VibrationPreferenceController controller =
                new VibrationPreferenceController(mContext, PREFERENCE_KEY);
        mPreference = new Preference(mContext);
        mPreference.setSummary("Test summary");
        mPreference.setKey(PREFERENCE_KEY);
        when(mScreen.findPreference(controller.getPreferenceKey())).thenReturn(mPreference);
        controller.displayPreference(mScreen);
        return controller;
    }
}
