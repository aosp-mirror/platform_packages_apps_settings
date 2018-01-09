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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SmartBatteryPreferenceControllerTest {
    private static final int ON = 1;
    private static final int OFF = 0;

    private SmartBatteryPreferenceController mController;
    private SwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mController = new SmartBatteryPreferenceController(mContext);
        mPreference = new SwitchPreference(mContext);
    }

    @Test
    public void testUpdateState_smartBatteryOn_preferenceChecked() {
        putSmartBatteryValue(ON);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_smartBatteryOff_preferenceUnchecked() {
        putSmartBatteryValue(OFF);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_checkPreference_smartBatteryOn() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(getSmartBatteryValue()).isEqualTo(ON);
    }

    @Test
    public void testUpdateState_unCheckPreference_smartBatteryOff() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(getSmartBatteryValue()).isEqualTo(OFF);
    }

    private void putSmartBatteryValue(int value) {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.APP_STANDBY_ENABLED,
                value);
    }

    private int getSmartBatteryValue() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.APP_STANDBY_ENABLED, ON);
    }
}
