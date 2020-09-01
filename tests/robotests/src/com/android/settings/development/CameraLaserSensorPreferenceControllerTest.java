/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.CameraLaserSensorPreferenceController.ENG_BUILD;
import static com.android.settings.development.CameraLaserSensorPreferenceController
        .USERDEBUG_BUILD;
import static com.android.settings.development.CameraLaserSensorPreferenceController.USER_BUILD;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class CameraLaserSensorPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private CameraLaserSensorPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new CameraLaserSensorPreferenceController(RuntimeEnvironment.application);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController.displayPreference(mScreen);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_withConfigNoShow_shouldReturnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_withUserdebugBuild_shouldReturnTrue() {
        SystemProperties.set(CameraLaserSensorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_withEngBuild_shouldReturnTrue() {
        SystemProperties.set(CameraLaserSensorPreferenceController.BUILD_TYPE, ENG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_withUserBuild_shouldReturnTrue() {
        SystemProperties.set(CameraLaserSensorPreferenceController.BUILD_TYPE, USER_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_cameraLaserSensorEnabled_shouldCheckedPreference() {
        SystemProperties.set(CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                Integer.toString(CameraLaserSensorPreferenceController.ENABLED));
        SystemProperties.set(CameraLaserSensorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mScreen);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_cameraLaserSensorEnabled_shouldUncheckedPreference() {
        SystemProperties.set(CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                Integer.toString(CameraLaserSensorPreferenceController.DISABLED));
        SystemProperties.set(
                CameraLaserSensorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mScreen);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldEnableCameraLaserSensor() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(Integer.toString(CameraLaserSensorPreferenceController.ENABLED)).isEqualTo(
                SystemProperties.get(
                        CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                        Integer.toString(CameraLaserSensorPreferenceController.ENABLED)));
    }

    @Test
    public void onPreferenceChange__preferenceUnchecked_shouldDisableCameraLaserSensor() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(Integer.toString(CameraLaserSensorPreferenceController.DISABLED)).isEqualTo(
                SystemProperties.get(
                        CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                        Integer.toString(CameraLaserSensorPreferenceController.ENABLED)));
    }
}
