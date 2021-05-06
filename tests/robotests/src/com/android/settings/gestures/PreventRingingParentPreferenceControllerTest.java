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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;

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

    private Context mContext;
    private PreventRingingParentPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = new PreventRingingParentPreferenceController(mContext, "test_key");
        mPreference = new Preference(mContext);
    }

    @Test
    public void testIsAvailable_configIsTrue_shouldAvailableUnSearchable() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void testIsAvailable_configIsFalse_shouldReturnFalse() {
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
