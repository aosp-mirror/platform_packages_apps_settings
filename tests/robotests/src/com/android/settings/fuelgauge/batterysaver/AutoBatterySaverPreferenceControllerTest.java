/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class AutoBatterySaverPreferenceControllerTest {

    private AutoBatterySaverPreferenceController mController;
    private Context mContext;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new AutoBatterySaverPreferenceController(mContext);
    }

    @Test
    public void testUpdateState_lowPowerLevelZero_preferenceNotChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_lowPowerLevelZero_preferenceChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 15);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testOnPreferenceChange_turnOn_setValueNotZero() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0)).isNotEqualTo(0);
    }

    @Test
    public void testOnPreferenceChange_turnOff_setValueZero() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0)).isEqualTo(0);
    }

    @Test
    public void testIsChecked_useDefaultValue_returnFalse() {
        assertThat(mController.isChecked()).isFalse();
    }
}
