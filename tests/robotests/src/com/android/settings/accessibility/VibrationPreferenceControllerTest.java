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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VibrationPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";
    private static final int OFF = 0;
    private static final int ON = 1;

    @Mock private Vibrator mVibrator;
    @Mock private PreferenceScreen mScreen;

    private Context mContext;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(mVibrator);
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
        when(mVibrator.hasVibrator()).thenReturn(true);
        Settings.System.putString(mContext.getContentResolver(), Settings.System.VIBRATE_ON,
                /* value= */ null);
        VibrationPreferenceController controller = createPreferenceController();
        controller.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_vibration_settings_state_on));
    }

    @Test
    public void getSummary_vibrateSettingOn_returnsOnText() {
        when(mVibrator.hasVibrator()).thenReturn(true);
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.VIBRATE_ON, ON);
        VibrationPreferenceController controller = createPreferenceController();
        controller.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_vibration_settings_state_on));
    }

    @Test
    public void getSummary_vibrateSettingOff_returnsOffText() {
        when(mVibrator.hasVibrator()).thenReturn(true);
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.VIBRATE_ON, OFF);
        VibrationPreferenceController controller = createPreferenceController();
        controller.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_vibration_settings_state_off));
    }

    private VibrationPreferenceController createPreferenceController() {
        VibrationPreferenceController controller =
                new VibrationPreferenceController(mContext, PREFERENCE_KEY);
        mPreference = new Preference(mContext);
        mPreference.setSummary("Test summary");
        when(mScreen.findPreference(controller.getPreferenceKey())).thenReturn(mPreference);
        controller.displayPreference(mScreen);
        return controller;
    }
}
