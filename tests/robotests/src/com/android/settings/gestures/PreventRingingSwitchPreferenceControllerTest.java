/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.provider.Settings.Secure.VOLUME_HUSH_MUTE;
import static android.provider.Settings.Secure.VOLUME_HUSH_OFF;
import static android.provider.Settings.Secure.VOLUME_HUSH_VIBRATE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PreventRingingSwitchPreferenceControllerTest {

    private static final int UNKNOWN = -1;

    private Context mContext;
    private Resources mResources;
    private PreventRingingSwitchPreferenceController mController;
    private Preference mPreference = mock(Preference.class);

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(com.android.internal.R.bool.config_volumeHushGestureEnabled))
                .thenReturn(true);
        mController = new PreventRingingSwitchPreferenceController(mContext);
        mController.mSwitch = mock(SwitchBar.class);
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
    public void updateState_hushOff_uncheck() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_OFF);

        mController.updateState(mPreference);

        verify(mController.mSwitch, times(1)).setChecked(false);
    }

    @Test
    public void updateState_hushVibrate_setChecked() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_VIBRATE);

        mController.updateState(mPreference);

        verify(mController.mSwitch, times(1)).setChecked(true);
    }

    @Test
    public void updateState_hushMute_setChecked() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_MUTE);

        mController.updateState(mPreference);

        verify(mController.mSwitch, times(1)).setChecked(true);
    }

    @Test
    public void onSwitchChanged_wasHushOff_checked_returnHushVibrate() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_OFF);

        mController.onSwitchChanged(null, true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, UNKNOWN)).isEqualTo(VOLUME_HUSH_VIBRATE);
    }

    @Test
    public void onSwitchChanged_wasHushMute_unchecked_returnHushOff() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_MUTE);

        mController.onSwitchChanged(null, false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, UNKNOWN)).isEqualTo(VOLUME_HUSH_OFF);
    }

    @Test
    public void onSwitchChanged_wasHushMute_checked_returnHushMute() {
        // this is the case for the page open
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_MUTE);

        mController.onSwitchChanged(null, true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, UNKNOWN)).isEqualTo(VOLUME_HUSH_MUTE);
    }

    @Test
    public void onSwitchChanged_wasHushVibrate_checked_returnHushVibrate() {
        // this is the case for the page open
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_VIBRATE);

        mController.onSwitchChanged(null, true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, UNKNOWN)).isEqualTo(VOLUME_HUSH_VIBRATE);
    }

    @Test
    public void testPreferenceClickListenerAttached() {
        PreferenceScreen preferenceScreen = mock(PreferenceScreen.class);
        LayoutPreference mLayoutPreference = mock(LayoutPreference.class);
        when(preferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mLayoutPreference);

        mController.displayPreference(preferenceScreen);

        verify(mLayoutPreference, times(1))
                .setOnPreferenceClickListener(any());
    }
}
