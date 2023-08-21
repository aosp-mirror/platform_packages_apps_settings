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

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Data access object for accessing {@link BatteryState} in the database. */
@Dao
public interface BatteryStateDao {

    /** Inserts a {@link BatteryState} data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BatteryState state);

    /** Inserts {@link BatteryState} data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BatteryState> states);

    /** Gets the {@link Cursor} of the latest record timestamp no later than the given timestamp. */
    @Query("SELECT MAX(timestamp) FROM BatteryState WHERE timestamp <= :timestamp")
    Cursor getLatestTimestampBefore(long timestamp);

    /** Lists all recorded battery states after a specific timestamp. */
    @Query("SELECT * FROM BatteryState WHERE timestamp >= :timestamp ORDER BY timestamp ASC")
    Cursor getBatteryStatesAfter(long timestamp);

    /** Lists all recorded data after a specific timestamp. */
    @Query("SELECT * FROM BatteryState WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    List<BatteryState> getAllAfter(long timestamp);

    /** Get the count of distinct timestamp after a specific timestamp. */
    @Query("SELECT COUNT(DISTINCT timestamp) FROM BatteryState WHERE timestamp > :timestamp")
    int getDistinctTimestampCount(long timestamp);

    /** Lists all distinct timestamps after a specific timestamp. */
    @Query("SELECT DISTINCT timestamp FROM BatteryState WHERE timestamp > :timestamp")
    List<Long> getDistinctTimestamps(long timestamp);

    /** Deletes all recorded data before a specific timestamp. */
    @Query("DELETE FROM BatteryState WHERE timestamp <= :timestamp")
    void clearAllBefore(long timestamp);

    /** Clears all recorded data in the database. */
    @Query("DELETE FROM BatteryState")
    void clearAll();
}
