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

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
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
    // Whether a specific item is valid to launch restriction page?
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public static final Map<String, Boolean> sValidForRestriction = new HashMap<>();

    /** A comparator for {@link BatteryDiffEntry} based on consumed percentage. */
    public static final Comparator<BatteryDiffEntry> COMPARATOR =
            (a, b) -> Double.compare(b.getPercentOfTotal(), a.getPercentOfTotal());

    public long mForegroundUsageTimeInMs;
    public long mBackgroundUsageTimeInMs;
    public long mScreenOnTimeInMs;
    public double mConsumePower;
    public double mForegroundUsageConsumePower;
    public double mForegroundServiceUsageConsumePower;
    public double mBackgroundUsageConsumePower;
    public double mCachedUsageConsumePower;
    // A BatteryHistEntry corresponding to this diff usage data.
    public final BatteryHistEntry mBatteryHistEntry;

    private double mTotalConsumePower;
    private double mPercentOfTotal;

    private Context mContext;
    private UserManager mUserManager;
    private String mDefaultPackageName = null;

    @VisibleForTesting
    int mAppIconId;
    @VisibleForTesting
    String mAppLabel = null;
    @VisibleForTesting
    Drawable mAppIcon = null;
    @VisibleForTesting
    boolean mIsLoaded = false;
    @VisibleForTesting
    boolean mValidForRestriction = true;

    public BatteryDiffEntry(
            Context context,
            long foregroundUsageTimeInMs,
            long backgroundUsageTimeInMs,
            long screenOnTimeInMs,
            double consumePower,
            double foregroundUsageConsumePower,
            double foregroundServiceUsageConsumePower,
            double backgroundUsageConsumePower,
            double cachedUsageConsumePower,
            BatteryHistEntry batteryHistEntry) {
        mContext = context;
        mConsumePower = consumePower;
        mForegroundUsageConsumePower = foregroundUsageConsumePower;
        mForegroundServiceUsageConsumePower = foregroundServiceUsageConsumePower;
        mBackgroundUsageConsumePower = backgroundUsageConsumePower;
        mCachedUsageConsumePower = cachedUsageConsumePower;
        mForegroundUsageTimeInMs = foregroundUsageTimeInMs;
        mBackgroundUsageTimeInMs = backgroundUsageTimeInMs;
        mScreenOnTimeInMs = screenOnTimeInMs;
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
                this.mScreenOnTimeInMs,
                this.mConsumePower,
                this.mForegroundUsageConsumePower,
                this.mForegroundServiceUsageConsumePower,
                this.mBackgroundUsageConsumePower,
                this.mCachedUsageConsumePower,
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
        return mAppIcon != null && mAppIcon.getConstantState() != null
                ? mAppIcon.getConstantState().newDrawable()
                : null;
    }

    /** Gets the app icon id for this entry. */
    public int getAppIconId() {
        loadLabelAndIcon();
        return mAppIconId;
    }

    /** Gets the searching package name for UID battery type. */
    public String getPackageName() {
        final String packageName = mDefaultPackageName != null
                ? mDefaultPackageName : mBatteryHistEntry.mPackageName;
        if (packageName == null) {
            return packageName;
        }
        // Removes potential appended process name in the PackageName.
        // From "com.opera.browser:privileged_process0" to "com.opera.browser"
        final String[] splitPackageNames = packageName.split(":");
        return splitPackageNames != null && splitPackageNames.length > 0
                ? splitPackageNames[0] : packageName;
    }

    /** Whether this item is valid for users to launch restriction page? */
    public boolean validForRestriction() {
        loadLabelAndIcon();
        return mValidForRestriction;
    }

    /** Whether the current BatteryDiffEntry is system component or not. */
    public boolean isSystemEntry() {
        switch (mBatteryHistEntry.mConsumerType) {
            case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
            case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                return true;
            case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
                final int uid = (int) mBatteryHistEntry.mUid;
                if (mBatteryHistEntry.mIsHidden
                        || uid == BatteryUtils.UID_REMOVED_APPS
                        || uid == BatteryUtils.UID_TETHERING) {
                    return true;
                }
                final boolean combineSystemComponents =
                        mContext.getResources().getBoolean(
                                R.bool.config_battery_combine_system_components);
                return combineSystemComponents && isSystemUid(uid);
        }
        return false;
    }

    void loadLabelAndIcon() {
        if (mIsLoaded) {
            return;
        }
        // Checks whether we have cached data or not first before fetching.
        final BatteryEntry.NameAndIcon nameAndIcon = getCache();
        if (nameAndIcon != null) {
            mAppLabel = nameAndIcon.mName;
            mAppIcon = nameAndIcon.mIcon;
            mAppIconId = nameAndIcon.mIconId;
        }
        final Boolean validForRestriction = sValidForRestriction.get(getKey());
        if (validForRestriction != null) {
            mValidForRestriction = validForRestriction;
        }
        // Both nameAndIcon and restriction configuration have cached data.
        if (nameAndIcon != null && validForRestriction != null) {
            return;
        }
        mIsLoaded = true;

        // Configures whether we can launch restriction page or not.
        updateRestrictionFlagState();
        sValidForRestriction.put(getKey(), Boolean.valueOf(mValidForRestriction));

        // Loads application icon and label based on consumer type.
        switch (mBatteryHistEntry.mConsumerType) {
            case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
                final BatteryEntry.NameAndIcon nameAndIconForUser =
                        BatteryEntry.getNameAndIconFromUserId(
                                mContext, (int) mBatteryHistEntry.mUserId);
                if (nameAndIconForUser != null) {
                    mAppIcon = nameAndIconForUser.mIcon;
                    mAppLabel = nameAndIconForUser.mName;
                    sResourceCache.put(
                            getKey(),
                            new BatteryEntry.NameAndIcon(mAppLabel, mAppIcon, /*iconId=*/ 0));
                }
                break;
            case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                final BatteryEntry.NameAndIcon nameAndIconForSystem =
                        BatteryEntry.getNameAndIconFromPowerComponent(
                                mContext, mBatteryHistEntry.mDrainType);
                if (nameAndIconForSystem != null) {
                    mAppLabel = nameAndIconForSystem.mName;
                    if (nameAndIconForSystem.mIconId != 0) {
                        mAppIconId = nameAndIconForSystem.mIconId;
                        mAppIcon = mContext.getDrawable(nameAndIconForSystem.mIconId);
                    }
                    sResourceCache.put(
                            getKey(),
                            new BatteryEntry.NameAndIcon(mAppLabel, mAppIcon, mAppIconId));
                }
                break;
            case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
                loadNameAndIconForUid();
                // Uses application default icon if we cannot find it from package.
                if (mAppIcon == null) {
                    mAppIcon = mContext.getPackageManager().getDefaultActivityIcon();
                }
                // Adds badge icon into app icon for work profile.
                mAppIcon = getBadgeIconForUser(mAppIcon);
                if (mAppLabel != null || mAppIcon != null) {
                    sResourceCache.put(
                            getKey(),
                            new BatteryEntry.NameAndIcon(mAppLabel, mAppIcon, /*iconId=*/ 0));
                }
                break;
        }
    }

    @VisibleForTesting
    String getKey() {
        return mBatteryHistEntry.getKey();
    }

    @VisibleForTesting
    void updateRestrictionFlagState() {
        mValidForRestriction = true;
        if (!mBatteryHistEntry.isAppEntry()) {
            return;
        }
        final boolean isValidPackage =
                BatteryUtils.getInstance(mContext).getPackageUid(getPackageName())
                        != BatteryUtils.UID_NULL;
        if (!isValidPackage) {
            mValidForRestriction = false;
            return;
        }
        try {
            mValidForRestriction =
                    mContext.getPackageManager().getPackageInfo(
                            getPackageName(),
                            PackageManager.MATCH_DISABLED_COMPONENTS
                                    | PackageManager.MATCH_ANY_USER
                                    | PackageManager.GET_SIGNATURES
                                    | PackageManager.GET_PERMISSIONS)
                            != null;
        } catch (Exception e) {
            Log.e(TAG, String.format("getPackageInfo() error %s for package=%s",
                    e.getCause(), getPackageName()));
            mValidForRestriction = false;
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
        return sResourceCache.get(getKey());
    }

    private void loadNameAndIconForUid() {
        final String packageName = getPackageName();
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
            mAppLabel = nameAndIcon.mName;
            mAppIcon = nameAndIcon.mIcon;
        }

        final BatteryEntry.NameAndIcon nameAndIcon = BatteryEntry.loadNameAndIcon(
                mContext, uid, /*batteryEntry=*/ null, packageName, mAppLabel, mAppIcon);
        // Clears BatteryEntry internal cache since we will have another one.
        BatteryEntry.clearUidCache();
        if (nameAndIcon != null) {
            mAppLabel = nameAndIcon.mName;
            mAppIcon = nameAndIcon.mIcon;
            mDefaultPackageName = nameAndIcon.mPackageName;
            if (mDefaultPackageName != null
                    && !mDefaultPackageName.equals(nameAndIcon.mPackageName)) {
                Log.w(TAG, String.format("found different package: %s | %s",
                        mDefaultPackageName, nameAndIcon.mPackageName));
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()
                .append("BatteryDiffEntry{")
                .append(String.format("\n\tname=%s restrictable=%b",
                        mAppLabel, mValidForRestriction))
                .append(String.format("\n\tconsume=%.2f%% %f/%f",
                        mPercentOfTotal, mConsumePower, mTotalConsumePower))
                .append(String.format("\n\tconsume power= foreground:%f foregroundService:%f",
                        mForegroundUsageConsumePower, mForegroundServiceUsageConsumePower))
                .append(String.format("\n\tconsume power= background:%f cached:%f",
                        mBackgroundUsageConsumePower, mCachedUsageConsumePower))
                .append(String.format("\n\ttime= foreground:%s background:%s screen-on:%s",
                        StringUtil.formatElapsedTime(mContext, (double) mForegroundUsageTimeInMs,
                                /*withSeconds=*/ true, /*collapseTimeUnit=*/ false),
                        StringUtil.formatElapsedTime(mContext, (double) mBackgroundUsageTimeInMs,
                                /*withSeconds=*/ true, /*collapseTimeUnit=*/ false),
                        StringUtil.formatElapsedTime(mContext, (double) mScreenOnTimeInMs,
                                /*withSeconds=*/ true, /*collapseTimeUnit=*/ false)))
                .append(String.format("\n\tpackage:%s|%s uid:%d userId:%d",
                        mBatteryHistEntry.mPackageName, getPackageName(),
                        mBatteryHistEntry.mUid, mBatteryHistEntry.mUserId));
        return builder.toString();
    }

    /** Clears app icon and label cache data. */
    public static void clearCache() {
        sResourceCache.clear();
        sValidForRestriction.clear();
    }

    private Drawable getBadgeIconForUser(Drawable icon) {
        final int userId = UserHandle.getUserId((int) mBatteryHistEntry.mUid);
        return userId == UserHandle.USER_OWNER ? icon :
                mUserManager.getBadgedIconForUser(icon, new UserHandle(userId));
    }

    private static boolean isSystemUid(int uid) {
        final int appUid = UserHandle.getAppId(uid);
        return appUid >= Process.SYSTEM_UID && appUid < Process.FIRST_APPLICATION_UID;
    }
}
