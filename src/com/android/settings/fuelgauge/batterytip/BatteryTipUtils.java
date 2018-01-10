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

package com.android.settings.fuelgauge.batterytip;

import android.app.Fragment;

import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.batterytip.actions.BatteryTipAction;
import com.android.settings.fuelgauge.batterytip.actions.SmartBatteryAction;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

/**
 * Utility class for {@link BatteryTip}
 */
public class BatteryTipUtils {

    /**
     * Get a corresponding action based on {@code batteryTip}
     * @param batteryTip used to detect which action to choose
     * @param settingsActivity used to populate {@link BatteryTipAction}
     * @param fragment used to populate {@link BatteryTipAction}
     * @return an action for {@code batteryTip}
     */
    public static BatteryTipAction getActionForBatteryTip(BatteryTip batteryTip,
            SettingsActivity settingsActivity, Fragment fragment) {
        switch (batteryTip.getType()) {
            case BatteryTip.TipType.SMART_BATTERY_MANAGER:
                return new SmartBatteryAction(settingsActivity, fragment);
            default:
                return null;
        }
    }
}
