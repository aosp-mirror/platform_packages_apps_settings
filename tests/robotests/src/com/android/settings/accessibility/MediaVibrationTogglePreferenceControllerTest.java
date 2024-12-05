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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Test for {@link MediaVibrationIntensityPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class MediaVibrationTogglePreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    @Mock private PreferenceScreen mScreen;
    @Mock AudioManager mAudioManager;

    private Lifecycle mLifecycle;
    private Context mContext;
    private Vibrator mVibrator;
    private MediaVibrationTogglePreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mAudioManager);
        when(mAudioManager.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        mVibrator = mContext.getSystemService(Vibrator.class);
        mController = new MediaVibrationTogglePreferenceController(mContext, PREFERENCE_KEY);
        mLifecycle.addObserver(mController);
        mPreference = new SwitchPreference(mContext);
        mPreference.setSummary("Test summary");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void verifyConstants() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREFERENCE_KEY);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void missingSetting_shouldReturnDefault() {
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.MEDIA_VIBRATION_INTENSITY, /* value= */ null);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_ringerModeUpdates_shouldNotAffectSettings() {
        updateSetting(Settings.System.MEDIA_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW);

        when(mAudioManager.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();

        when(mAudioManager.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_SILENT);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();

        when(mAudioManager.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_VIBRATE);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_shouldDisplayOnOffState() {
        updateSetting(Settings.System.MEDIA_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_HIGH);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        updateSetting(Settings.System.MEDIA_VIBRATION_INTENSITY,
                Vibrator.VIBRATION_INTENSITY_MEDIUM);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        updateSetting(Settings.System.MEDIA_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        updateSetting(Settings.System.MEDIA_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_updatesIntensityAndDependentSettings() throws Exception {
        updateSetting(Settings.System.MEDIA_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();

        mController.setChecked(true);
        assertThat(readSetting(Settings.System.MEDIA_VIBRATION_INTENSITY)).isEqualTo(
                mVibrator.getDefaultVibrationIntensity(VibrationAttributes.USAGE_MEDIA));

        mController.setChecked(false);
        assertThat(readSetting(Settings.System.MEDIA_VIBRATION_INTENSITY))
                .isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF);
    }

    @Test
    public void configForMediaVibration_enabled_shouldShowToogle() {
        SettingsShadowResources.overrideResource(R.bool.config_media_vibration_supported, true);
        mController.updateState(mPreference);

        final boolean mediaVibrationConfig = mContext.getResources()
                .getBoolean(R.bool.config_media_vibration_supported);

        assertThat(mediaVibrationConfig).isTrue();
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.isSupported()).isTrue();
    }

    @Test
    public void configForMediaVibration_disabled_shouldHideToggle() {
        SettingsShadowResources.overrideResource(R.bool.config_media_vibration_supported, false);
        mController.updateState(mPreference);

        final boolean mediaVibrationConfig = mContext.getResources()
                .getBoolean(R.bool.config_media_vibration_supported);

        assertThat(mediaVibrationConfig).isFalse();
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.isSupported()).isFalse();
    }

    private void updateSetting(String key, int value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value);
    }

    private int readSetting(String settingKey) throws Settings.SettingNotFoundException {
        return Settings.System.getInt(mContext.getContentResolver(), settingKey);
    }
}
