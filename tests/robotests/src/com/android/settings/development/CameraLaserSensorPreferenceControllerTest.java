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

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {SettingsShadowSystemProperties.class})
public class CameraLaserSensorPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    static final String USERDEBUG_BUILD = "userdebug";
    static final String ENG_BUILD = "eng";
    static final String USER_BUILD = "user";

    private CameraLaserSensorPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new CameraLaserSensorPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @After
    public void tearDown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void isAvailable_withConfigNoShow_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_withUserdebugBuild_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(true);

        SettingsShadowSystemProperties.set(
                CameraLaserSensorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_withEngBuild_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(true);

        SettingsShadowSystemProperties.set(
                CameraLaserSensorPreferenceController.BUILD_TYPE, ENG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_withUserBuild_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(true);

        SettingsShadowSystemProperties.set(
                CameraLaserSensorPreferenceController.BUILD_TYPE, USER_BUILD);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_cameraLaserSensorEnabled_shouldCheckedPreference() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(true);

        SettingsShadowSystemProperties.set(
                CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                Integer.toString(CameraLaserSensorPreferenceController.ENABLED));
        SettingsShadowSystemProperties.set(
                CameraLaserSensorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void displayPreference_cameraLaserSensorEnabled_shouldUncheckedPreference() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(true);

        SettingsShadowSystemProperties.set(
                CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                Integer.toString(CameraLaserSensorPreferenceController.DISABLED));
        SettingsShadowSystemProperties.set(
                CameraLaserSensorPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void handlePreferenceTreeClick_preferenceChecked_shouldEnableCameraLaserSensor() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(true);

        when(mPreference.isChecked()).thenReturn(true);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(Integer.toString(CameraLaserSensorPreferenceController.ENABLED).equals(
            SystemProperties.get(
                        CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                        Integer.toString(CameraLaserSensorPreferenceController.ENABLED)))).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_preferenceUnchecked_shouldDisableCameraLaserSensor() {
        when(mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor))
                .thenReturn(true);

        when(mPreference.isChecked()).thenReturn(false);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(Integer.toString(CameraLaserSensorPreferenceController.DISABLED).equals(
                SystemProperties.get(
                        CameraLaserSensorPreferenceController.PROPERTY_CAMERA_LASER_SENSOR,
                        Integer.toString(CameraLaserSensorPreferenceController.ENABLED)))).isTrue();
    }
}
