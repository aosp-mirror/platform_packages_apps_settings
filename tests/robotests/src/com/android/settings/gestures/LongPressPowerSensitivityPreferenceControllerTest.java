/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.app.Application;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LongPressPowerSensitivityPreferenceControllerTest {

    private static final String KEY_LONG_PRESS_SENSITIVITY =
            "gesture_power_menu_long_press_for_assist_sensitivity";

    private static final int[] SENSITIVITY_VALUES = {250, 350, 500, 750, 850};

    private Application mContext;
    private Resources mResources;
    private LongPressPowerSensitivityPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);

        when(mResources.getIntArray(
                com.android.internal.R.array.config_longPressOnPowerDurationSettings))
                .thenReturn(SENSITIVITY_VALUES);

        mController = new LongPressPowerSensitivityPreferenceController(mContext,
                KEY_LONG_PRESS_SENSITIVITY);
    }

    @Test
    public void getSliderPosition_returnsDefaultValue() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_longPressOnPowerDurationMs))
                .thenReturn(750);
        assertThat(mController.getSliderPosition()).isEqualTo(3);
    }

    @Test
    public void getSliderPosition_returnsSetValue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS_DURATION_MS, 350);
        assertThat(mController.getSliderPosition()).isEqualTo(1);
    }

    @Test
    public void setSliderPosition_setsValue() {
        mController.setSliderPosition(4);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS_DURATION_MS, 0)).isEqualTo(850);
    }

    @Test
    public void setSliderPositionOutOfBounds_returnsFalse() {
        assertThat(mController.setSliderPosition(-1)).isFalse();
        assertThat(mController.setSliderPosition(10)).isFalse();
    }

    @Test
    public void getMin_isZero() {
        assertThat(mController.getMin()).isEqualTo(0);
    }

    @Test
    public void getMax_isEqualToLastValueIndex() {
        assertThat(mController.getMax()).isEqualTo(4);
    }

    @Test
    public void longPressForAssistEnabled_isAvailable() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS,
                PowerMenuSettingsUtils.LONG_PRESS_POWER_ASSISTANT_VALUE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                LongPressPowerSensitivityPreferenceController.AVAILABLE);
    }

    @Test
    public void longPressForAssistDisabled_isNotAvailableDueToDependentSetting() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS,
                PowerMenuSettingsUtils.LONG_PRESS_POWER_NO_ACTION);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                LongPressPowerSensitivityPreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void sensitivityValuesAreNull_notAvailable() {
        when(mResources.getIntArray(
                com.android.internal.R.array.config_longPressOnPowerDurationSettings))
                .thenReturn(null);
        mController = new LongPressPowerSensitivityPreferenceController(mContext,
                KEY_LONG_PRESS_SENSITIVITY);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                LongPressPowerSensitivityPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void sensitivityValuesArrayTooShort_notAvailable() {
        when(mResources.getIntArray(
                com.android.internal.R.array.config_longPressOnPowerDurationSettings))
                .thenReturn(new int[]{200});
        mController = new LongPressPowerSensitivityPreferenceController(mContext,
                KEY_LONG_PRESS_SENSITIVITY);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                LongPressPowerSensitivityPreferenceController.UNSUPPORTED_ON_DEVICE);
    }
}
