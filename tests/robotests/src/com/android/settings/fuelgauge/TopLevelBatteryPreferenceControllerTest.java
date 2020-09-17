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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.fuelgauge.TopLevelBatteryPreferenceController.getDashboardLabel;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class TopLevelBatteryPreferenceControllerTest {
    private Context mContext;
    private TopLevelBatteryPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new TopLevelBatteryPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_unsupportedWhenSet() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getDashboardLabel_returnsCorrectLabel() {
        BatteryInfo info = new BatteryInfo();
        info.batteryPercentString = "3%";
        assertThat(getDashboardLabel(mContext, info)).isEqualTo(info.batteryPercentString);

        info.remainingLabel = "Phone will shut down soon";
        assertThat(getDashboardLabel(mContext, info)).isEqualTo("3% - Phone will shut down soon");

        info.discharging = false;
        info.chargeLabel = "5% - charging";
        assertThat(getDashboardLabel(mContext, info)).isEqualTo("5% - charging");
    }
}
