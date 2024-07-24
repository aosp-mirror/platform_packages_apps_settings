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

package com.android.settings.fuelgauge.batteryusage.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** A {@link RoomDatabase} for battery usage states history. */
@Database(
        entities = {
            AppUsageEventEntity.class,
            BatteryEventEntity.class,
            BatteryState.class,
            BatteryUsageSlotEntity.class
        },
        version = 1)
public abstract class BatteryStateDatabase extends RoomDatabase {
    private static final String TAG = "BatteryStateDatabase";

    private static BatteryStateDatabase sBatteryStateDatabase;

    /** Provides DAO for app usage event table. */
    public abstract AppUsageEventDao appUsageEventDao();

    /** Provides DAO for battery event table. */
    public abstract BatteryEventDao batteryEventDao();

    /** Provides DAO for battery state table. */
    public abstract BatteryStateDao batteryStateDao();

    /** Provides DAO for battery usage slot table. */
    public abstract BatteryUsageSlotDao batteryUsageSlotDao();

    /** Gets or creates an instance of {@link RoomDatabase}. */
    public static BatteryStateDatabase getInstance(Context context) {
        if (sBatteryStateDatabase == null) {
            sBatteryStateDatabase =
                    Room.databaseBuilder(context, BatteryStateDatabase.class, "battery-usage-db-v9")
                            // Allows accessing data in the main thread for dumping bugreport.
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
            Log.d(TAG, "initialize battery states database");
        }
        return sBatteryStateDatabase;
    }

    /** Sets the instance of {@link RoomDatabase}. */
    public static void setBatteryStateDatabase(BatteryStateDatabase database) {
        BatteryStateDatabase.sBatteryStateDatabase = database;
    }
}
