/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputSettings;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link TrackpadPointerSpeedPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
})
public class TrackpadPointerSpeedPreferenceControllerTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final String PREFERENCE_KEY = "trackpad_pointer_speed";
    private static final String SETTING_KEY = Settings.System.TOUCHPAD_POINTER_SPEED;

    private Context mContext;
    private TrackpadPointerSpeedPreferenceController mController;
    private int mDefaultSpeed;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new TrackpadPointerSpeedPreferenceController(mContext, PREFERENCE_KEY);
        mDefaultSpeed = Settings.System.getIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                InputSettings.DEFAULT_POINTER_SPEED,
                UserHandle.USER_CURRENT);
    }

    @Test
    public void getAvailabilityStatus_expected() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getMin_expected() {
        assertThat(mController.getMin()).isEqualTo(InputSettings.MIN_POINTER_SPEED);
    }

    @Test
    public void getMax_expected() {
        assertThat(mController.getMax()).isEqualTo(InputSettings.MAX_POINTER_SPEED);
    }

    @Test
    public void getSliderPosition_defaultSpeed_return0() {
        int result = mController.getSliderPosition();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void setSliderPosition_speedValue1_shouldReturnTrue() {
        int inputSpeed = 1;

        boolean result = mController.setSliderPosition(inputSpeed);

        assertThat(result).isTrue();
        assertThat(mController.getSliderPosition()).isEqualTo(inputSpeed);
        verify(mFeatureFactory.metricsFeatureProvider).action(
                any(),
                eq(SettingsEnums.ACTION_GESTURE_POINTER_SPEED_CHANGED),
                eq(1));
    }

    @Test
    public void setSliderPosition_speedValueOverMaxValue_shouldReturnFalse() {
        int inputSpeed = InputSettings.MAX_POINTER_SPEED + 1;

        boolean result = mController.setSliderPosition(inputSpeed);

        assertThat(result).isFalse();
        assertThat(mController.getSliderPosition()).isEqualTo(mDefaultSpeed);
    }

    @Test
    public void setSliderPosition_speedValueOverMinValue_shouldReturnFalse() {
        int inputSpeed = InputSettings.MIN_POINTER_SPEED - 1;

        boolean result = mController.setSliderPosition(inputSpeed);

        assertThat(result).isFalse();
        assertThat(mController.getSliderPosition()).isEqualTo(mDefaultSpeed);
    }
}
