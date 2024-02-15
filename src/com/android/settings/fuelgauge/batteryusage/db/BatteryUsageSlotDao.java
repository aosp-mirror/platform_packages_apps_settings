/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Data access object for accessing {@link BatteryUsageSlotEntity} in the database. */
@Dao
public interface BatteryUsageSlotDao {
    /** Inserts a {@link BatteryUsageSlotEntity} data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BatteryUsageSlotEntity event);

    /** Gets all recorded data. */
    @Query("SELECT * FROM BatteryUsageSlotEntity ORDER BY timestamp ASC")
    List<BatteryUsageSlotEntity> getAll();

    /** Gets the {@link Cursor} of all recorded data after a specific timestamp. */
    @Query(
            "SELECT * FROM BatteryUsageSlotEntity WHERE timestamp >= :timestamp"
                    + " ORDER BY timestamp ASC")
    Cursor getAllAfter(long timestamp);

    /** Gets all recorded data after a specific timestamp for log.*/
    @Query(
            "SELECT * FROM BatteryUsageSlotEntity WHERE timestamp >= :timestamp"
                    + " ORDER BY timestamp DESC")
    List<BatteryUsageSlotEntity> getAllAfterForLog(long timestamp);

    /** Deletes all recorded data before a specific timestamp. */
    @Query("DELETE FROM BatteryUsageSlotEntity WHERE timestamp <= :timestamp")
    void clearAllBefore(long timestamp);

    /** Clears all recorded data in the database. */
    @Query("DELETE FROM BatteryUsageSlotEntity")
    void clearAll();
}
