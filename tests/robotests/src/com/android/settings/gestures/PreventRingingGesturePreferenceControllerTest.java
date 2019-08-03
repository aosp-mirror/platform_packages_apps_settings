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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.widget.RadioButtonPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreventRingingGesturePreferenceControllerTest {
    private Context mContext;
    private Resources mResources;
    private PreventRingingGesturePreferenceController mController;

    @Mock
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(com.android.internal.R.bool.config_volumeHushGestureEnabled))
                .thenReturn(true);
        mController = new PreventRingingGesturePreferenceController(mContext, null);
        mController.mPreferenceCategory = new PreferenceCategory(mContext);
        mController.mVibratePref = new RadioButtonPreference(mContext);
        mController.mMutePref = new RadioButtonPreference(mContext);
    }

    @Test
    public void testIsAvailable_configIsTrue_shouldReturnTrue() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_configIsFalse_shouldReturnFalse() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testUpdateState_mute() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_MUTE);
        mController.updateState(mPreference);
        assertThat(mController.mVibratePref.isEnabled()).isTrue();
        assertThat(mController.mMutePref.isEnabled()).isTrue();
        assertThat(mController.mVibratePref.isChecked()).isFalse();
        assertThat(mController.mMutePref.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_vibrate() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_VIBRATE);
        mController.updateState(mPreference);
        assertThat(mController.mVibratePref.isEnabled()).isTrue();
        assertThat(mController.mMutePref.isEnabled()).isTrue();
        assertThat(mController.mVibratePref.isChecked()).isTrue();
        assertThat(mController.mMutePref.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_off() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_OFF);
        mController.updateState(mPreference);
        assertThat(mController.mVibratePref.isEnabled()).isFalse();
        assertThat(mController.mMutePref.isEnabled()).isFalse();
        assertThat(mController.mVibratePref.isChecked()).isFalse();
        assertThat(mController.mMutePref.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_other() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                7);
        mController.updateState(mPreference);
        assertThat(mController.mVibratePref.isChecked()).isFalse();
        assertThat(mController.mMutePref.isChecked()).isFalse();
    }

    @Test
    public void testRadioButtonClicked_mute() {
        RadioButtonPreference rbPref = new RadioButtonPreference(mContext);
        rbPref.setKey(PreventRingingGesturePreferenceController.KEY_MUTE);

        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_OFF);
        mController.onRadioButtonClicked(rbPref);

        assertThat(Settings.Secure.VOLUME_HUSH_MUTE).isEqualTo(
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_OFF));
    }

    @Test
    public void testRadioButtonClicked_vibrate() {
        RadioButtonPreference rbPref = new RadioButtonPreference(mContext);
        rbPref.setKey(PreventRingingGesturePreferenceController.KEY_VIBRATE);

        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_OFF);
        mController.onRadioButtonClicked(rbPref);

        assertThat(Settings.Secure.VOLUME_HUSH_VIBRATE).isEqualTo(
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_OFF));
    }
}
