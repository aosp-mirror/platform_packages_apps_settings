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

/** Data access object for accessing {@link AppUsageEventEntity} in the database. */
@Dao
public interface AppUsageEventDao {

    /** Inserts a {@link AppUsageEventEntity} data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppUsageEventEntity event);

    /** Inserts {@link AppUsageEventEntity} data into the database. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppUsageEventEntity> events);

    /** Lists all recorded data after a specific timestamp. */
    @Query("SELECT * FROM AppUsageEventEntity WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    List<AppUsageEventEntity> getAllAfter(long timestamp);

    /** Gets the {@link Cursor} of all recorded data after a specific timestamp of the users. */
    @Query(
            "SELECT * FROM AppUsageEventEntity WHERE timestamp >= :timestamp"
                    + " AND userId IN (:userIds) ORDER BY timestamp ASC")
    Cursor getAllForUsersAfter(List<Long> userIds, long timestamp);

    /** Gets the {@link Cursor} of the latest timestamp of the specific user. */
    @Query("SELECT MAX(timestamp) as timestamp FROM AppUsageEventEntity WHERE userId = :userId")
    Cursor getLatestTimestampOfUser(long userId);

    /** Deletes all recorded data before a specific timestamp. */
    @Query("DELETE FROM AppUsageEventEntity WHERE timestamp <= :timestamp")
    void clearAllBefore(long timestamp);

    /** Clears all recorded data in the database. */
    @Query("DELETE FROM AppUsageEventEntity")
    void clearAll();
}
