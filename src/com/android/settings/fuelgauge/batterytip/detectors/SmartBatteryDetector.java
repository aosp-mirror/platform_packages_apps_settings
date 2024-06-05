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

package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SmartBatteryTip;

/** Detect whether to show smart battery tip. */
public class SmartBatteryDetector implements BatteryTipDetector {
    private static final int EXPECTED_BATTERY_LEVEL = 30;

    private final BatteryInfo mBatteryInfo;
    private final BatteryTipPolicy mPolicy;
    private final ContentResolver mContentResolver;
    private final boolean mIsPowerSaveMode;

    public SmartBatteryDetector(
            Context context,
            BatteryTipPolicy policy,
            BatteryInfo batteryInfo,
            ContentResolver contentResolver,
            boolean isPowerSaveMode) {
        mPolicy = policy;
        mBatteryInfo = batteryInfo;
        mContentResolver = contentResolver;
        mIsPowerSaveMode = isPowerSaveMode;
    }

    @Override
    public BatteryTip detect() {
        final boolean smartBatteryOff =
                Settings.Global.getInt(
                                mContentResolver,
                                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED,
                                1)
                        == 0;
        final boolean isUnderExpectedBatteryLevel =
                mBatteryInfo.batteryLevel <= EXPECTED_BATTERY_LEVEL;
        // Show it if in test or smart battery is off.
        final boolean enableSmartBatteryTip =
                smartBatteryOff && !mIsPowerSaveMode && isUnderExpectedBatteryLevel
                        || mPolicy.testSmartBatteryTip;
        final int state =
                enableSmartBatteryTip ? BatteryTip.StateType.NEW : BatteryTip.StateType.INVISIBLE;
        return new SmartBatteryTip(state);
    }
}
