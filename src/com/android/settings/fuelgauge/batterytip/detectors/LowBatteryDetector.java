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

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;

/** Detect whether the battery is too low */
public class LowBatteryDetector implements BatteryTipDetector {
    private final BatteryInfo mBatteryInfo;
    private final BatteryTipPolicy mPolicy;
    private final boolean mIsPowerSaveMode;
    private final int mWarningLevel;

    public LowBatteryDetector(
            Context context,
            BatteryTipPolicy policy,
            BatteryInfo batteryInfo,
            boolean isPowerSaveMode) {
        mPolicy = policy;
        mBatteryInfo = batteryInfo;
        mWarningLevel =
                context.getResources()
                        .getInteger(com.android.internal.R.integer.config_lowBatteryWarningLevel);
        mIsPowerSaveMode = isPowerSaveMode;
    }

    @Override
    public BatteryTip detect() {
        final boolean lowBattery = mBatteryInfo.batteryLevel <= mWarningLevel;
        final boolean lowBatteryEnabled = mPolicy.lowBatteryEnabled && !mIsPowerSaveMode;
        final boolean dischargingLowBatteryState =
                mPolicy.testLowBatteryTip || (mBatteryInfo.discharging && lowBattery);

        int state = BatteryTip.StateType.INVISIBLE;

        // Show it as new if in test or in discharging low battery state,
        // dismiss it if battery saver is on or disabled by config.
        if (lowBatteryEnabled && dischargingLowBatteryState) {
            state = BatteryTip.StateType.NEW;
        }

        return new LowBatteryTip(state, mIsPowerSaveMode);
    }
}
