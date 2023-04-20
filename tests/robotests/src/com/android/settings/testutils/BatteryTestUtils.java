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

package com.android.settings.testutils;

import android.content.Intent;
import android.os.BatteryManager;

public class BatteryTestUtils {

    public static Intent getChargingIntent() {
        return getCustomBatteryIntent(
                BatteryManager.BATTERY_PLUGGED_AC,
                50 /* level */,
                100 /* scale */,
                BatteryManager.BATTERY_STATUS_CHARGING);
    }

    public static Intent getDischargingIntent() {
        return getCustomBatteryIntent(
                0 /* plugged */,
                10 /* level */,
                100 /* scale */,
                BatteryManager.BATTERY_STATUS_DISCHARGING);
    }

    public static Intent getCustomBatteryIntent(int plugged, int level, int scale, int status) {
        Intent intent = new Intent();
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, plugged);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, scale);
        intent.putExtra(BatteryManager.EXTRA_STATUS, status);

        return intent;
    }
}
