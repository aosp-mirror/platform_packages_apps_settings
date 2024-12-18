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

import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.utcToLocalTimeForLogging;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.android.settings.fuelgauge.batteryusage.BatteryReattribute;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

/** A {@link Entity} for battery usage reattribution data in the database. */
@Entity
public class BatteryReattributeEntity {

    /** The start timestamp of this record data. */
    @PrimaryKey
    public final long timestampStart;

    /** The end timestamp of this record data. */
    public final long timestampEnd;

    /** The battery usage reattribution data for corresponding  uids. */
    @NonNull public final String reattributeData;

    public BatteryReattributeEntity(@NonNull BatteryReattribute batteryReattribute) {
        this(
                batteryReattribute.getTimestampStart(),
                batteryReattribute.getTimestampEnd(),
                ConvertUtils.encodeBatteryReattribute(batteryReattribute));
    }

    @VisibleForTesting
    BatteryReattributeEntity(
            long timestampStart, long timestampEnd, @NonNull String reattributeData) {
        this.timestampStart = timestampStart;
        this.timestampEnd = timestampEnd;
        this.reattributeData = reattributeData;
    }

    @NonNull
    @Override
    public String toString() {
        final BatteryReattribute batteryReattribute =
                ConvertUtils.decodeBatteryReattribute(reattributeData);
        final StringBuilder builder = new StringBuilder()
                .append("\nBatteryReattributeEntity{")
                .append("\n\t" + utcToLocalTimeForLogging(timestampStart))
                .append("\n\t" + utcToLocalTimeForLogging(timestampEnd))
                .append("\n\t" + batteryReattribute);
        if (batteryReattribute != null) {
            builder.append("\n\t" + batteryReattribute.getReattributeDataMap());
        }
        return builder.append("\n}").toString();
    }
}
