/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Global;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverStickyPreferenceControllerTest {

    private static final String PREF_KEY = "battery_saver_sticky";

    private Context mContext;
    private BatterySaverStickyPreferenceController mController;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
        mController = new BatterySaverStickyPreferenceController(mContext, PREF_KEY);
    }

    private int getAutoDisableSetting() {
        return Settings.Global.getInt(mContext.getContentResolver(),
            Global.LOW_POWER_MODE_STICKY_AUTO_DISABLE_ENABLED,
                1);
    }

    @Test
    public void testOnPreferenceChange_turnOnAutoOff_autoDisableOn() {
        mController.setChecked(true);
        final int isOn = getAutoDisableSetting();
        assertThat(isOn).isEqualTo(1);
    }

    @Test
    public void testOnPreferenceChange_TurnOffAutoOff_autoDisableOff() {
        mController.setChecked(false);
        final int isOn = getAutoDisableSetting();
        assertThat(isOn).isEqualTo(0);
    }
}
