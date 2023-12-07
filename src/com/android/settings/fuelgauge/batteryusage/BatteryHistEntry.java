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

/** A container class to carry data from {@link ContentValues}. */
public class BatteryHistEntry {
    private static final boolean DEBUG = false;
    private static final String TAG = "BatteryHistEntry";

    /** Keys for accessing {@link ContentValues} or {@link Cursor}. */
    public static final String KEY_UID = "uid";

    public static final String KEY_USER_ID = "userId";
    public static final String KEY_PACKAGE_NAME = "packageName";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_CONSUMER_TYPE = "consumerType";
    public static final String KEY_IS_FULL_CHARGE_CYCLE_START = "isFullChargeCycleStart";
    public static final String KEY_BATTERY_INFORMATION = "batteryInformation";
    public static final String KEY_BATTERY_INFORMATION_DEBUG = "batteryInformationDebug";

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
    public final double mForegroundUsageConsumePower;
    public final double mForegroundServiceUsageConsumePower;
    public final double mBackgroundUsageConsumePower;
    public final double mCachedUsageConsumePower;
    public final double mPercentOfTotal;
    public final long mForegroundUsageTimeInMs;
    public final long mForegroundServiceUsageTimeInMs;
    public final long mBackgroundUsageTimeInMs;
    @BatteryConsumer.PowerComponent public final int mDrainType;
    @ConvertUtils.ConsumerType public final int mConsumerType;
    // Records the battery intent relative information.
    public final int mBatteryLevel;
    public final int mBatteryStatus;
    public final int mBatteryHealth;

    private String mKey = null;
    private boolean mIsValidEntry = true;

    public BatteryHistEntry(ContentValues values) {
        mUid = getLong(values, KEY_UID);
        mUserId = getLong(values, KEY_USER_ID);
        mPackageName = getString(values, KEY_PACKAGE_NAME);
        mTimestamp = getLong(values, KEY_TIMESTAMP);
        mConsumerType = getInteger(values, KEY_CONSUMER_TYPE);
        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(values, KEY_BATTERY_INFORMATION);
        mAppLabel = batteryInformation.getAppLabel();
        mIsHidden = batteryInformation.getIsHidden();
        mBootTimestamp = batteryInformation.getBootTimestamp();
        mZoneId = batteryInformation.getZoneId();
        mTotalPower = batteryInformation.getTotalPower();
        mConsumePower = batteryInformation.getConsumePower();
        mForegroundUsageConsumePower = batteryInformation.getForegroundUsageConsumePower();
        mForegroundServiceUsageConsumePower =
                batteryInformation.getForegroundServiceUsageConsumePower();
        mBackgroundUsageConsumePower = batteryInformation.getBackgroundUsageConsumePower();
        mCachedUsageConsumePower = batteryInformation.getCachedUsageConsumePower();
        mPercentOfTotal = batteryInformation.getPercentOfTotal();
        mForegroundUsageTimeInMs = batteryInformation.getForegroundUsageTimeInMs();
        mForegroundServiceUsageTimeInMs = batteryInformation.getForegroundServiceUsageTimeInMs();
        mBackgroundUsageTimeInMs = batteryInformation.getBackgroundUsageTimeInMs();
        mDrainType = batteryInformation.getDrainType();
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();
        mBatteryLevel = deviceBatteryState.getBatteryLevel();
        mBatteryStatus = deviceBatteryState.getBatteryStatus();
        mBatteryHealth = deviceBatteryState.getBatteryHealth();
    }

    public BatteryHistEntry(Cursor cursor) {
        mUid = getLong(cursor, KEY_UID);
        mUserId = getLong(cursor, KEY_USER_ID);
        mPackageName = getString(cursor, KEY_PACKAGE_NAME);
        mTimestamp = getLong(cursor, KEY_TIMESTAMP);
        mConsumerType = getInteger(cursor, KEY_CONSUMER_TYPE);
        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(cursor, KEY_BATTERY_INFORMATION);
        mAppLabel = batteryInformation.getAppLabel();
        mIsHidden = batteryInformation.getIsHidden();
        mBootTimestamp = batteryInformation.getBootTimestamp();
        mZoneId = batteryInformation.getZoneId();
        mTotalPower = batteryInformation.getTotalPower();
        mConsumePower = batteryInformation.getConsumePower();
        mForegroundUsageConsumePower = batteryInformation.getForegroundUsageConsumePower();
        mForegroundServiceUsageConsumePower =
                batteryInformation.getForegroundServiceUsageConsumePower();
        mBackgroundUsageConsumePower = batteryInformation.getBackgroundUsageConsumePower();
        mCachedUsageConsumePower = batteryInformation.getCachedUsageConsumePower();
        mPercentOfTotal = batteryInformation.getPercentOfTotal();
        mForegroundUsageTimeInMs = batteryInformation.getForegroundUsageTimeInMs();
        mForegroundServiceUsageTimeInMs = batteryInformation.getForegroundServiceUsageTimeInMs();
        mBackgroundUsageTimeInMs = batteryInformation.getBackgroundUsageTimeInMs();
        mDrainType = batteryInformation.getDrainType();
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();
        mBatteryLevel = deviceBatteryState.getBatteryLevel();
        mBatteryStatus = deviceBatteryState.getBatteryStatus();
        mBatteryHealth = deviceBatteryState.getBatteryHealth();
    }

    private BatteryHistEntry(
            BatteryHistEntry fromEntry,
            long bootTimestamp,
            long timestamp,
            double totalPower,
            double consumePower,
            double foregroundUsageConsumePower,
            double foregroundServiceUsageConsumePower,
            double backgroundUsageConsumePower,
            double cachedUsageConsumePower,
            long foregroundUsageTimeInMs,
            long foregroundServiceUsageTimeInMs,
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
        mForegroundUsageConsumePower = foregroundUsageConsumePower;
        mForegroundServiceUsageConsumePower = foregroundServiceUsageConsumePower;
        mBackgroundUsageConsumePower = backgroundUsageConsumePower;
        mCachedUsageConsumePower = cachedUsageConsumePower;
        mPercentOfTotal = fromEntry.mPercentOfTotal;
        mForegroundUsageTimeInMs = foregroundUsageTimeInMs;
        mForegroundServiceUsageTimeInMs = foregroundServiceUsageTimeInMs;
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
        final String recordAtDateTime = ConvertUtils.utcToLocalTimeForLogging(mTimestamp);
        final StringBuilder builder = new StringBuilder();
        builder.append("\nBatteryHistEntry{");
        builder.append(
                String.format(
                        "\n\tpackage=%s|label=%s|uid=%d|userId=%d|isHidden=%b",
                        mPackageName, mAppLabel, mUid, mUserId, mIsHidden));
        builder.append(
                String.format(
                        "\n\ttimestamp=%s|zoneId=%s|bootTimestamp=%d",
                        recordAtDateTime, mZoneId, TimestampUtils.getSeconds(mBootTimestamp)));
        builder.append(
                String.format(
                        "\n\tusage=%f|total=%f|consume=%f",
                        mPercentOfTotal, mTotalPower, mConsumePower));
        builder.append(
                String.format(
                        "\n\tforeground=%f|foregroundService=%f",
                        mForegroundUsageConsumePower, mForegroundServiceUsageConsumePower));
        builder.append(
                String.format(
                        "\n\tbackground=%f|cached=%f",
                        mBackgroundUsageConsumePower, mCachedUsageConsumePower));
        builder.append(
                String.format(
                        "\n\telapsedTime,fg=%d|fgs=%d|bg=%d",
                        TimestampUtils.getSeconds(mBackgroundUsageTimeInMs),
                        TimestampUtils.getSeconds(mForegroundServiceUsageTimeInMs),
                        TimestampUtils.getSeconds(mBackgroundUsageTimeInMs)));
        builder.append(
                String.format("\n\tdrainType=%d|consumerType=%d", mDrainType, mConsumerType));
        builder.append(
                String.format(
                        "\n\tbattery=%d|status=%d|health=%d\n}",
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

    /** Creates new {@link BatteryHistEntry} from interpolation. */
    public static BatteryHistEntry interpolate(
            long slotTimestamp,
            long upperTimestamp,
            double ratio,
            BatteryHistEntry lowerHistEntry,
            BatteryHistEntry upperHistEntry) {
        final double totalPower =
                interpolate(
                        lowerHistEntry == null ? 0 : lowerHistEntry.mTotalPower,
                        upperHistEntry.mTotalPower,
                        ratio);
        final double consumePower =
                interpolate(
                        lowerHistEntry == null ? 0 : lowerHistEntry.mConsumePower,
                        upperHistEntry.mConsumePower,
                        ratio);
        final double foregroundUsageConsumePower =
                interpolate(
                        lowerHistEntry == null ? 0 : lowerHistEntry.mForegroundUsageConsumePower,
                        upperHistEntry.mForegroundUsageConsumePower,
                        ratio);
        final double foregroundServiceUsageConsumePower =
                interpolate(
                        lowerHistEntry == null
                                ? 0
                                : lowerHistEntry.mForegroundServiceUsageConsumePower,
                        upperHistEntry.mForegroundServiceUsageConsumePower,
                        ratio);
        final double backgroundUsageConsumePower =
                interpolate(
                        lowerHistEntry == null ? 0 : lowerHistEntry.mBackgroundUsageConsumePower,
                        upperHistEntry.mBackgroundUsageConsumePower,
                        ratio);
        final double cachedUsageConsumePower =
                interpolate(
                        lowerHistEntry == null ? 0 : lowerHistEntry.mCachedUsageConsumePower,
                        upperHistEntry.mCachedUsageConsumePower,
                        ratio);
        final double foregroundUsageTimeInMs =
                interpolate(
                        (lowerHistEntry == null ? 0 : lowerHistEntry.mForegroundUsageTimeInMs),
                        upperHistEntry.mForegroundUsageTimeInMs,
                        ratio);
        final double foregroundServiceUsageTimeInMs =
                interpolate(
                        (lowerHistEntry == null
                                ? 0
                                : lowerHistEntry.mForegroundServiceUsageTimeInMs),
                        upperHistEntry.mForegroundServiceUsageTimeInMs,
                        ratio);
        final double backgroundUsageTimeInMs =
                interpolate(
                        (lowerHistEntry == null ? 0 : lowerHistEntry.mBackgroundUsageTimeInMs),
                        upperHistEntry.mBackgroundUsageTimeInMs,
                        ratio);
        // Checks whether there is any abnormal cases!
        if (upperHistEntry.mConsumePower < consumePower
                || upperHistEntry.mForegroundUsageConsumePower < foregroundUsageConsumePower
                || upperHistEntry.mForegroundServiceUsageConsumePower
                        < foregroundServiceUsageConsumePower
                || upperHistEntry.mBackgroundUsageConsumePower < backgroundUsageConsumePower
                || upperHistEntry.mCachedUsageConsumePower < cachedUsageConsumePower
                || upperHistEntry.mForegroundUsageTimeInMs < foregroundUsageTimeInMs
                || upperHistEntry.mForegroundServiceUsageTimeInMs < foregroundServiceUsageTimeInMs
                || upperHistEntry.mBackgroundUsageTimeInMs < backgroundUsageTimeInMs) {
            if (DEBUG) {
                Log.w(
                        TAG,
                        String.format(
                                "abnormal interpolation:\nupper:%s\nlower:%s",
                                upperHistEntry, lowerHistEntry));
            }
        }
        final double batteryLevel =
                lowerHistEntry == null
                        ? upperHistEntry.mBatteryLevel
                        : interpolate(
                                lowerHistEntry.mBatteryLevel, upperHistEntry.mBatteryLevel, ratio);
        return new BatteryHistEntry(
                upperHistEntry,
                /* bootTimestamp= */ upperHistEntry.mBootTimestamp
                        - (upperTimestamp - slotTimestamp),
                /* timestamp= */ slotTimestamp,
                totalPower,
                consumePower,
                foregroundUsageConsumePower,
                foregroundServiceUsageConsumePower,
                backgroundUsageConsumePower,
                cachedUsageConsumePower,
                Math.round(foregroundUsageTimeInMs),
                Math.round(foregroundServiceUsageTimeInMs),
                Math.round(backgroundUsageTimeInMs),
                (int) Math.round(batteryLevel));
    }

    private static double interpolate(double v1, double v2, double ratio) {
        return v1 + ratio * (v2 - v1);
    }
}
