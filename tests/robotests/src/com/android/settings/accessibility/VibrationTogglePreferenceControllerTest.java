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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link VibrationTogglePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class VibrationTogglePreferenceControllerTest {

    private static final String SETTING_KEY = Settings.System.NOTIFICATION_VIBRATION_INTENSITY;
    private static final int VIBRATION_USAGE = VibrationAttributes.USAGE_NOTIFICATION;
    private static final int OFF = 0;
    private static final int ON = 1;

    /** Basic implementation of preference controller to test generic behavior. */
    private static class TestPreferenceController extends VibrationTogglePreferenceController {

        TestPreferenceController(Context context) {
            super(context, "preference_key",
                    new VibrationPreferenceConfig(context, SETTING_KEY, VIBRATION_USAGE) {});
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }

    @Mock private PreferenceScreen mScreen;

    private Lifecycle mLifecycle;
    private Context mContext;
    private Vibrator mVibrator;
    private SwitchPreference mPreference;
    private VibrationTogglePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mContext = ApplicationProvider.getApplicationContext();
        mVibrator = mContext.getSystemService(Vibrator.class);
        mController = new TestPreferenceController(mContext);
        mLifecycle.addObserver(mController);
        mPreference = new SwitchPreference(mContext);
        mPreference.setTitle("Test title");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void missingSetting_shouldBeCheckedByDefault() {
        Settings.System.putString(mContext.getContentResolver(), SETTING_KEY, /* value= */ null);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_mainSwitchUpdates_shouldPreserveSettingBetweenUpdates() {
        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, ON);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, ON);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_shouldUpdateToggleState() {
        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_HIGH);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void setProgress_mainSwitchDisabled_ignoresUpdates() throws Exception {
        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();

        mController.setChecked(true);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();

    }
    @Test
    public void setProgress_updatesCheckedState() throws Exception {
        mController.setChecked(false);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);

        mController.setChecked(true);
        assertThat(readSetting(SETTING_KEY))
                .isEqualTo(mVibrator.getDefaultVibrationIntensity(VIBRATION_USAGE));

        mController.setChecked(false);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
    }

    private void updateSetting(String key, int value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value);
    }

    private int readSetting(String settingKey) throws Settings.SettingNotFoundException {
        return Settings.System.getInt(mContext.getContentResolver(), settingKey);
    }
}
