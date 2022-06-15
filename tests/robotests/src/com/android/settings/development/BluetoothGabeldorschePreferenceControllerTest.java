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

package com.android.settings.development;

import static com.android.settings.development.BluetoothGabeldorschePreferenceController
        .CURRENT_GD_FLAG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.DeviceConfig;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothGabeldorschePreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private BluetoothGabeldorschePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Context context = RuntimeEnvironment.application;
        mController = new BluetoothGabeldorschePreferenceController(context);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_shouldTurnOnBluetoothGabeldorsche() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        boolean enabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_BLUETOOTH,
                CURRENT_GD_FLAG, false /* defaultValue */);

        assertThat(enabled).isTrue();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_shouldTurnOffBluetoothGabeldorsche() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        boolean enabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_BLUETOOTH,
                CURRENT_GD_FLAG, false /* defaultValue */);

        assertThat(enabled).isFalse();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_BLUETOOTH,
                CURRENT_GD_FLAG, "true", false /* makeDefault */);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_BLUETOOTH,
                CURRENT_GD_FLAG, "false", false /* makeDefault */);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();

        String configStr = DeviceConfig.getProperty(DeviceConfig.NAMESPACE_BLUETOOTH,
                CURRENT_GD_FLAG);

        assertThat(configStr).isNull();
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
