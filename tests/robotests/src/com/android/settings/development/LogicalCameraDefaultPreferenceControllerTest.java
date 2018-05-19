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

package com.android.settings.development;

import static com.android.settings.development.LogicalCameraDefaultPreferenceController.ENG_BUILD;
import static com.android.settings.development.LogicalCameraDefaultPreferenceController.USERDEBUG_BUILD;
import static com.android.settings.development.LogicalCameraDefaultPreferenceController.USER_BUILD;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
public class LogicalCameraDefaultPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private LogicalCameraDefaultPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new LogicalCameraDefaultPreferenceController(RuntimeEnvironment.application);
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
        SystemProperties.set(LogicalCameraDefaultPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_withEngBuild_shouldReturnTrue() {
        SystemProperties.set(LogicalCameraDefaultPreferenceController.BUILD_TYPE, ENG_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_withUserBuild_shouldReturnTrue() {
        SystemProperties.set(LogicalCameraDefaultPreferenceController.BUILD_TYPE, USER_BUILD);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_logicalCameraDefaultEnabled_shouldCheckedPreference() {
        SystemProperties.set(LogicalCameraDefaultPreferenceController.PROPERTY_LOGICAL_CAMERA_DEFAULT,
                Integer.toString(LogicalCameraDefaultPreferenceController.ENABLED));
        SystemProperties.set(LogicalCameraDefaultPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mScreen);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_logicalCameraDefaultEnabled_shouldUncheckedPreference() {
        SystemProperties.set(LogicalCameraDefaultPreferenceController.PROPERTY_LOGICAL_CAMERA_DEFAULT,
                Integer.toString(LogicalCameraDefaultPreferenceController.DISABLED));
        SystemProperties.set(
                LogicalCameraDefaultPreferenceController.BUILD_TYPE, USERDEBUG_BUILD);

        mController.updateState(mScreen);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_preferenceChecked_shouldEnableLogicalCameraDefault() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(Integer.toString(LogicalCameraDefaultPreferenceController.ENABLED)).isEqualTo(
                SystemProperties.get(
                        LogicalCameraDefaultPreferenceController.PROPERTY_LOGICAL_CAMERA_DEFAULT,
                        Integer.toString(LogicalCameraDefaultPreferenceController.ENABLED)));
    }

    @Test
    public void onPreferenceChange_preferenceUnchecked_shouldDisableLogicalCameraDefault() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(Integer.toString(LogicalCameraDefaultPreferenceController.DISABLED)).isEqualTo(
                SystemProperties.get(
                        LogicalCameraDefaultPreferenceController.PROPERTY_LOGICAL_CAMERA_DEFAULT,
                        Integer.toString(LogicalCameraDefaultPreferenceController.ENABLED)));
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
        assertThat(Integer.toString(LogicalCameraDefaultPreferenceController.DISABLED)).isEqualTo(
                SystemProperties.get(
                        LogicalCameraDefaultPreferenceController.PROPERTY_LOGICAL_CAMERA_DEFAULT,
                        Integer.toString(LogicalCameraDefaultPreferenceController.ENABLED)));
    }
}
