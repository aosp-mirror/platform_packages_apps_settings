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

import android.content.ContentValues;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.BatteryInformation;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Locale;

/** A {@link Entity} class to save battery states snapshot into database. */
@Entity
public class BatteryState {
    @PrimaryKey(autoGenerate = true)
    private long mId;

    // Records the app relative information.
    public final long uid;
    public final long userId;
    public final String packageName;
    public final long timestamp;
    public final int consumerType;
    public final boolean isFullChargeCycleStart;
    public final String batteryInformation;

    /**
     * This field is filled only when build type is "userdebug".
     *
     * <p>For now, Java Proto Lite is recommended by the Android team as the more lightweight
     * solution designed specifically for mobile apps to process protobuf. However, converting
     * protobuf to string through Java Proto Lite needs to parse it into a bytes field first, which
     * leads to the strings saved in our database are encoded and hard to understand.
     *
     * <p>To make it easier to debug in our daily development, this field is added. It will not be
     * filled for the real users.
     */
    public final String batteryInformationDebug;

    public BatteryState(
            long uid,
            long userId,
            String packageName,
            long timestamp,
            int consumerType,
            boolean isFullChargeCycleStart,
            String batteryInformation,
            String batteryInformationDebug) {
        // Records the app relative information.
        this.uid = uid;
        this.userId = userId;
        this.packageName = packageName;
        this.timestamp = timestamp;
        this.consumerType = consumerType;
        this.isFullChargeCycleStart = isFullChargeCycleStart;
        this.batteryInformation = batteryInformation;
        this.batteryInformationDebug = batteryInformationDebug;
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
        final BatteryInformation batteryInformationInstance =
                BatteryUtils.parseProtoFromString(
                        batteryInformation, BatteryInformation.getDefaultInstance());
        final StringBuilder builder =
                new StringBuilder()
                        .append("\nBatteryState{")
                        .append(
                                String.format(
                                        Locale.US,
                                        "\n\tpackage=%s|uid=%d|userId=%d",
                                        packageName,
                                        uid,
                                        userId))
                        .append(
                                String.format(
                                        Locale.US,
                                        "\n\ttimestamp=%s|consumer=%d|isStart=%b",
                                        recordAtDateTime,
                                        consumerType,
                                        isFullChargeCycleStart))
                        .append(String.format(Locale.US, "\n\tbatteryInfo="))
                        .append(batteryInformationInstance.toString());
        return builder.toString();
    }

    /** Creates new {@link BatteryState} from {@link ContentValues}. */
    public static BatteryState create(ContentValues contentValues) {
        Builder builder = BatteryState.newBuilder();
        if (contentValues.containsKey("uid")) {
            builder.setUid(contentValues.getAsLong("uid"));
        }
        if (contentValues.containsKey("userId")) {
            builder.setUserId(contentValues.getAsLong("userId"));
        }
        if (contentValues.containsKey("packageName")) {
            builder.setPackageName(contentValues.getAsString("packageName"));
        }
        if (contentValues.containsKey("timestamp")) {
            builder.setTimestamp(contentValues.getAsLong("timestamp"));
        }
        if (contentValues.containsKey("consumerType")) {
            builder.setConsumerType(contentValues.getAsInteger("consumerType"));
        }
        if (contentValues.containsKey("isFullChargeCycleStart")) {
            builder.setIsFullChargeCycleStart(contentValues.getAsBoolean("isFullChargeCycleStart"));
        }
        if (contentValues.containsKey("batteryInformation")) {
            builder.setBatteryInformation(contentValues.getAsString("batteryInformation"));
        }
        if (contentValues.containsKey("batteryInformationDebug")) {
            builder.setBatteryInformationDebug(
                    contentValues.getAsString("batteryInformationDebug"));
        }
        return builder.build();
    }

    /** Creates a new {@link Builder} instance. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /** A convenience builder class to improve readability. */
    public static class Builder {
        private long mUid;
        private long mUserId;
        private String mPackageName;
        private long mTimestamp;
        private int mConsumerType;
        private boolean mIsFullChargeCycleStart;
        private String mBatteryInformation;
        private String mBatteryInformationDebug;

        /** Sets the uid. */
        @CanIgnoreReturnValue
        public Builder setUid(long uid) {
            this.mUid = uid;
            return this;
        }

        /** Sets the user ID. */
        @CanIgnoreReturnValue
        public Builder setUserId(long userId) {
            this.mUserId = userId;
            return this;
        }

        /** Sets the package name. */
        @CanIgnoreReturnValue
        public Builder setPackageName(String packageName) {
            this.mPackageName = packageName;
            return this;
        }

        /** Sets the timestamp. */
        @CanIgnoreReturnValue
        public Builder setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
            return this;
        }

        /** Sets the consumer type. */
        @CanIgnoreReturnValue
        public Builder setConsumerType(int consumerType) {
            this.mConsumerType = consumerType;
            return this;
        }

        /** Sets whether is the full charge cycle start. */
        @CanIgnoreReturnValue
        public Builder setIsFullChargeCycleStart(boolean isFullChargeCycleStart) {
            this.mIsFullChargeCycleStart = isFullChargeCycleStart;
            return this;
        }

        /** Sets the battery information. */
        @CanIgnoreReturnValue
        public Builder setBatteryInformation(String batteryInformation) {
            this.mBatteryInformation = batteryInformation;
            return this;
        }

        /** Sets the battery information debug string. */
        @CanIgnoreReturnValue
        public Builder setBatteryInformationDebug(String batteryInformationDebug) {
            this.mBatteryInformationDebug = batteryInformationDebug;
            return this;
        }

        /** Builds the BatteryState. */
        public BatteryState build() {
            return new BatteryState(
                    mUid,
                    mUserId,
                    mPackageName,
                    mTimestamp,
                    mConsumerType,
                    mIsFullChargeCycleStart,
                    mBatteryInformation,
                    mBatteryInformationDebug);
        }

        private Builder() {}
    }
}
