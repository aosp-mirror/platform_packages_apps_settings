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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.VOLUME_HUSH_GESTURE;
import static android.provider.Settings.Secure.VOLUME_HUSH_MUTE;
import static android.provider.Settings.Secure.VOLUME_HUSH_OFF;
import static android.provider.Settings.Secure.VOLUME_HUSH_VIBRATE;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreventRingingParentPreferenceControllerTest {

    @Mock
    private Resources mResources;

    @Mock
    PreferenceScreen mScreen;

    private Context mContext;
    private PreventRingingParentPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getInteger(
                com.android.internal.R.integer.config_keyChordPowerVolumeUp)).thenReturn(
                PreventRingingParentPreferenceController.KEY_CHORD_POWER_VOLUME_UP_MUTE_TOGGLE);
        mController = new PreventRingingParentPreferenceController(mContext, "test_key");
        mPreference = new PrimarySwitchPreference(mContext);
        when(mScreen.findPreference("test_key")).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_configIsTrueAndKeyChordMute_shouldAvailableUnSearchable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_configIsTrueAndKeyNotMute_shouldReturnDisabledDependent() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(true);
        when(mResources.getInteger(
                com.android.internal.R.integer.config_keyChordPowerVolumeUp)).thenReturn(2);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_configIsTrueLppDisabled_shouldReturnUnsupportedOnDevice() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(false);
        when(mResources.getInteger(
                com.android.internal.R.integer.config_keyChordPowerVolumeUp)).thenReturn(2);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isAvailable_configIsFalse_shouldReturnFalse() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_summaryUpdated() {
        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_MUTE);
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary()).isEqualTo(mContext.getResources().getText(
                R.string.prevent_ringing_option_mute_summary));

        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_VIBRATE);
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary()).isEqualTo(mContext.getResources().getText(
                R.string.prevent_ringing_option_vibrate_summary));

        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_OFF);
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary()).isEqualTo(mContext.getResources().getText(
                R.string.switch_off_text));
    }

    @Test
    public void updateState_keyChordDisabled_summaryUpdated() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_keyChordPowerVolumeUp)).thenReturn(2);
        // Ensure that the state displays unchecked even if the underlying field is set.
        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_MUTE);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(mContext.getResources().getText(
                R.string.prevent_ringing_option_unavailable_lpp_summary));
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_vibrate_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_VIBRATE);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_mute_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_MUTE);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_off_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_OFF);

        assertThat(mController.isChecked()).isFalse();
    }
}
