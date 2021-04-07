/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.tips.BatteryDefenderTip;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

/**
 * Detect whether the battery is overheated
 */
public class BatteryDefenderDetector implements BatteryTipDetector {
    private BatteryInfo mBatteryInfo;

    public BatteryDefenderDetector(BatteryInfo batteryInfo) {
        mBatteryInfo = batteryInfo;
    }

    @Override
    public BatteryTip detect() {
        final int state =
                BatteryUtils.isBatteryDefenderOn(mBatteryInfo)
                        ? BatteryTip.StateType.NEW
                        : BatteryTip.StateType.INVISIBLE;
        return new BatteryDefenderTip(state);
    }
}
