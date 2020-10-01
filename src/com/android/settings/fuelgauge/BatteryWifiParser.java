/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.os.BatteryStats.HistoryItem;
import android.os.BatteryStatsManager;

public class BatteryWifiParser extends BatteryFlagParser {

    public BatteryWifiParser(int accentColor) {
        super(accentColor, false, 0);
    }

    @Override
    protected boolean isSet(HistoryItem record) {
        switch ((record.states2 & HistoryItem.STATE2_WIFI_SUPPL_STATE_MASK)
                >> HistoryItem.STATE2_WIFI_SUPPL_STATE_SHIFT) {
            case BatteryStatsManager.WIFI_SUPPL_STATE_DISCONNECTED:
            case BatteryStatsManager.WIFI_SUPPL_STATE_DORMANT:
            case BatteryStatsManager.WIFI_SUPPL_STATE_INACTIVE:
            case BatteryStatsManager.WIFI_SUPPL_STATE_INTERFACE_DISABLED:
            case BatteryStatsManager.WIFI_SUPPL_STATE_INVALID:
            case BatteryStatsManager.WIFI_SUPPL_STATE_UNINITIALIZED:
                return false;
        }
        return true;
    }
}
