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

package com.android.settings.fuelgauge.batteryusage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/** DAO for accessing {@link BatteryReattributeEntity} in the database. */
@Dao
interface BatteryReattributeDao {

    /** Inserts a {@link BatteryReattributeEntity} data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(event: BatteryReattributeEntity)

    /** Gets all recorded data after a specific timestamp. */
    @Query(
            "SELECT * FROM BatteryReattributeEntity WHERE "
                    + "timestampStart >= :timestampStart ORDER BY timestampStart DESC")
    fun getAllAfter(timestampStart: Long): List<BatteryReattributeEntity>

    /** Deletes all recorded data before a specific timestamp. */
    @Query("DELETE FROM BatteryReattributeEntity WHERE timestampStart <= :timestampStart")
    fun clearAllBefore(timestampStart: Long)

    /** Deletes all recorded data after a specific timestamp. */
    @Query("DELETE FROM BatteryReattributeEntity WHERE timestampStart >= :timestampStart")
    fun clearAllAfter(timestampStart: Long)

    /** Clears all recorded data in the database. */
    @Query("DELETE FROM BatteryReattributeEntity") fun clearAll()
}
