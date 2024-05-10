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

package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.Context;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.DockDefenderTip;

/** Detect whether the dock defender mode is enabled. */
public class DockDefenderDetector implements BatteryTipDetector {
    private final BatteryInfo mBatteryInfo;
    private final Context mContext;

    public DockDefenderDetector(BatteryInfo batteryInfo, Context context) {
        mBatteryInfo = batteryInfo;
        mContext = context;
    }

    @Override
    public BatteryTip detect() {
        int mode = BatteryUtils.getCurrentDockDefenderMode(mContext, mBatteryInfo);
        return new DockDefenderTip(
                mode != BatteryUtils.DockDefenderMode.DISABLED
                        ? BatteryTip.StateType.NEW
                        : BatteryTip.StateType.INVISIBLE,
                mode);
    }
}
