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

package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.Context;
import android.os.PowerManager;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;

import java.util.concurrent.TimeUnit;

/**
 * Detect whether the battery is too low
 */
public class LowBatteryDetector implements BatteryTipDetector {
    private BatteryInfo mBatteryInfo;
    private BatteryTipPolicy mPolicy;
    private PowerManager mPowerManager;
    private int mWarningLevel;

    public LowBatteryDetector(Context context, BatteryTipPolicy policy, BatteryInfo batteryInfo) {
        mPolicy = policy;
        mBatteryInfo = batteryInfo;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWarningLevel = context.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
    }

    @Override
    public BatteryTip detect() {
        final boolean powerSaveModeOn = mPowerManager.isPowerSaveMode();
        final boolean lowBattery = mBatteryInfo.batteryLevel <= mWarningLevel
                || (mBatteryInfo.discharging && mBatteryInfo.remainingTimeUs != 0
                && mBatteryInfo.remainingTimeUs < TimeUnit.HOURS.toMicros(mPolicy.lowBatteryHour));

        int state = BatteryTip.StateType.INVISIBLE;
        if (mPolicy.lowBatteryEnabled) {
            if (powerSaveModeOn) {
                // Show it is handled if battery saver is on
                state = BatteryTip.StateType.HANDLED;
            } else if (mPolicy.testLowBatteryTip || (mBatteryInfo.discharging && lowBattery)) {
                // Show it is new if in test or in discharging low battery state
                state = BatteryTip.StateType.NEW;
            }
        }

        return new LowBatteryTip(
                state, powerSaveModeOn, mBatteryInfo.suggestionLabel);
    }
}
