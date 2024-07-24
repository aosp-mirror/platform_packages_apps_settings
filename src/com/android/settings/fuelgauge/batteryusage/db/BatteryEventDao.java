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

/** Data access object for accessing {@link BatteryEventEntity} in the database. */
@Dao
public interface BatteryEventDao {
    /** Inserts a {@link BatteryEventEntity} data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BatteryEventEntity event);

    /** Gets all recorded data. */
    @Query("SELECT * FROM BatteryEventEntity ORDER BY timestamp DESC")
    List<BatteryEventEntity> getAll();

    /** Gets the {@link Cursor} of the last full charge time . */
    @Query(
            "SELECT MAX(timestamp) FROM BatteryEventEntity"
                    + " WHERE batteryEventType = 3") // BatteryEventType.FULL_CHARGED = 3
    Cursor getLastFullChargeTimestamp();

    /** Gets the {@link Long} of the last full charge time . */
    @Query(
            "SELECT MAX(timestamp) FROM BatteryEventEntity"
                    + " WHERE batteryEventType = 3") // BatteryEventType.FULL_CHARGED = 3
    Long getLastFullChargeTimestampForLog();

    /** Gets the {@link Cursor} of all recorded data after a specific timestamp. */
    @Query(
            "SELECT * FROM BatteryEventEntity"
                    + " WHERE timestamp >= :timestamp AND batteryEventType IN (:batteryEventTypes)"
                    + " ORDER BY timestamp DESC")
    Cursor getAllAfter(long timestamp, List<Integer> batteryEventTypes);

    /** Gets all recorded data after a specific timestamp for log.*/
    @Query(
            "SELECT * FROM BatteryEventEntity "
                    + "WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    List<BatteryEventEntity> getAllAfterForLog(long timestamp);

    /** Deletes all recorded data before a specific timestamp. */
    @Query("DELETE FROM BatteryEventEntity WHERE timestamp <= :timestamp")
    void clearAllBefore(long timestamp);

    /** Clears all recorded data in the database. */
    @Query("DELETE FROM BatteryEventEntity")
    void clearAll();
}
