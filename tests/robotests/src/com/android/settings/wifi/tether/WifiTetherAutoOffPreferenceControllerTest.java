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

package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherAutoOffPreferenceControllerTest {

    private static final String KEY_PREF = "wifi_tether_auto_off";
    private static final int ON = 1;
    private static final int OFF = 0;
    private Context mContext;
    private WifiTetherAutoOffPreferenceController mController;
    private SwitchPreference mSwitchPreference;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mController = new WifiTetherAutoOffPreferenceController(mContext, KEY_PREF);
        mSwitchPreference = new SwitchPreference(mContext);
    }

    @Test
    public void testOnPreferenceChange_toggleOn_settingsOn() {
        mController.onPreferenceChange(null, true);

        assertThat(getAutoOffSetting()).isEqualTo(ON);
    }

    @Test
    public void testOnPreferenceChange_toggleOff_settingsOff() {
        mController.onPreferenceChange(null, false);

        assertThat(getAutoOffSetting()).isEqualTo(OFF);
    }

    @Test
    public void testUpdateState_settingsOn_toggleOn() {
        setAutoOffSetting(ON);

        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_settingsOff_toggleOff() {
        setAutoOffSetting(OFF);

        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_toggleDefaultOn() {
        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    private int getAutoOffSetting() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SOFT_AP_TIMEOUT_ENABLED, OFF);
    }

    private void setAutoOffSetting(int config) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SOFT_AP_TIMEOUT_ENABLED, config);
    }
}
