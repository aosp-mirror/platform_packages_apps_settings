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
package com.android.settings.fuelgauge.batteryusage;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.BatteryConsumer;
import android.util.Log;

import java.time.Duration;

/** A container class to carry data from {@link ContentValues}. */
public class BatteryHistEntry {
    private static final boolean DEBUG = false;
    private static final String TAG = "BatteryHistEntry";

    /** Keys for accessing {@link ContentValues} or {@link Cursor}. */
    public static final String KEY_UID = "uid";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_APP_LABEL = "appLabel";
    public static final String KEY_PACKAGE_NAME = "packageName";
    public static final String KEY_IS_HIDDEN = "isHidden";
    // Device booting elapsed time from SystemClock.elapsedRealtime().
    public static final String KEY_BOOT_TIMESTAMP = "bootTimestamp";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_ZONE_ID = "zoneId";
    public static final String KEY_TOTAL_POWER = "totalPower";
    public static final String KEY_CONSUME_POWER = "consumePower";
    public static final String KEY_PERCENT_OF_TOTAL = "percentOfTotal";
    public static final String KEY_FOREGROUND_USAGE_TIME = "foregroundUsageTimeInMs";
    public static final String KEY_BACKGROUND_USAGE_TIME = "backgroundUsageTimeInMs";
    public static final String KEY_DRAIN_TYPE = "drainType";
    public static final String KEY_CONSUMER_TYPE = "consumerType";
    public static final String KEY_BATTERY_LEVEL = "batteryLevel";
    public static final String KEY_BATTERY_STATUS = "batteryStatus";
    public static final String KEY_BATTERY_HEALTH = "batteryHealth";

    public final long mUid;
    public final long mUserId;
    public final String mAppLabel;
    public final String mPackageName;
    // Whether the data is represented as system component or not?
    public final boolean mIsHidden;
    // Records the timestamp relative information.
    public final long mBootTimestamp;
    public final long mTimestamp;
    public final String mZoneId;
    // Records the battery usage relative information.
    public final double mTotalPower;
    public final double mConsumePower;
    public final double mPercentOfTotal;
    public final long mForegroundUsageTimeInMs;
    public final long mBackgroundUsageTimeInMs;
    @BatteryConsumer.PowerComponent
    public final int mDrainType;
    @ConvertUtils.ConsumerType
    public final int mConsumerType;
    // Records the battery intent relative information.
    public final int mBatteryLevel;
    public final int mBatteryStatus;
    public final int mBatteryHealth;

    private String mKey = null;
    private boolean mIsValidEntry = true;

    public BatteryHistEntry(ContentValues values) {
        mUid = getLong(values, KEY_UID);
        mUserId = getLong(values, KEY_USER_ID);
        mAppLabel = getString(values, KEY_APP_LABEL);
        mPackageName = getString(values, KEY_PACKAGE_NAME);
        mIsHidden = getBoolean(values, KEY_IS_HIDDEN);
        mBootTimestamp = getLong(values, KEY_BOOT_TIMESTAMP);
        mTimestamp = getLong(values, KEY_TIMESTAMP);
        mZoneId = getString(values, KEY_ZONE_ID);
        mTotalPower = getDouble(values, KEY_TOTAL_POWER);
        mConsumePower = getDouble(values, KEY_CONSUME_POWER);
        mPercentOfTotal = getDouble(values, KEY_PERCENT_OF_TOTAL);
        mForegroundUsageTimeInMs = getLong(values, KEY_FOREGROUND_USAGE_TIME);
        mBackgroundUsageTimeInMs = getLong(values, KEY_BACKGROUND_USAGE_TIME);
        mDrainType = getInteger(values, KEY_DRAIN_TYPE);
        mConsumerType = getInteger(values, KEY_CONSUMER_TYPE);
        mBatteryLevel = getInteger(values, KEY_BATTERY_LEVEL);
        mBatteryStatus = getInteger(values, KEY_BATTERY_STATUS);
        mBatteryHealth = getInteger(values, KEY_BATTERY_HEALTH);
    }

    public BatteryHistEntry(Cursor cursor) {
        mUid = getLong(cursor, KEY_UID);
        mUserId = getLong(cursor, KEY_USER_ID);
        mAppLabel = getString(cursor, KEY_APP_LABEL);
        mPackageName = getString(cursor, KEY_PACKAGE_NAME);
        mIsHidden = getBoolean(cursor, KEY_IS_HIDDEN);
        mBootTimestamp = getLong(cursor, KEY_BOOT_TIMESTAMP);
        mTimestamp = getLong(cursor, KEY_TIMESTAMP);
        mZoneId = getString(cursor, KEY_ZONE_ID);
        mTotalPower = getDouble(cursor, KEY_TOTAL_POWER);
        mConsumePower = getDouble(cursor, KEY_CONSUME_POWER);
        mPercentOfTotal = getDouble(cursor, KEY_PERCENT_OF_TOTAL);
        mForegroundUsageTimeInMs = getLong(cursor, KEY_FOREGROUND_USAGE_TIME);
        mBackgroundUsageTimeInMs = getLong(cursor, KEY_BACKGROUND_USAGE_TIME);
        mDrainType = getInteger(cursor, KEY_DRAIN_TYPE);
        mConsumerType = getInteger(cursor, KEY_CONSUMER_TYPE);
        mBatteryLevel = getInteger(cursor, KEY_BATTERY_LEVEL);
        mBatteryStatus = getInteger(cursor, KEY_BATTERY_STATUS);
        mBatteryHealth = getInteger(cursor, KEY_BATTERY_HEALTH);
    }

    private BatteryHistEntry(
            BatteryHistEntry fromEntry,
            long bootTimestamp,
            long timestamp,
            double totalPower,
            double consumePower,
            long foregroundUsageTimeInMs,
            long backgroundUsageTimeInMs,
            int batteryLevel) {
        mUid = fromEntry.mUid;
        mUserId = fromEntry.mUserId;
        mAppLabel = fromEntry.mAppLabel;
        mPackageName = fromEntry.mPackageName;
        mIsHidden = fromEntry.mIsHidden;
        mBootTimestamp = bootTimestamp;
        mTimestamp = timestamp;
        mZoneId = fromEntry.mZoneId;
        mTotalPower = totalPower;
        mConsumePower = consumePower;
        mPercentOfTotal = fromEntry.mPercentOfTotal;
        mForegroundUsageTimeInMs = foregroundUsageTimeInMs;
        mBackgroundUsageTimeInMs = backgroundUsageTimeInMs;
        mDrainType = fromEntry.mDrainType;
        mConsumerType = fromEntry.mConsumerType;
        mBatteryLevel = batteryLevel;
        mBatteryStatus = fromEntry.mBatteryStatus;
        mBatteryHealth = fromEntry.mBatteryHealth;
    }

    /** Whether this {@link BatteryHistEntry} is valid or not? */
    public boolean isValidEntry() {
        return mIsValidEntry;
    }

    /** Whether this {@link BatteryHistEntry} is user consumer or not. */
    public boolean isUserEntry() {
        return mConsumerType == ConvertUtils.CONSUMER_TYPE_USER_BATTERY;
    }

    /** Whether this {@link BatteryHistEntry} is app consumer or not. */
    public boolean isAppEntry() {
        return mConsumerType == ConvertUtils.CONSUMER_TYPE_UID_BATTERY;
    }

    /** Whether this {@link BatteryHistEntry} is system consumer or not. */
    public boolean isSystemEntry() {
        return mConsumerType == ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY;
    }

    /** Gets an identifier to represent this {@link BatteryHistEntry}. */
    public String getKey() {
        if (mKey == null) {
            switch (mConsumerType) {
                case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
                    mKey = Long.toString(mUid);
                    break;
                case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                    mKey = "S|" + mDrainType;
                    break;
                case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
                    mKey = "U|" + mUserId;
                    break;
            }
        }
        return mKey;
    }

    @Override
    public String toString() {
        final String recordAtDateTime =
                ConvertUtils.utcToLocalTime(/*context=*/ null, mTimestamp);
        final StringBuilder builder = new StringBuilder()
                .append("\nBatteryHistEntry{")
                .append(String.format("\n\tpackage=%s|label=%s|uid=%d|userId=%d|isHidden=%b",
                        mPackageName, mAppLabel, mUid, mUserId, mIsHidden))
                .append(String.format("\n\ttimestamp=%s|zoneId=%s|bootTimestamp=%d",
                        recordAtDateTime, mZoneId, Duration.ofMillis(mBootTimestamp).getSeconds()))
                .append(String.format("\n\tusage=%f|total=%f|consume=%f|elapsedTime=%d|%d",
                        mPercentOfTotal, mTotalPower, mConsumePower,
                        Duration.ofMillis(mForegroundUsageTimeInMs).getSeconds(),
                        Duration.ofMillis(mBackgroundUsageTimeInMs).getSeconds()))
                .append(String.format("\n\tdrainType=%d|consumerType=%d",
                        mDrainType, mConsumerType))
                .append(String.format("\n\tbattery=%d|status=%d|health=%d\n}",
                        mBatteryLevel, mBatteryStatus, mBatteryHealth));
        return builder.toString();
    }

    private int getInteger(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsInteger(key);
        }
        mIsValidEntry = false;
        return 0;
    }

    private int getInteger(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getInt(columnIndex);
        }
        mIsValidEntry = false;
        return 0;
    }

    private long getLong(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsLong(key);
        }
        mIsValidEntry = false;
        return 0L;
    }

    private long getLong(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getLong(columnIndex);
        }
        mIsValidEntry = false;
        return 0L;
    }

    private double getDouble(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsDouble(key);
        }
        mIsValidEntry = false;
        return 0f;
    }

    private double getDouble(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getDouble(columnIndex);
        }
        mIsValidEntry = false;
        return 0f;
    }

    private String getString(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsString(key);
        }
        mIsValidEntry = false;
        return null;
    }

    private String getString(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            return cursor.getString(columnIndex);
        }
        mIsValidEntry = false;
        return null;
    }

    private boolean getBoolean(ContentValues values, String key) {
        if (values != null && values.containsKey(key)) {
            return values.getAsBoolean(key);
        }
        mIsValidEntry = false;
        return false;
    }

    private boolean getBoolean(Cursor cursor, String key) {
        final int columnIndex = cursor.getColumnIndex(key);
        if (columnIndex >= 0) {
            // Use value == 1 to represent boolean value in the database.
            return cursor.getInt(columnIndex) == 1;
        }
        mIsValidEntry = false;
        return false;
    }

    /** Creates new {@link BatteryHistEntry} from interpolation. */
    public static BatteryHistEntry interpolate(
            long slotTimestamp,
            long upperTimestamp,
            double ratio,
            BatteryHistEntry lowerHistEntry,
            BatteryHistEntry upperHistEntry) {
        final double totalPower = interpolate(
                lowerHistEntry == null ? 0 : lowerHistEntry.mTotalPower,
                upperHistEntry.mTotalPower,
                ratio);
        final double consumePower = interpolate(
                lowerHistEntry == null ? 0 : lowerHistEntry.mConsumePower,
                upperHistEntry.mConsumePower,
                ratio);
        final double foregroundUsageTimeInMs = interpolate(
                lowerHistEntry == null ? 0 : lowerHistEntry.mForegroundUsageTimeInMs,
                upperHistEntry.mForegroundUsageTimeInMs,
                ratio);
        final double backgroundUsageTimeInMs = interpolate(
                lowerHistEntry == null ? 0 : lowerHistEntry.mBackgroundUsageTimeInMs,
                upperHistEntry.mBackgroundUsageTimeInMs,
                ratio);
        // Checks whether there is any abnoaml cases!
        if (upperHistEntry.mConsumePower < consumePower
                || upperHistEntry.mForegroundUsageTimeInMs < foregroundUsageTimeInMs
                || upperHistEntry.mBackgroundUsageTimeInMs < backgroundUsageTimeInMs) {
            if (DEBUG) {
                Log.w(TAG, String.format(
                        "abnormal interpolation:\nupper:%s\nlower:%s",
                        upperHistEntry, lowerHistEntry));
            }
        }
        final double batteryLevel =
                lowerHistEntry == null
                        ? upperHistEntry.mBatteryLevel
                        : interpolate(
                                lowerHistEntry.mBatteryLevel,
                                upperHistEntry.mBatteryLevel,
                                ratio);
        return new BatteryHistEntry(
                upperHistEntry,
                /*bootTimestamp=*/ upperHistEntry.mBootTimestamp
                - (upperTimestamp - slotTimestamp),
                /*timestamp=*/ slotTimestamp,
                totalPower,
                consumePower,
                Math.round(foregroundUsageTimeInMs),
                Math.round(backgroundUsageTimeInMs),
                (int) Math.round(batteryLevel));
    }

    private static double interpolate(double v1, double v2, double ratio) {
        return v1 + ratio * (v2 - v1);
    }
}
