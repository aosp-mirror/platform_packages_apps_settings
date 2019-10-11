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

package com.android.settings.fuelgauge.batterytip;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryTipPolicyTest {

    private static final String BATTERY_TIP_CONSTANTS_VALUE = "battery_tip_enabled=true"
            + ",summary_enabled=false"
            + ",battery_saver_tip_enabled=false"
            + ",high_usage_enabled=true"
            + ",high_usage_app_count=5"
            + ",high_usage_period_ms=2000"
            + ",high_usage_battery_draining=30"
            + ",app_restriction_enabled=true"
            + ",reduced_battery_enabled=true"
            + ",reduced_battery_percent=30"
            + ",low_battery_enabled=false"
            + ",low_battery_hour=10"
            + ",data_history_retain_day=24"
            + ",excessive_bg_drain_percentage=25"
            + ",test_battery_saver_tip=true"
            + ",test_high_usage_tip=false"
            + ",test_smart_battery_tip=true"
            + ",test_low_battery_tip=true"
            + ",app_restriction_active_hour=6";
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testInit_usesConfigValues() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.BATTERY_TIP_CONSTANTS, BATTERY_TIP_CONSTANTS_VALUE);

        final BatteryTipPolicy batteryTipPolicy = new BatteryTipPolicy(mContext);

        assertThat(batteryTipPolicy.batteryTipEnabled).isTrue();
        assertThat(batteryTipPolicy.summaryEnabled).isFalse();
        assertThat(batteryTipPolicy.batterySaverTipEnabled).isFalse();
        assertThat(batteryTipPolicy.highUsageEnabled).isTrue();
        assertThat(batteryTipPolicy.highUsageAppCount).isEqualTo(5);
        assertThat(batteryTipPolicy.highUsagePeriodMs).isEqualTo(2000);
        assertThat(batteryTipPolicy.highUsageBatteryDraining).isEqualTo(30);
        assertThat(batteryTipPolicy.appRestrictionEnabled).isTrue();
        assertThat(batteryTipPolicy.appRestrictionActiveHour).isEqualTo(6);
        assertThat(batteryTipPolicy.reducedBatteryEnabled).isTrue();
        assertThat(batteryTipPolicy.reducedBatteryPercent).isEqualTo(30);
        assertThat(batteryTipPolicy.lowBatteryEnabled).isFalse();
        assertThat(batteryTipPolicy.lowBatteryHour).isEqualTo(10);
        assertThat(batteryTipPolicy.dataHistoryRetainDay).isEqualTo(24);
        assertThat(batteryTipPolicy.excessiveBgDrainPercentage).isEqualTo(25);
        assertThat(batteryTipPolicy.testBatterySaverTip).isTrue();
        assertThat(batteryTipPolicy.testHighUsageTip).isFalse();
        assertThat(batteryTipPolicy.testSmartBatteryTip).isTrue();
        assertThat(batteryTipPolicy.testLowBatteryTip).isTrue();
    }

    @Test
    public void testInit_defaultValues() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.BATTERY_TIP_CONSTANTS, "");

        final BatteryTipPolicy batteryTipPolicy = new BatteryTipPolicy(mContext);

        assertThat(batteryTipPolicy.batteryTipEnabled).isTrue();
        assertThat(batteryTipPolicy.summaryEnabled).isTrue();
        assertThat(batteryTipPolicy.batterySaverTipEnabled).isTrue();
        assertThat(batteryTipPolicy.highUsageEnabled).isTrue();
        assertThat(batteryTipPolicy.highUsageAppCount).isEqualTo(3);
        assertThat(batteryTipPolicy.highUsagePeriodMs).isEqualTo(2 * DateUtils.HOUR_IN_MILLIS);
        assertThat(batteryTipPolicy.highUsageBatteryDraining).isEqualTo(25);
        assertThat(batteryTipPolicy.appRestrictionEnabled).isTrue();
        assertThat(batteryTipPolicy.appRestrictionActiveHour).isEqualTo(24);
        assertThat(batteryTipPolicy.reducedBatteryEnabled).isFalse();
        assertThat(batteryTipPolicy.reducedBatteryPercent).isEqualTo(50);
        assertThat(batteryTipPolicy.lowBatteryEnabled).isTrue();
        assertThat(batteryTipPolicy.lowBatteryHour).isEqualTo(3);
        assertThat(batteryTipPolicy.dataHistoryRetainDay).isEqualTo(30);
        assertThat(batteryTipPolicy.excessiveBgDrainPercentage).isEqualTo(10);
        assertThat(batteryTipPolicy.testBatterySaverTip).isFalse();
        assertThat(batteryTipPolicy.testHighUsageTip).isFalse();
        assertThat(batteryTipPolicy.testSmartBatteryTip).isFalse();
        assertThat(batteryTipPolicy.testLowBatteryTip).isFalse();
    }
}
