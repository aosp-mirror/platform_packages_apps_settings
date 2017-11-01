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

package com.android.settings.notification;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BootSoundPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private BootSoundPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SettingsShadowSystemProperties.clear();
        when(mContext.getResources().getBoolean(com.android.settings.R.bool.has_boot_sounds))
            .thenReturn(true);
        mController = new BootSoundPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void isAvailable_hasBootSounds_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(
            com.android.settings.R.bool.has_boot_sounds)).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noBootSounds_shouldReturnFale() {
        when(mContext.getResources().getBoolean(
            com.android.settings.R.bool.has_boot_sounds)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void displayPreference_bootSoundEnabled_shouldCheckedPreference() {
        SettingsShadowSystemProperties.set(BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, "1");

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(true);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void displayPreference_bootSoundDisabled_shouldUncheckedPreference() {
        SettingsShadowSystemProperties.set(BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, "0");

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void handlePreferenceTreeClick_preferenceChecked_shouldEnableBootSound() {
        when(mPreference.isChecked()).thenReturn(true);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(SystemProperties.getBoolean(
            BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, true)).isTrue();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void handlePreferenceTreeClick_preferenceUnchecked_shouldDisableBootSound() {
        when(mPreference.isChecked()).thenReturn(false);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(SystemProperties.getBoolean(
            BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, true)).isFalse();
    }

}
