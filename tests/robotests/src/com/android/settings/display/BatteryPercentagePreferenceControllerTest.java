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

package com.android.settings.display;

import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryPercentagePreferenceControllerTest {

    private static final String PREF_KEY = "battery_percentage";

    private Context mContext;
    private BatteryPercentagePreferenceController mController;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
        mController = new BatteryPercentagePreferenceController(mContext, PREF_KEY);
    }

    private int getPercentageSetting() {
        return Settings.System.getInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT, 0);
    }

    @Test
    public void testOnPreferenceChange_TurnOnPercentage_PercentageOn() {
        mController.onPreferenceChange(null, true);
        final int isOn = getPercentageSetting();
        assertThat(isOn).isEqualTo(1);
    }

    @Test
    public void testOnPreferenceChange_TurnOffPercentage_PercentageOff() {
        mController.onPreferenceChange(null, false);
        final int isOn = getPercentageSetting();
        assertThat(isOn).isEqualTo(0);
    }
}
