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
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.utils.StringUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** A container class to carry battery data in a specific time slot. */
public class BatteryDiffEntry {
    private static final String TAG = "BatteryDiffEntry";

    static Locale sCurrentLocale = null;
    // Caches app label and icon to improve loading performance.
    static final Map<String, BatteryEntry.NameAndIcon> sResourceCache = new HashMap<>();

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
    private UserManager mUserManager;
    private String mDefaultPackageName = null;

    @VisibleForTesting int mAppIconId;
    @VisibleForTesting String mAppLabel = null;
    @VisibleForTesting Drawable mAppIcon = null;
    @VisibleForTesting boolean mIsLoaded = false;

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
        mUserManager = context.getSystemService(UserManager.class);
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
        // Returns default applicationn label if we cannot find it.
        return mAppLabel == null || mAppLabel.length() == 0
            ? mBatteryHistEntry.mAppLabel
            : mAppLabel;
    }

    /** Gets the app icon {@link Drawable} for this entry. */
    public Drawable getAppIcon() {
        loadLabelAndIcon();
        return mAppIcon;
    }

    /** Gets the app icon id for this entry. */
    public int getAppIconId() {
        loadLabelAndIcon();
        return mAppIconId;
    }

    /** Gets the searching package name for UID battery type. */
    public String getPackageName() {
        return mDefaultPackageName != null
            ? mDefaultPackageName : mBatteryHistEntry.mPackageName;
    }

    /** Whether the current BatteryDiffEntry is system component or not. */
    public boolean isSystemEntry() {
        switch (mBatteryHistEntry.mConsumerType) {
            case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
            case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                return true;
            case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
                return isSystemUid((int) mBatteryHistEntry.mUid)
                    || mBatteryHistEntry.mIsHidden;
        }
        return false;
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
                    BatteryEntry.getNameAndIconFromPowerComponent(
                        mContext, mBatteryHistEntry.mDrainType);
                if (nameAndIconForSystem != null) {
                    mAppLabel = nameAndIconForSystem.name;
                    if (nameAndIconForSystem.iconId != 0) {
                        mAppIconId = nameAndIconForSystem.iconId;
                        mAppIcon = mContext.getDrawable(nameAndIconForSystem.iconId);
                    }
                }
                break;
            case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
                final BatteryEntry.NameAndIcon nameAndIcon = getCache();
                if (nameAndIcon != null) {
                    mAppLabel = nameAndIcon.name;
                    mAppIcon = nameAndIcon.icon;
                    break;
                }
                loadNameAndIconForUid();
                // Uses application default icon if we cannot find it from package.
                if (mAppIcon == null) {
                    mAppIcon = mContext.getPackageManager().getDefaultActivityIcon();
                }
                // Adds badge icon into app icon for work profile.
                mAppIcon = getBadgeIconForUser(mAppIcon);
                if (mAppLabel != null || mAppIcon != null) {
                    sResourceCache.put(
                        mBatteryHistEntry.getKey(),
                        new BatteryEntry.NameAndIcon(mAppLabel, mAppIcon, /*iconId=*/ 0));
                }
                break;
        }
    }

    private BatteryEntry.NameAndIcon getCache() {
        final Locale locale = Locale.getDefault();
        if (sCurrentLocale != locale) {
            Log.d(TAG, String.format("clearCache() locale is changed from %s to %s",
                sCurrentLocale, locale));
            sCurrentLocale = locale;
            clearCache();
        }
        return sResourceCache.get(mBatteryHistEntry.getKey());
    }

    private void loadNameAndIconForUid() {
        final String packageName = mBatteryHistEntry.mPackageName;
        final PackageManager packageManager = mContext.getPackageManager();
        // Gets the application label from PackageManager.
        if (packageName != null && packageName.length() != 0) {
            try {
                final ApplicationInfo appInfo =
                    packageManager.getApplicationInfo(packageName, /*no flags*/ 0);
                if (appInfo != null) {
                    mAppLabel = packageManager.getApplicationLabel(appInfo).toString();
                    mAppIcon = packageManager.getApplicationIcon(appInfo);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "failed to retrieve ApplicationInfo for: " + packageName);
                mAppLabel = packageName;
            }
        }
        // Early return if we found the app label and icon resource.
        if (mAppLabel != null && mAppIcon != null) {
            return;
        }

        final int uid = (int) mBatteryHistEntry.mUid;
        final String[] packages = packageManager.getPackagesForUid(uid);
        // Loads special defined application label and icon if available.
        if (packages == null || packages.length == 0) {
            final BatteryEntry.NameAndIcon nameAndIcon =
                BatteryEntry.getNameAndIconFromUid(mContext, mAppLabel, uid);
            mAppLabel = nameAndIcon.name;
            mAppIcon = nameAndIcon.icon;
        }

        final BatteryEntry.NameAndIcon nameAndIcon =
            BatteryEntry.loadNameAndIcon(
                mContext, uid, /*handler=*/ null, /*batteryEntry=*/ null,
                packageName, mAppLabel, mAppIcon);
        // Clears BatteryEntry internal cache since we will have another one.
        BatteryEntry.clearUidCache();
        if (nameAndIcon != null) {
            mAppLabel = nameAndIcon.name;
            mAppIcon = nameAndIcon.icon;
            mDefaultPackageName = nameAndIcon.packageName;
            if (mDefaultPackageName != null
                    && !mDefaultPackageName.equals(nameAndIcon.packageName)) {
                Log.w(TAG, String.format("found different package: %s | %s",
                    mDefaultPackageName, nameAndIcon.packageName));
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()
            .append("BatteryDiffEntry{")
            .append("\n\tname=" + getAppLabel())
            .append(String.format("\n\tconsume=%.2f%% %f/%f",
                  mPercentOfTotal, mConsumePower, mTotalConsumePower))
            .append(String.format("\n\tforeground:%s background:%s",
                  StringUtil.formatElapsedTime(mContext, mForegroundUsageTimeInMs,
                      /*withSeconds=*/ true, /*collapseTimeUnit=*/ false),
                  StringUtil.formatElapsedTime(mContext, mBackgroundUsageTimeInMs,
                      /*withSeconds=*/ true, /*collapseTimeUnit=*/ false)))
            .append(String.format("\n\tpackage:%s|%s uid:%d userId:%d",
                  mBatteryHistEntry.mPackageName, getPackageName(),
                  mBatteryHistEntry.mUid, mBatteryHistEntry.mUserId));
        return builder.toString();
    }

    static void clearCache() {
        sResourceCache.clear();
    }

    private Drawable getBadgeIconForUser(Drawable icon) {
        final int userId = UserHandle.getUserId((int) mBatteryHistEntry.mUid);
        final UserHandle userHandle = new UserHandle(userId);
        return mUserManager.getBadgedIconForUser(icon, userHandle);
    }

    private static boolean isSystemUid(int uid) {
        final int appUid = UserHandle.getAppId(uid);
        return appUid >= Process.SYSTEM_UID && appUid < Process.FIRST_APPLICATION_UID;
    }
}
