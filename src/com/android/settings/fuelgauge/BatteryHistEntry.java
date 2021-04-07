/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.content.ContentValues;

/** A container class to carry data from {@link ContentValues}. */
public final class BatteryHistEntry {
    private static final String TAG = "BatteryHistEntry";

    public final long mUid;
    public final long mUserId;
    public final String mAppLabel;
    public final String mPackageName;
    // Whether the data is represented as system component or not?
    public final boolean mIsHidden;
    // Records the timestamp relative information.
    public final long mTimestamp;
    public final String mZoneId;
    // Records the battery usage relative information.
    public final double mTotalPower;
    public final double mConsumePower;
    public final double mPercentOfTotal;
    public final long mForegroundUsageTimeInMs;
    public final long mBackgroundUsageTimeInMs;
    public final int mDrainType;
    public final int mConsumerType;
    // Records the battery intent relative information.
    public final int mBatteryLevel;
    public final int mBatteryStatus;
    public final int mBatteryHealth;

    private boolean mIsValidEntry = true;
    private ContentValues mContentValues;

    public BatteryHistEntry(ContentValues contentValues) {
        mContentValues = contentValues;
        mUid = getLong("uid");
        mUserId = getLong("userId");
        mAppLabel = getString("appLabel");
        mPackageName = getString("packageName");
        mIsHidden = getBoolean("isHidden");
        mTimestamp = getLong("timestamp");
        mZoneId = getString("zoneId");
        mTotalPower = getDouble("totalPower");
        mConsumePower = getDouble("consumePower");
        mPercentOfTotal = getDouble("percentOfTotal");
        mForegroundUsageTimeInMs = getLong("foregroundUsageTimeInMs");
        mBackgroundUsageTimeInMs = getLong("backgroundUsageTimeInMs");
        mDrainType = getInteger("drainType");
        mConsumerType = getInteger("consumerType");
        mBatteryLevel = getInteger("batteryLevel");
        mBatteryStatus = getInteger("batteryStatus");
        mBatteryHealth = getInteger("batteryHealth");
    }

    /** Whether this {@link BatteryHistEntry} is valid or not? */
    public boolean isValidEntry() {
        return mIsValidEntry;
    }

    private int getInteger(String key) {
        if (mContentValues != null && mContentValues.containsKey(key)) {
            return mContentValues.getAsInteger(key);
        };
        mIsValidEntry = false;
        return -1;
    }

    private long getLong(String key) {
        if (mContentValues != null && mContentValues.containsKey(key)) {
            return mContentValues.getAsLong(key);
        }
        mIsValidEntry = false;
        return -1L;
    }

    private double getDouble(String key) {
        if (mContentValues != null && mContentValues.containsKey(key)) {
            return mContentValues.getAsDouble(key);
        }
        mIsValidEntry = false;
        return 0f;
    }

    private String getString(String key) {
        if (mContentValues != null && mContentValues.containsKey(key)) {
            return mContentValues.getAsString(key);
        }
        mIsValidEntry = false;
        return null;
    }

    private boolean getBoolean(String key) {
        if (mContentValues != null && mContentValues.containsKey(key)) {
            return mContentValues.getAsBoolean(key);
        }
        mIsValidEntry = false;
        return false;
    }
}
