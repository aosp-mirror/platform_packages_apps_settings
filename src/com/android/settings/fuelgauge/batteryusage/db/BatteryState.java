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
import android.content.Intent;
import android.os.BatteryManager;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** A {@link Entity} class to save battery states snapshot into database. */
@Entity
public class BatteryState {
    private static String sCacheZoneId;
    private static SimpleDateFormat sCacheSimpleDateFormat;

    @PrimaryKey(autoGenerate = true)
    private long mId;

    // Records the app relative information.
    public final long uid;
    public final long userId;
    public final String appLabel;
    public final String packageName;
    // Whether the data is represented as system component or not?
    public final boolean isHidden;
    // Records the timestamp relative information.
    public final long bootTimestamp;
    public final long timestamp;
    public final String zoneId;
    // Records the battery usage relative information.
    public final double totalPower;
    public final double consumePower;
    public final double percentOfTotal;
    public final long foregroundUsageTimeInMs;
    public final long backgroundUsageTimeInMs;
    public final int drainType;
    public final int consumerType;
    // Records the battery intent relative information.
    public final int batteryLevel;
    public final int batteryStatus;
    public final int batteryHealth;

    public BatteryState(
            long uid,
            long userId,
            String appLabel,
            String packageName,
            boolean isHidden,
            long bootTimestamp,
            long timestamp,
            String zoneId,
            double totalPower,
            double consumePower,
            double percentOfTotal,
            long foregroundUsageTimeInMs,
            long backgroundUsageTimeInMs,
            int drainType,
            int consumerType,
            int batteryLevel,
            int batteryStatus,
            int batteryHealth) {
        // Records the app relative information.
        this.uid = uid;
        this.userId = userId;
        this.appLabel = appLabel;
        this.packageName = packageName;
        this.isHidden = isHidden;
        // Records the timestamp relative information.
        this.bootTimestamp = bootTimestamp;
        this.timestamp = timestamp;
        this.zoneId = zoneId;
        // Records the battery usage relative information.
        this.totalPower = totalPower;
        this.consumePower = consumePower;
        this.percentOfTotal = percentOfTotal;
        this.foregroundUsageTimeInMs = foregroundUsageTimeInMs;
        this.backgroundUsageTimeInMs = backgroundUsageTimeInMs;
        this.drainType = drainType;
        this.consumerType = consumerType;
        // Records the battery intent relative information.
        this.batteryLevel = batteryLevel;
        this.batteryStatus = batteryStatus;
        this.batteryHealth = batteryHealth;
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
    @SuppressWarnings("JavaUtilDate")
    public String toString() {
        final String currentZoneId = TimeZone.getDefault().getID();
        if (!currentZoneId.equals(sCacheZoneId) || sCacheSimpleDateFormat == null) {
            sCacheZoneId = currentZoneId;
            sCacheSimpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss", Locale.US);
        }
        final String recordAtDateTime = sCacheSimpleDateFormat.format(new Date(timestamp));
        final StringBuilder builder = new StringBuilder()
                .append("\nBatteryState{")
                .append(String.format(Locale.US,
                        "\n\tpackage=%s|label=%s|uid=%d|userId=%d|isHidden=%b",
                        packageName, appLabel, uid, userId, isHidden))
                .append(String.format(Locale.US, "\n\ttimestamp=%s|zoneId=%s|bootTimestamp=%d",
                        recordAtDateTime, zoneId, Duration.ofMillis(bootTimestamp).getSeconds()))
                .append(String.format(Locale.US,
                        "\n\tusage=%f|total=%f|consume=%f|elapsedTime=%d|%d",
                        percentOfTotal, totalPower, consumePower,
                        Duration.ofMillis(foregroundUsageTimeInMs).getSeconds(),
                        Duration.ofMillis(backgroundUsageTimeInMs).getSeconds()))
                .append(String.format(Locale.US,
                        "\n\tdrain=%d|consumer=%d", drainType, consumerType))
                .append(String.format(Locale.US, "\n\tbattery=%d|status=%d|health=%d\n}",
                        batteryLevel, batteryStatus, batteryHealth));
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
        if (contentValues.containsKey("appLabel")) {
            builder.setAppLabel(contentValues.getAsString("appLabel"));
        }
        if (contentValues.containsKey("packageName")) {
            builder.setPackageName(contentValues.getAsString("packageName"));
        }
        if (contentValues.containsKey("isHidden")) {
            builder.setIsHidden(contentValues.getAsBoolean("isHidden"));
        }
        if (contentValues.containsKey("bootTimestamp")) {
            builder.setBootTimestamp(contentValues.getAsLong("bootTimestamp"));
        }
        if (contentValues.containsKey("timestamp")) {
            builder.setTimestamp(contentValues.getAsLong("timestamp"));
        }
        if (contentValues.containsKey("consumePower")) {
            builder.setConsumePower(contentValues.getAsDouble("consumePower"));
        }
        if (contentValues.containsKey("totalPower")) {
            builder.setTotalPower(contentValues.getAsDouble("totalPower"));
        }
        if (contentValues.containsKey("percentOfTotal")) {
            builder.setPercentOfTotal(contentValues.getAsDouble("percentOfTotal"));
        }
        if (contentValues.containsKey("foregroundUsageTimeInMs")) {
            builder.setForegroundUsageTimeInMs(
                    contentValues.getAsLong("foregroundUsageTimeInMs"));
        }
        if (contentValues.containsKey("backgroundUsageTimeInMs")) {
            builder.setBackgroundUsageTimeInMs(
                    contentValues.getAsLong("backgroundUsageTimeInMs"));
        }
        if (contentValues.containsKey("drainType")) {
            builder.setDrainType(contentValues.getAsInteger("drainType"));
        }
        if (contentValues.containsKey("consumerType")) {
            builder.setConsumerType(contentValues.getAsInteger("consumerType"));
        }
        if (contentValues.containsKey("batteryLevel")) {
            builder.setBatteryLevel(contentValues.getAsInteger("batteryLevel"));
        }
        if (contentValues.containsKey("batteryStatus")) {
            builder.setBatteryStatus(contentValues.getAsInteger("batteryStatus"));
        }
        if (contentValues.containsKey("batteryHealth")) {
            builder.setBatteryHealth(contentValues.getAsInteger("batteryHealth"));
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
        private String mAppLabel;
        private String mPackageName;
        private boolean mIsHidden;
        private long mBootTimestamp;
        private long mTimestamp;
        private double mTotalPower;
        private double mConsumePower;
        private double mPercentOfTotal;
        private long mForegroundUsageTimeInMs;
        private long mBackgroundUsageTimeInMs;
        private int mDrainType;
        private int mConsumerType;
        private int mBatteryLevel;
        private int mBatteryStatus;
        private int mBatteryHealth;

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

        /** Sets the app label. */
        @CanIgnoreReturnValue
        public Builder setAppLabel(String appLabel) {
            this.mAppLabel = appLabel;
            return this;
        }

        /** Sets the package name. */
        @CanIgnoreReturnValue
        public Builder setPackageName(String packageName) {
            this.mPackageName = packageName;
            return this;
        }

        /** Sets the is hidden value. */
        @CanIgnoreReturnValue
        public Builder setIsHidden(boolean isHidden) {
            this.mIsHidden = isHidden;
            return this;
        }

        /** Sets the boot timestamp. */
        @CanIgnoreReturnValue
        public Builder setBootTimestamp(long bootTimestamp) {
            this.mBootTimestamp = bootTimestamp;
            return this;
        }

        /** Sets the timestamp. */
        @CanIgnoreReturnValue
        public Builder setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
            return this;
        }

        /** Sets the total power. */
        @CanIgnoreReturnValue
        public Builder setTotalPower(double totalPower) {
            this.mTotalPower = totalPower;
            return this;
        }

        /** Sets the consumed power. */
        @CanIgnoreReturnValue
        public Builder setConsumePower(double consumePower) {
            this.mConsumePower = consumePower;
            return this;
        }

        /** Sets the percentage of total. */
        @CanIgnoreReturnValue
        public Builder setPercentOfTotal(double percentOfTotal) {
            this.mPercentOfTotal = percentOfTotal;
            return this;
        }

        /** Sets the foreground usage time. */
        @CanIgnoreReturnValue
        public Builder setForegroundUsageTimeInMs(long foregroundUsageTimeInMs) {
            this.mForegroundUsageTimeInMs = foregroundUsageTimeInMs;
            return this;
        }

        /** Sets the background usage time. */
        @CanIgnoreReturnValue
        public Builder setBackgroundUsageTimeInMs(long backgroundUsageTimeInMs) {
            this.mBackgroundUsageTimeInMs = backgroundUsageTimeInMs;
            return this;
        }

        /** Sets the drain type. */
        @CanIgnoreReturnValue
        public Builder setDrainType(int drainType) {
            this.mDrainType = drainType;
            return this;
        }

        /** Sets the consumer type. */
        @CanIgnoreReturnValue
        public Builder setConsumerType(int consumerType) {
            this.mConsumerType = consumerType;
            return this;
        }

        /** Sets the battery level. */
        @CanIgnoreReturnValue
        public Builder setBatteryLevel(int batteryLevel) {
            this.mBatteryLevel = batteryLevel;
            return this;
        }

        /** Sets the battery status. */
        @CanIgnoreReturnValue
        public Builder setBatteryStatus(int batteryStatus) {
            this.mBatteryStatus = batteryStatus;
            return this;
        }

        /** Sets the battery health. */
        @CanIgnoreReturnValue
        public Builder setBatteryHealth(int batteryHealth) {
            this.mBatteryHealth = batteryHealth;
            return this;
        }

        /** Sets the battery intent. */
        @CanIgnoreReturnValue
        public Builder setBatteryIntent(Intent batteryIntent) {
            final int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            final int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            this.mBatteryLevel =
                    scale == 0
                            ? -1 /*invalid battery level*/
                            : Math.round((level / (float) scale) * 100f);
            this.mBatteryStatus =
                    batteryIntent.getIntExtra(
                            BatteryManager.EXTRA_STATUS,
                            BatteryManager.BATTERY_STATUS_UNKNOWN);
            this.mBatteryHealth =
                    batteryIntent.getIntExtra(
                            BatteryManager.EXTRA_HEALTH,
                            BatteryManager.BATTERY_HEALTH_UNKNOWN);
            return this;
        }

        /** Builds the BatteryState. */
        public BatteryState build() {
            return new BatteryState(
                    mUid,
                    mUserId,
                    mAppLabel,
                    mPackageName,
                    mIsHidden,
                    mBootTimestamp,
                    mTimestamp,
                    /*zoneId=*/ TimeZone.getDefault().getID(),
                    mTotalPower,
                    mConsumePower,
                    mPercentOfTotal,
                    mForegroundUsageTimeInMs,
                    mBackgroundUsageTimeInMs,
                    mDrainType,
                    mConsumerType,
                    mBatteryLevel,
                    mBatteryStatus,
                    mBatteryHealth);
        }

        private Builder() {}
    }
}
