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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link VibrationIntensityPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class})
public class VibrationIntensityPreferenceControllerTest {

    private static final String SETTING_KEY = Settings.System.NOTIFICATION_VIBRATION_INTENSITY;
    private static final int VIBRATION_USAGE = VibrationAttributes.USAGE_NOTIFICATION;
    private static final int OFF = 0;
    private static final int ON = 1;

    /** Basic implementation of preference controller to test generic behavior. */
    private static class TestPreferenceController extends VibrationIntensityPreferenceController {

        TestPreferenceController(Context context, int supportedIntensityLevels) {
            super(context, "preference_key",
                    new VibrationPreferenceConfig(context, SETTING_KEY, VIBRATION_USAGE) {},
                    supportedIntensityLevels);
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
    private SeekBarPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mContext = ApplicationProvider.getApplicationContext();
        mVibrator = mContext.getSystemService(Vibrator.class);
    }

    @Test
    public void missingSetting_shouldReturnDefault() {
        VibrationIntensityPreferenceController controller = createPreferenceController(3);
        Settings.System.putString(mContext.getContentResolver(), SETTING_KEY, /* value= */ null);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress())
                .isEqualTo(mVibrator.getDefaultVibrationIntensity(VIBRATION_USAGE));
    }

    @Test
    public void updateState_mainSwitchUpdates_shouldPreserveSettingBetweenUpdates() {
        VibrationIntensityPreferenceController controller = createPreferenceController(3);
        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, ON);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(mPreference.isEnabled()).isTrue();

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, OFF);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
        assertThat(mPreference.isEnabled()).isFalse();

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, ON);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_allLevelsSupported_shouldDisplayIntensityInSliderPosition() {
        VibrationIntensityPreferenceController controller = createPreferenceController(3);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_HIGH);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_OFF);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
    }

    @Test
    public void updateState_twoLevelsSupported_shouldDisplayMediumAndHighAtLastPosition() {
        VibrationIntensityPreferenceController controller = createPreferenceController(2);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_HIGH);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_OFF);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
    }

    @Test
    public void updateState_oneLevelSupported_shouldDisplayAllAtLastPosition() {
        VibrationIntensityPreferenceController controller = createPreferenceController(1);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_HIGH);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_MEDIUM);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_OFF);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
    }

    @Test
    public void setProgress_mainSwitchDisabled_ignoresUpdates() throws Exception {
        VibrationIntensityPreferenceController controller = createPreferenceController(3);
        updateSetting(SETTING_KEY, Vibrator.VIBRATION_INTENSITY_LOW);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        updateSetting(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY, OFF);
        controller.updateState(mPreference);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
        assertThat(mPreference.isEnabled()).isFalse();

        assertThat(controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_HIGH)).isFalse();
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(mPreference.getProgress()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
    }
    @Test
    public void setProgress_allSupportedPositions_updatesIntensitySetting() throws Exception {
        VibrationIntensityPreferenceController controller = createPreferenceController(3);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_OFF);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_MEDIUM);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_HIGH);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH);
    }

    @Test
    public void setProgress_twoSupportedPositions_updatesMediumPositionToHigh() throws Exception {
        VibrationIntensityPreferenceController controller = createPreferenceController(2);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_OFF);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_MEDIUM);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_HIGH);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH);
    }

    @Test
    public void setProgress_oneSupportedPosition_updatesOnPositionsToDeviceDefault()
            throws Exception {
        int defaultIntensity = mVibrator.getDefaultVibrationIntensity(VIBRATION_USAGE);
        VibrationIntensityPreferenceController controller = createPreferenceController(1);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_OFF);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_LOW);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(defaultIntensity);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_MEDIUM);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(defaultIntensity);

        controller.setSliderPosition(Vibrator.VIBRATION_INTENSITY_HIGH);
        assertThat(readSetting(SETTING_KEY)).isEqualTo(defaultIntensity);
    }

    private void updateSetting(String key, int value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value);
    }

    private int readSetting(String settingKey) throws Settings.SettingNotFoundException {
        return Settings.System.getInt(mContext.getContentResolver(), settingKey);
    }

    private VibrationIntensityPreferenceController createPreferenceController(
            int supportedIntensityLevels) {
        VibrationIntensityPreferenceController controller =
                new TestPreferenceController(mContext, supportedIntensityLevels);
        mLifecycle.addObserver(controller);
        mPreference = new SeekBarPreference(mContext);
        mPreference.setSummary("Test summary");
        when(mScreen.findPreference(controller.getPreferenceKey())).thenReturn(mPreference);
        controller.displayPreference(mScreen);
        return controller;
    }
}
