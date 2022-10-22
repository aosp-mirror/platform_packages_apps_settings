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

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.UserManager;

import androidx.room.Room;

import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;

import com.google.common.collect.ImmutableList;

import org.robolectric.Shadows;

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

    /** Sets the work profile mode. */
    public static void setWorkProfile(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        Shadows.shadowOf(userManager).setManagedProfile(true);
        Shadows.shadowOf(userManager).setIsSystemUser(false);
    }

    /** Creates and sets up the in-memory {@link BatteryStateDatabase}. */
    public static BatteryStateDatabase setUpBatteryStateDatabase(Context context) {
        final BatteryStateDatabase inMemoryDatabase =
                Room.inMemoryDatabaseBuilder(context, BatteryStateDatabase.class)
                        .allowMainThreadQueries()
                        .build();
        BatteryStateDatabase.setBatteryStateDatabase(inMemoryDatabase);
        return inMemoryDatabase;
    }

    /** Inserts a fake data into the database for testing. */
    public static void insertDataToBatteryStateDatabase(
            Context context, long timestamp, String packageName) {
        insertDataToBatteryStateDatabase(context, timestamp, packageName, /*multiple=*/ false);
    }

    /** Inserts a fake data into the database for testing. */
    public static void insertDataToBatteryStateDatabase(
            Context context, long timestamp, String packageName, boolean multiple) {
        final BatteryState state =
                new BatteryState(
                        /*uid=*/ 1001L,
                        /*userId=*/ 100L,
                        /*appLabel=*/ "Settings",
                        packageName,
                        /*isHidden=*/ true,
                        /*bootTimestamp=*/ timestamp - 1,
                        timestamp,
                        /*zoneId=*/ "Europe/Paris",
                        /*totalPower=*/ 100f,
                        /*consumePower=*/ 0.3f,
                        /*percentOfTotal=*/ 10f,
                        /*foregroundUsageTimeInMs=*/ 60000,
                        /*backgroundUsageTimeInMs=*/ 10000,
                        /*drainType=*/ 1,
                        /*consumerType=*/ 2,
                        /*batteryLevel=*/ 31,
                        /*batteryStatus=*/ 0,
                        /*batteryHealth=*/ 0);
        BatteryStateDao dao =
                BatteryStateDatabase.getInstance(context).batteryStateDao();
        if (multiple) {
            dao.insertAll(ImmutableList.of(state));
        } else {
            dao.insert(state);
        }
    }

    private static Intent getCustomBatteryIntent(int plugged, int level, int scale, int status) {
        Intent intent = new Intent();
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, plugged);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, scale);
        intent.putExtra(BatteryManager.EXTRA_STATUS, status);

        return intent;
    }
}
