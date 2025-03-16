/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DoubleTapPowerMainSwitchPreferenceControllerTest {

    private static final String KEY = "gesture_double_tap_power_enabled_main_switch";

    private Context mContext;
    private Resources mResources;
    private DoubleTapPowerMainSwitchPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new DoubleTapPowerMainSwitchPreferenceController(mContext, KEY);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureAvailable_preferenceEnabled() {
        when(mResources.getBoolean(R.bool.config_doubleTapPowerGestureEnabled)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureUnavailable_preferenceUnsupported() {
        when(mResources.getBoolean(R.bool.config_doubleTapPowerGestureEnabled)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_setDoubleTapPowerGestureEnabled_mainSwitchChecked() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_setDoubleTapPowerGestureDisabled_mainSwitchUnchecked() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_checkMainSwitch_doubleTapPowerGestureEnabled() {
        mController.setChecked(true);
        assertThat(DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext))
                .isTrue();
    }

    @Test
    public void setChecked_uncheckMainSwitch_doubleTapPowerGestureDisabled() {
        mController.setChecked(false);
        assertThat(DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext))
                .isFalse();
    }
}
