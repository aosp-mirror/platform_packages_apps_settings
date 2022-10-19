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

import android.content.Intent;
import android.os.BatteryManager;

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

    private Intent mBatteryIntent;

    @Before
    public void setUp() {
        mBatteryIntent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        // Inserts the battery states into intent.
        mBatteryIntent.putExtra(BatteryManager.EXTRA_LEVEL, BATTERY_LEVEL);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_STATUS, BATTERY_STATUS);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_HEALTH, BATTERY_HEALTH);
    }

    @Test
    public void testBuilder_returnsExpectedResult() {
        mBatteryIntent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        BatteryState state = create(mBatteryIntent);

        // Verifies the app relative information.
        assertThat(state.uid).isEqualTo(1001L);
        assertThat(state.userId).isEqualTo(100L);
        assertThat(state.appLabel).isEqualTo("Settings");
        assertThat(state.packageName).isEqualTo("com.android.settings");
        assertThat(state.isHidden).isTrue();
        assertThat(state.bootTimestamp).isEqualTo(101L);
        assertThat(state.timestamp).isEqualTo(100001L);
        // Verifies the battery relative information.
        assertThat(state.totalPower).isEqualTo(100);
        assertThat(state.consumePower).isEqualTo(3);
        assertThat(state.percentOfTotal).isEqualTo(10);
        assertThat(state.foregroundUsageTimeInMs).isEqualTo(60000);
        assertThat(state.backgroundUsageTimeInMs).isEqualTo(10000);
        assertThat(state.drainType).isEqualTo(1);
        assertThat(state.consumerType).isEqualTo(2);
        assertThat(state.batteryLevel).isEqualTo(BATTERY_LEVEL);
        assertThat(state.batteryStatus).isEqualTo(BATTERY_STATUS);
        assertThat(state.batteryHealth).isEqualTo(BATTERY_HEALTH);
    }

    @Test
    public void create_withoutBatteryScale_returnsStateWithInvalidLevel() {
        BatteryState state = create(mBatteryIntent);
        assertThat(state.batteryLevel).isEqualTo(-1);
    }

    private static BatteryState create(Intent intent) {
        return BatteryState.newBuilder()
                .setUid(1001L)
                .setUserId(100L)
                .setAppLabel("Settings")
                .setPackageName("com.android.settings")
                .setIsHidden(true)
                .setBootTimestamp(101L)
                .setTimestamp(100001L)
                .setTotalPower(100f)
                .setConsumePower(3f)
                .setPercentOfTotal(10f)
                .setForegroundUsageTimeInMs(60000)
                .setBackgroundUsageTimeInMs(10000)
                .setDrainType(1)
                .setConsumerType(2)
                .setBatteryIntent(intent)
                .build();
    }
}
