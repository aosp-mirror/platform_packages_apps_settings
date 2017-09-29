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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {SettingsShadowSystemProperties.class})
public class CameraHalHdrPlusPreferenceControllerV2Test {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;
    private CameraHalHdrPlusPreferenceControllerV2 mController;

    static final String USERDEBUG_BUILD = "userdebug";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new CameraHalHdrPlusPreferenceControllerV2(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_withConfigNoShowAndUserDebugBuild_shouldReturnFalse() {
        SettingsShadowSystemProperties.set(
                CameraHalHdrPlusPreferenceControllerV2.BUILD_TYPE, USERDEBUG_BUILD);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_cameraHalHdrPlusEnabled_shouldCheckedPreference() {
        SettingsShadowSystemProperties.set(
                CameraHalHdrPlusPreferenceControllerV2.PROPERTY_CAMERA_HAL_HDRPLUS,
                CameraHalHdrPlusPreferenceControllerV2.ENABLED);
        SettingsShadowSystemProperties.set(
                CameraHalHdrPlusPreferenceControllerV2.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_cameraHalHdrPlusEnabled_shouldUncheckedPreference() {
        SettingsShadowSystemProperties.set(
                CameraHalHdrPlusPreferenceControllerV2.PROPERTY_CAMERA_HAL_HDRPLUS,
                CameraHalHdrPlusPreferenceControllerV2.DISABLED);
        SettingsShadowSystemProperties.set(
                CameraHalHdrPlusPreferenceControllerV2.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldEnableCameraHalHdrplus() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        assertThat(CameraHalHdrPlusPreferenceControllerV2.ENABLED).isEqualTo(
                SystemProperties.get(
                        CameraHalHdrPlusPreferenceControllerV2.PROPERTY_CAMERA_HAL_HDRPLUS,
                        CameraHalHdrPlusPreferenceControllerV2.DISABLED));
    }

    @Test
    public void handlePreferenceTreeClick_preferenceUnchecked_shouldDisableCameraHalHdrplus() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        assertThat(CameraHalHdrPlusPreferenceControllerV2.DISABLED).isEqualTo(
                SystemProperties.get(
                        CameraHalHdrPlusPreferenceControllerV2.PROPERTY_CAMERA_HAL_HDRPLUS,
                        CameraHalHdrPlusPreferenceControllerV2.DISABLED));
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_shouldEnablePreference() {
        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
        assertThat(CameraHalHdrPlusPreferenceControllerV2.DISABLED).isEqualTo(
                SystemProperties.get(
                        CameraHalHdrPlusPreferenceControllerV2.PROPERTY_CAMERA_HAL_HDRPLUS,
                        CameraHalHdrPlusPreferenceControllerV2.DISABLED));
    }
}
