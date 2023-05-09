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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;

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

@RunWith(RobolectricTestRunner.class)
public class VibrationRampingRingerTogglePreferenceControllerTest {

    private static final String PREFERENCE_KEY = "preference_key";

    @Mock private PreferenceScreen mScreen;
    @Mock private TelephonyManager mTelephonyManager;
    @Mock private AudioManager mAudioManager;
    @Mock private VibrationRampingRingerTogglePreferenceController.DeviceConfigProvider
            mDeviceConfigProvider;

    private Lifecycle mLifecycle;
    private Context mContext;
    private VibrationRampingRingerTogglePreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mAudioManager);
        when(mAudioManager.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_NORMAL);
        mController = new VibrationRampingRingerTogglePreferenceController(mContext,
                PREFERENCE_KEY, mDeviceConfigProvider);
        mLifecycle.addObserver(mController);
        mPreference = new SwitchPreference(mContext);
        mPreference.setSummary("Test summary");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void verifyConstants() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_notVoiceCapable_returnUnsupportedOnDevice() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);
        when(mDeviceConfigProvider.isRampingRingerEnabledOnTelephonyConfig()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_rampingRingerEnabled_returnUnsupportedOnDevice() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mDeviceConfigProvider.isRampingRingerEnabledOnTelephonyConfig()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_voiceCapableAndRampingRingerDisabled_returnAvailable() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mDeviceConfigProvider.isRampingRingerEnabledOnTelephonyConfig()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_withRingDisabled_shouldReturnFalseForCheckedAndEnabled() {
        updateSetting(Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);
        when(mAudioManager.isRampingRingerEnabled()).thenReturn(true);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_withRingEnabled_shouldReturnTheSettingStateAndAlwaysEnabled() {
        updateSetting(Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_HIGH);
        when(mAudioManager.isRampingRingerEnabled()).thenReturn(true, false);

        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();

        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_withRingDisabled_ignoresUpdates() {
        updateSetting(Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_OFF);

        mController.setChecked(true);
        mController.setChecked(false);
        verify(mAudioManager, never()).setRampingRingerEnabled(anyBoolean());
    }

    @Test
    public void setChecked_withRingEnabled_updatesSetting() {
        updateSetting(Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_HIGH);

        mController.setChecked(true);
        verify(mAudioManager).setRampingRingerEnabled(true);

        mController.setChecked(false);
        verify(mAudioManager).setRampingRingerEnabled(false);
    }

    private void updateSetting(String key, int value) {
        Settings.System.putInt(mContext.getContentResolver(), key, value);
    }
}
