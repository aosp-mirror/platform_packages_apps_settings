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

import android.content.ContentValues;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Locale;

/** A {@link Entity} class to save battery events into database. */
@Entity
public class BatteryEventEntity {
    /** Keys for accessing {@link ContentValues}. */
    public static final String KEY_TIMESTAMP = "timestamp";

    public static final String KEY_BATTERY_EVENT_TYPE = "batteryEventType";
    public static final String KEY_BATTERY_LEVEL = "batteryLevel";

    @PrimaryKey(autoGenerate = true)
    private long mId;

    public final long timestamp;
    public final int batteryEventType;
    public final int batteryLevel;

    public BatteryEventEntity(
            final long timestamp, final int batteryEventType, final int batteryLevel) {
        this.timestamp = timestamp;
        this.batteryEventType = batteryEventType;
        this.batteryLevel = batteryLevel;
    }

    /** Sets the auto-generated content ID. */
    public void setId(long id) {
        this.mId = id;
    }

    /** Gets the auto-generated content ID. */
    public long getId() {
        return mId;
    }

    @Override
    public String toString() {
        final String recordAtDateTime = ConvertUtils.utcToLocalTimeForLogging(timestamp);
        final StringBuilder builder =
                new StringBuilder()
                        .append("\nBatteryEvent{")
                        .append(
                                String.format(
                                        Locale.US,
                                        "\n\ttimestamp=%s|batteryEventType=%d|batteryLevel=%d",
                                        recordAtDateTime,
                                        batteryEventType,
                                        batteryLevel))
                        .append("\n}");
        return builder.toString();
    }

    /** Creates new {@link BatteryEventEntity} from {@link ContentValues}. */
    public static BatteryEventEntity create(ContentValues contentValues) {
        Builder builder = BatteryEventEntity.newBuilder();
        if (contentValues.containsKey(KEY_TIMESTAMP)) {
            builder.setTimestamp(contentValues.getAsLong(KEY_TIMESTAMP));
        }
        if (contentValues.containsKey(KEY_BATTERY_EVENT_TYPE)) {
            builder.setBatteryEventType(contentValues.getAsInteger(KEY_BATTERY_EVENT_TYPE));
        }
        if (contentValues.containsKey(KEY_BATTERY_LEVEL)) {
            builder.setBatteryLevel(contentValues.getAsInteger(KEY_BATTERY_LEVEL));
        }
        return builder.build();
    }

    /** Creates a new {@link Builder} instance. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /** A convenience builder class to improve readability. */
    public static class Builder {
        private long mTimestamp;
        private int mBatteryEventType;
        private int mBatteryLevel;

        /** Sets the timestamp. */
        @CanIgnoreReturnValue
        public Builder setTimestamp(final long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        /** Sets the battery event type. */
        @CanIgnoreReturnValue
        public Builder setBatteryEventType(final int batteryEventType) {
            mBatteryEventType = batteryEventType;
            return this;
        }

        /** Sets the battery level. */
        @CanIgnoreReturnValue
        public Builder setBatteryLevel(final int batteryLevel) {
            mBatteryLevel = batteryLevel;
            return this;
        }

        /** Builds the {@link BatteryEventEntity}. */
        public BatteryEventEntity build() {
            return new BatteryEventEntity(mTimestamp, mBatteryEventType, mBatteryLevel);
        }

        private Builder() {}
    }
}
