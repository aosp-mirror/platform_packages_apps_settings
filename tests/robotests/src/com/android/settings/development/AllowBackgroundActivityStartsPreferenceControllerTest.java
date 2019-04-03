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

import static com.android.settings.development.AllowBackgroundActivityStartsPreferenceController.KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class AllowBackgroundActivityStartsPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private AllowBackgroundActivityStartsPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new AllowBackgroundActivityStartsPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_allowBackgroundActivityStartsShouldBeOn() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        assertThat(getModeFroMSettings()).isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_settingDisabled_allowBackgroundActivityStartsShouldBeOff() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        assertThat(getModeFroMSettings()).isEqualTo(0);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BACKGROUND_ACTIVITY_STARTS_ENABLED, 0);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
   }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BACKGROUND_ACTIVITY_STARTS_ENABLED, 1);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingReset_defaultDisabled_preferenceShouldNotBeChecked() {
        setDefault(false);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_settingReset_defaultEnabled_preferenceShouldBeChecked() {
        setDefault(true);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_noDefault_shouldResetPreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setChecked(true);
        verify(mPreference).setEnabled(false);

        assertThat(getModeFroMSettings()).isEqualTo(-1);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_defaultDisabled_shouldResetPreference() {
        setDefault(false);
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);

        assertThat(getModeFroMSettings()).isEqualTo(-1);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_defaultEnabled_shouldResetPreference() {
        setDefault(true);
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setChecked(true);
        verify(mPreference).setEnabled(false);

        assertThat(getModeFroMSettings()).isEqualTo(-1);
    }

    private int getModeFroMSettings() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.BACKGROUND_ACTIVITY_STARTS_ENABLED, 999 /* default */);
    }

    private void setDefault(boolean defaultEnabled) {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_ACTIVITY_MANAGER,
                KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED,
                Boolean.toString(defaultEnabled),
                false /* makeDefault */);
    }
}

