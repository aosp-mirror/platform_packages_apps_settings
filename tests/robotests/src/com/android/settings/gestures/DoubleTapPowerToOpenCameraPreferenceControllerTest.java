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

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;

import static com.android.settings.gestures.DoubleTapPowerToOpenCameraPreferenceController.OFF;
import static com.android.settings.gestures.DoubleTapPowerToOpenCameraPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = SettingsShadowResources.class)
public class DoubleTapPowerToOpenCameraPreferenceControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private Resources mResources;
    private DoubleTapPowerToOpenCameraPreferenceController mController;
    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.getApplication());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        mController =
                new DoubleTapPowerToOpenCameraPreferenceController(mContext, KEY_DOUBLE_TAP_POWER);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureDisabled_preferenceUnsupported() {
        when(mResources.getBoolean(R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureEnabled_preferenceSupported() {
        when(mResources.getBoolean(R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_configIsNotSet_returnsTrue() {
        // Set the setting to be enabled.
        Settings.Secure.putInt(
                mContext.getContentResolver(), CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, ON);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_setConfigFalse_returnsFalse() {
        // Set the setting to be disabled.
        Settings.Secure.putInt(
                mContext.getContentResolver(), CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, OFF);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_setConfigTrue_returnsFalse() {
        // Set the setting to be disabled.
        Settings.Secure.putInt(
                mContext.getContentResolver(), CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, ON);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_checkToggle_cameraDoubleTapPowerGestureEnabled() {
        mController.setChecked(true);

        assertThat(
                        Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
                                OFF))
                .isEqualTo(ON);
    }

    @Test
    public void setChecked_uncheckToggle_cameraDoubleTapPowerGestureDisabled() {
        mController.setChecked(false);

        assertThat(
                        Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
                                ON))
                .isEqualTo(OFF);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final DoubleTapPowerToOpenCameraPreferenceController controller =
                new DoubleTapPowerToOpenCameraPreferenceController(
                        mContext, "gesture_double_tap_power");

        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final DoubleTapPowerToOpenCameraPreferenceController controller =
                new DoubleTapPowerToOpenCameraPreferenceController(mContext, "bad_key");

        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
