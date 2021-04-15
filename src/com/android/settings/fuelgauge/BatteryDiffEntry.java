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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.time.Duration;
import java.util.Comparator;

/** A container class to carry battery data in a specific time slot. */
public final class BatteryDiffEntry {
    private static final String TAG = "BatteryDiffEntry";

    /** A comparator for {@link BatteryDiffEntry} based on consumed percentage. */
    public static final Comparator<BatteryDiffEntry> COMPARATOR =
            (a, b) -> Double.compare(b.getPercentOfTotal(), a.getPercentOfTotal());

    public long mForegroundUsageTimeInMs;
    public long mBackgroundUsageTimeInMs;
    public double mConsumePower;
    // A BatteryHistEntry corresponding to this diff usage data.
    public final BatteryHistEntry mBatteryHistEntry;
    private double mTotalConsumePower;
    private double mPercentOfTotal;

    private Context mContext;
    private String mAppLabel = null;
    private Drawable mAppIcon = null;
    private boolean mIsLoaded = false;

    public BatteryDiffEntry(
            Context context,
            long foregroundUsageTimeInMs,
            long backgroundUsageTimeInMs,
            double consumePower,
            BatteryHistEntry batteryHistEntry) {
        mContext = context;
        mConsumePower = consumePower;
        mForegroundUsageTimeInMs = foregroundUsageTimeInMs;
        mBackgroundUsageTimeInMs = backgroundUsageTimeInMs;
        mBatteryHistEntry = batteryHistEntry;
    }

    /** Sets the total consumed power in a specific time slot. */
    public void setTotalConsumePower(double totalConsumePower) {
        mTotalConsumePower = totalConsumePower;
        mPercentOfTotal = totalConsumePower == 0
            ? 0 : (mConsumePower / mTotalConsumePower) * 100.0;
    }

    /** Gets the percentage of total consumed power. */
    public double getPercentOfTotal() {
        return mPercentOfTotal;
    }

    /** Clones a new instance. */
    public BatteryDiffEntry clone() {
        return new BatteryDiffEntry(
            this.mContext,
            this.mForegroundUsageTimeInMs,
            this.mBackgroundUsageTimeInMs,
            this.mConsumePower,
            this.mBatteryHistEntry /*same instance*/);
    }

    /** Gets the app label name for this entry. */
    public String getAppLabel() {
        loadLabelAndIcon();
        return mAppLabel;
    }

    /** Gets the app icon {@link Drawable} for this entry. */
    public Drawable getAppIcon() {
        loadLabelAndIcon();
        return mAppIcon;
    }

    private void loadLabelAndIcon() {
        if (mIsLoaded) {
            return;
        }
        mIsLoaded = true;
        // Loads application icon and label based on consumer type.
        switch (mBatteryHistEntry.mConsumerType) {
            case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
                final BatteryEntry.NameAndIcon nameAndIconForUser =
                    BatteryEntry.getNameAndIconFromUserId(
                        mContext, (int) mBatteryHistEntry.mUserId);
                if (nameAndIconForUser != null) {
                    mAppIcon = nameAndIconForUser.icon;
                    mAppLabel = nameAndIconForUser.name;
                }
                break;
            case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                final BatteryEntry.NameAndIcon nameAndIconForSystem =
                    BatteryEntry.getNameAndIconFromDrainType(
                        mContext, mBatteryHistEntry.mDrainType);
                if (nameAndIconForSystem != null) {
                    mAppLabel = nameAndIconForSystem.name;
                    if (nameAndIconForSystem.iconId != 0) {
                        mAppIcon = mContext.getDrawable(nameAndIconForSystem.iconId);
                    }
                }
                break;
            case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
                loadNameAndIconForUid();
                break;
        }
    }

    private void loadNameAndIconForUid() {
        // TODO(b/185187669) fetch label and icon for UID battery type
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()
            .append("BatteryDiffEntry{")
            .append("\n\tname=" + mBatteryHistEntry.mAppLabel)
            .append(String.format("\n\tconsume=%.2f%% %f/%f",
                  mPercentOfTotal, mConsumePower, mTotalConsumePower))
            .append(String.format("\n\tforeground:%d background:%d",
                  Duration.ofMillis(mForegroundUsageTimeInMs).getSeconds(),
                  Duration.ofMillis(mBackgroundUsageTimeInMs).getSeconds()))
            .append(String.format("\n\tpackage:%s uid:%s",
                  mBatteryHistEntry.mPackageName, mBatteryHistEntry.mUid));
        return builder.toString();
    }
}
