/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.db;

import static com.google.common.truth.Truth.assertThat;

import android.os.BatteryManager;

import com.android.settings.fuelgauge.batteryusage.BatteryInformation;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;
import com.android.settings.fuelgauge.batteryusage.DeviceBatteryState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BatteryState}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryStateTest {
    private static final int BATTERY_LEVEL = 45;
    private static final int BATTERY_STATUS = BatteryManager.BATTERY_STATUS_FULL;
    private static final int BATTERY_HEALTH = BatteryManager.BATTERY_HEALTH_COLD;

    private BatteryInformation mBatteryInformation;

    @Before
    public void setUp() {
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState.newBuilder()
                        .setBatteryLevel(BATTERY_LEVEL)
                        .setBatteryStatus(BATTERY_STATUS)
                        .setBatteryHealth(BATTERY_HEALTH)
                        .build();
        mBatteryInformation =
                BatteryInformation.newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .setBootTimestamp(101L)
                        .setIsHidden(true)
                        .setAppLabel("Settings")
                        .setTotalPower(100)
                        .setConsumePower(3)
                        .setForegroundUsageConsumePower(0)
                        .setForegroundServiceUsageConsumePower(1)
                        .setBackgroundUsageConsumePower(2)
                        .setCachedUsageConsumePower(3)
                        .setPercentOfTotal(10)
                        .setDrainType(1)
                        .setForegroundUsageTimeInMs(60000)
                        .setBackgroundUsageTimeInMs(10000)
                        .build();
    }

    @Test
    public void testBuilder_returnsExpectedResult() {
        BatteryState state = create(mBatteryInformation);

        // Verifies the app relative information.
        assertThat(state.uid).isEqualTo(1001L);
        assertThat(state.userId).isEqualTo(100L);
        assertThat(state.packageName).isEqualTo("com.android.settings");
        assertThat(state.timestamp).isEqualTo(100001L);
        assertThat(state.consumerType).isEqualTo(2);
        assertThat(state.isFullChargeCycleStart).isTrue();
        assertThat(state.batteryInformation)
                .isEqualTo(ConvertUtils.convertBatteryInformationToString(mBatteryInformation));
    }

    private static BatteryState create(BatteryInformation batteryInformation) {
        return BatteryState.newBuilder()
                .setUid(1001L)
                .setUserId(100L)
                .setPackageName("com.android.settings")
                .setTimestamp(100001L)
                .setConsumerType(2)
                .setIsFullChargeCycleStart(true)
                .setBatteryInformation(
                        ConvertUtils.convertBatteryInformationToString(batteryInformation))
                .build();
    }
}
