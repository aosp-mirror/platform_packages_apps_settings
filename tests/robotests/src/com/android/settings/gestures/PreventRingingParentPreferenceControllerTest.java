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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = new PreventRingingParentPreferenceController(mContext, "test_key");
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
        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getText(
                R.string.prevent_ringing_option_mute_summary));

        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_VIBRATE);
        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getText(
                R.string.prevent_ringing_option_vibrate_summary));

        Settings.Secure.putInt(mContext.getContentResolver(), VOLUME_HUSH_GESTURE,
                VOLUME_HUSH_OFF);
        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getText(
                R.string.prevent_ringing_option_none_summary));
    }
}
