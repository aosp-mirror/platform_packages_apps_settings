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
import android.provider.Settings;

import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SmartBatteryTip;

/**
 * Detect whether to show smart battery tip.
 */
public class SmartBatteryDetector implements BatteryTipDetector {
    private BatteryTipPolicy mPolicy;
    private ContentResolver mContentResolver;

    public SmartBatteryDetector(BatteryTipPolicy policy, ContentResolver contentResolver) {
        mPolicy = policy;
        mContentResolver = contentResolver;
    }

    @Override
    public BatteryTip detect() {
        // Show it if there is no other tips shown
        final boolean smartBatteryOff = Settings.Global.getInt(mContentResolver,
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, 1) == 0
                || mPolicy.testSmartBatteryTip;
        final int state =
                smartBatteryOff ? BatteryTip.StateType.NEW : BatteryTip.StateType.INVISIBLE;
        return new SmartBatteryTip(state);
    }
}
