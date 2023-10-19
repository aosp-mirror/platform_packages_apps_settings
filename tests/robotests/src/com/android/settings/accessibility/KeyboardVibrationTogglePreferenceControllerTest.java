/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.vibrator.Flags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link KeyboardVibrationTogglePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class KeyboardVibrationTogglePreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private Resources mResources;
    private KeyboardVibrationTogglePreferenceController mController;

    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        mController = new KeyboardVibrationTogglePreferenceController(mContext, "preferenceKey");
        mPreference = new SwitchPreference(mContext);
        when(mPreferenceScreen.findPreference(
                mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void getAvailabilityStatus_featureSupported_available() {
        mSetFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_CATEGORY_ENABLED);
        when(mResources.getBoolean(R.bool.config_keyboard_vibration_supported)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_featureNotSupported_unavailable() {
        mSetFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_CATEGORY_ENABLED);
        when(mResources.getBoolean(R.bool.config_keyboard_vibration_supported)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_keyboardCategoryDisabled_unavailable() {
        mSetFlagsRule.disableFlags(Flags.FLAG_KEYBOARD_CATEGORY_ENABLED);
        when(mResources.getBoolean(R.bool.config_keyboard_vibration_supported)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_mainVibrateDisabled_shouldReturnFalseForCheckedAndEnabled() {
        updateSystemSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, OFF);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_mainVibrateEnabled_shouldReturnTrueForEnabled() {
        updateSystemSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, ON);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void isChecked_keyboardVibrateEnabled_shouldReturnTrue() {
        updateSystemSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, ON);
        updateSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED, ON);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_keyboardVibrateDisabled_shouldReturnFalse() {
        updateSystemSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, ON);
        updateSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED, OFF);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_checked_updateSettings() throws Settings.SettingNotFoundException {
        // set an off state initially
        updateSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED, OFF);

        assertThat(readSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED)).isEqualTo(OFF);

        mController.setChecked(true);

        assertThat(readSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED)).isEqualTo(ON);
    }

    @Test
    public void setChecked_unchecked_updateSettings() throws Settings.SettingNotFoundException {
        // set an on state initially
        updateSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED, ON);

        assertThat(readSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED)).isEqualTo(ON);

        mController.setChecked(false);

        assertThat(readSystemSetting(Settings.System.KEYBOARD_VIBRATION_ENABLED)).isEqualTo(OFF);
    }

    private void updateSystemSetting(String key, int value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value);
    }

    private int readSystemSetting(String key) throws Settings.SettingNotFoundException {
        return Settings.System.getInt(mContext.getContentResolver(), key);
    }
}
