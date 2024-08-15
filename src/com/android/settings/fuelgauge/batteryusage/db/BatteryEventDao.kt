/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.fuelgauge.batteryusage.db

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.settings.fuelgauge.batteryusage.BatteryEventType

/** Data access object for accessing [BatteryEventEntity] in the database. */
@Dao
interface BatteryEventDao {
    /** Inserts a [BatteryEventEntity] data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(event: BatteryEventEntity)

    /** Gets all recorded data. */
    @Query("SELECT * FROM BatteryEventEntity ORDER BY timestamp DESC")
    fun getAll(): List<BatteryEventEntity>

    /** Gets the [Cursor] of the last full charge time. */
    @Query(
        "SELECT MAX(timestamp) FROM BatteryEventEntity" +
            " WHERE batteryEventType = 3" // BatteryEventType.FULL_CHARGED = 3
    )
    fun getLastFullChargeTimestamp(): Cursor

    /** Gets the [Long] of the last full charge time. */
    @Query(
        "SELECT MAX(timestamp) FROM BatteryEventEntity" +
            " WHERE batteryEventType = 3" // BatteryEventType.FULL_CHARGED = 3
    )
    fun getLastFullChargeTimestampForLog(): Long?

    /** Gets the [Cursor] of all recorded data after a specific timestamp. */
    @Query(
        "SELECT * FROM BatteryEventEntity" +
            " WHERE timestamp >= :timestamp AND batteryEventType IN (:batteryEventTypes)" +
            " ORDER BY timestamp DESC"
    )
    fun getAllAfter(timestamp: Long, batteryEventTypes: List<Int>): Cursor

    /** Gets all recorded data after a specific timestamp for log. */
    @Query(
        "SELECT * FROM BatteryEventEntity " +
            "WHERE timestamp >= :timestamp ORDER BY timestamp DESC"
    )
    fun getAllAfterForLog(timestamp: Long): List<BatteryEventEntity>

    /** Deletes all recorded data before a specific timestamp. */
    @Query("DELETE FROM BatteryEventEntity WHERE timestamp <= :timestamp")
    fun clearAllBefore(timestamp: Long)

    /** Deletes all recorded data after a specific timestamp. */
    @Query("DELETE FROM BatteryEventEntity WHERE timestamp >= :timestamp")
    fun clearAllAfter(timestamp: Long)

    /** Deletes even_hour event data in the database. */
    @Query(
        "DELETE FROM BatteryEventEntity " +
            "WHERE batteryEventType = 4" // BatteryEventType.EVEN_HOUR = 4
    )
    fun clearEvenHourEvent()

    /** Clears all recorded data in the database. */
    @Query("DELETE FROM BatteryEventEntity")
    fun clearAll()
}
