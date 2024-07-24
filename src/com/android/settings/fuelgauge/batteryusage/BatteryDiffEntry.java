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
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry.NameAndIcon;
import com.android.settingslib.utils.StringUtil;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/** A container class to carry battery data in a specific time slot. */
public class BatteryDiffEntry {
    private static final String TAG = "BatteryDiffEntry";
    private static final Object sResourceCacheLock = new Object();
    private static final Object sPackageNameAndUidCacheLock = new Object();
    private static final Object sValidForRestrictionLock = new Object();

    static Locale sCurrentLocale = null;

    // Caches app label and icon to improve loading performance.
    @GuardedBy("sResourceCacheLock")
    static final Map<String, NameAndIcon> sResourceCache = new ArrayMap<>();

    // Caches package name and uid to improve loading performance.
    @GuardedBy("sPackageNameAndUidCacheLock")
    static final Map<String, Integer> sPackageNameAndUidCache = new ArrayMap<>();

    // Whether a specific item is valid to launch restriction page?
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @GuardedBy("sValidForRestrictionLock")
    static final Map<String, Boolean> sValidForRestriction = new ArrayMap<>();

    /** A comparator for {@link BatteryDiffEntry} based on the sorting key. */
    static final Comparator<BatteryDiffEntry> COMPARATOR =
            (a, b) -> Double.compare(b.getSortingKey(), a.getSortingKey());

    static final String SYSTEM_APPS_KEY = "A|SystemApps";
    static final String UNINSTALLED_APPS_KEY = "A|UninstalledApps";
    static final String OTHERS_KEY = "S|Others";

    // key -> (label_id, icon_id)
    private static final Map<String, Pair<Integer, Integer>> SPECIAL_ENTRY_MAP =
            Map.of(
                    SYSTEM_APPS_KEY,
                    Pair.create(R.string.battery_usage_system_apps, R.drawable.ic_power_system),
                    UNINSTALLED_APPS_KEY,
                    Pair.create(
                            R.string.battery_usage_uninstalled_apps,
                            R.drawable.ic_battery_uninstalled),
                    OTHERS_KEY,
                    Pair.create(
                            R.string.battery_usage_others,
                            R.drawable.ic_settings_battery_usage_others));

    public long mUid;
    public long mUserId;
    public String mKey;
    public boolean mIsHidden;
    public int mComponentId;
    public String mLegacyPackageName;
    public String mLegacyLabel;
    public int mConsumerType;
    public long mForegroundUsageTimeInMs;
    public long mForegroundServiceUsageTimeInMs;
    public long mBackgroundUsageTimeInMs;
    public long mScreenOnTimeInMs;
    public double mConsumePower;
    public double mForegroundUsageConsumePower;
    public double mForegroundServiceUsageConsumePower;
    public double mBackgroundUsageConsumePower;
    public double mCachedUsageConsumePower;

    protected Context mContext;

    private double mTotalConsumePower;
    private double mPercentage;
    private int mAdjustPercentageOffset;
    private UserManager mUserManager;
    private String mDefaultPackageName = null;

    @VisibleForTesting int mAppIconId;
    @VisibleForTesting String mAppLabel = null;
    @VisibleForTesting Drawable mAppIcon = null;
    @VisibleForTesting boolean mIsLoaded = false;
    @VisibleForTesting boolean mValidForRestriction = true;

    public BatteryDiffEntry(
            Context context,
            long uid,
            long userId,
            String key,
            boolean isHidden,
            int componentId,
            String legacyPackageName,
            String legacyLabel,
            int consumerType,
            long foregroundUsageTimeInMs,
            long foregroundServiceUsageTimeInMs,
            long backgroundUsageTimeInMs,
            long screenOnTimeInMs,
            double consumePower,
            double foregroundUsageConsumePower,
            double foregroundServiceUsageConsumePower,
            double backgroundUsageConsumePower,
            double cachedUsageConsumePower) {
        mContext = context;
        mUid = uid;
        mUserId = userId;
        mKey = key;
        mIsHidden = isHidden;
        mComponentId = componentId;
        mLegacyPackageName = legacyPackageName;
        mLegacyLabel = legacyLabel;
        mConsumerType = consumerType;
        mForegroundUsageTimeInMs = foregroundUsageTimeInMs;
        mForegroundServiceUsageTimeInMs = foregroundServiceUsageTimeInMs;
        mBackgroundUsageTimeInMs = backgroundUsageTimeInMs;
        mScreenOnTimeInMs = screenOnTimeInMs;
        mConsumePower = consumePower;
        mForegroundUsageConsumePower = foregroundUsageConsumePower;
        mForegroundServiceUsageConsumePower = foregroundServiceUsageConsumePower;
        mBackgroundUsageConsumePower = backgroundUsageConsumePower;
        mCachedUsageConsumePower = cachedUsageConsumePower;
        mUserManager = context.getSystemService(UserManager.class);
    }

    public BatteryDiffEntry(Context context, String key, String legacyLabel, int consumerType) {
        this(
                context,
                /* uid= */ 0,
                /* userId= */ 0,
                key,
                /* isHidden= */ false,
                /* componentId= */ -1,
                /* legacyPackageName= */ null,
                legacyLabel,
                consumerType,
                /* foregroundUsageTimeInMs= */ 0,
                /* foregroundServiceUsageTimeInMs= */ 0,
                /* backgroundUsageTimeInMs= */ 0,
                /* screenOnTimeInMs= */ 0,
                /* consumePower= */ 0,
                /* foregroundUsageConsumePower= */ 0,
                /* foregroundServiceUsageConsumePower= */ 0,
                /* backgroundUsageConsumePower= */ 0,
                /* cachedUsageConsumePower= */ 0);
    }

    /** Sets the total consumed power in a specific time slot. */
    public void setTotalConsumePower(double totalConsumePower) {
        mTotalConsumePower = totalConsumePower;
        mPercentage = totalConsumePower == 0 ? 0 : (mConsumePower / mTotalConsumePower) * 100.0;
        mAdjustPercentageOffset = 0;
    }

    /** Gets the total consumed power in a specific time slot. */
    public double getTotalConsumePower() {
        return mTotalConsumePower;
    }

    /** Gets the percentage of total consumed power. */
    public double getPercentage() {
        return mPercentage;
    }

    /** Gets the percentage offset to adjust. */
    public double getAdjustPercentageOffset() {
        return mAdjustPercentageOffset;
    }

    /** Sets the percentage offset to adjust. */
    public void setAdjustPercentageOffset(int offset) {
        mAdjustPercentageOffset = offset;
    }

    /** Gets the key for sorting */
    public double getSortingKey() {
        String key = getKey();
        if (key == null) {
            return getPercentage() + getAdjustPercentageOffset();
        }

        // For special entries, put them to the end of the list.
        switch (key) {
            case UNINSTALLED_APPS_KEY:
            case OTHERS_KEY:
                return -1;
            case SYSTEM_APPS_KEY:
                return -2;
            default:
                return getPercentage() + getAdjustPercentageOffset();
        }
    }

    /** Clones a new instance. */
    public BatteryDiffEntry clone() {
        return new BatteryDiffEntry(
                this.mContext,
                this.mUid,
                this.mUserId,
                this.mKey,
                this.mIsHidden,
                this.mComponentId,
                this.mLegacyPackageName,
                this.mLegacyLabel,
                this.mConsumerType,
                this.mForegroundUsageTimeInMs,
                this.mForegroundServiceUsageTimeInMs,
                this.mBackgroundUsageTimeInMs,
                this.mScreenOnTimeInMs,
                this.mConsumePower,
                this.mForegroundUsageConsumePower,
                this.mForegroundServiceUsageConsumePower,
                this.mBackgroundUsageConsumePower,
                this.mCachedUsageConsumePower);
    }

    /** Gets the app label name for this entry. */
    public String getAppLabel() {
        loadLabelAndIcon();
        // Returns default application label if we cannot find it.
        return mAppLabel == null || mAppLabel.length() == 0 ? mLegacyLabel : mAppLabel;
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
        final String packageName =
                mDefaultPackageName != null ? mDefaultPackageName : mLegacyPackageName;
        if (packageName == null) {
            return packageName;
        }
        // Removes potential appended process name in the PackageName.
        // From "com.opera.browser:privileged_process0" to "com.opera.browser"
        final String[] splitPackageNames = packageName.split(":");
        return splitPackageNames != null && splitPackageNames.length > 0
                ? splitPackageNames[0]
                : packageName;
    }

    /** Whether this item is valid for users to launch restriction page? */
    public boolean validForRestriction() {
        loadLabelAndIcon();
        return mValidForRestriction;
    }

    /** Whether the current BatteryDiffEntry is system component or not. */
    public boolean isSystemEntry() {
        if (mIsHidden) {
            return false;
        }
        switch (mConsumerType) {
            case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
            case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                return true;
            case ConvertUtils.CONSUMER_TYPE_UID_BATTERY:
            default:
                return false;
        }
    }

    /** Whether the current BatteryDiffEntry is uninstalled app or not. */
    public boolean isUninstalledEntry() {
        final String packageName = getPackageName();
        if (TextUtils.isEmpty(packageName)
                || isSystemEntry()
                // Some special package UIDs could be 0. Those packages are not installed by users.
                || mUid == BatteryUtils.UID_ZERO) {
            return false;
        }

        final int uid = getPackageUid(packageName);
        return uid == BatteryUtils.UID_REMOVED_APPS || uid == BatteryUtils.UID_NULL;
    }

    private int getPackageUid(String packageName) {
        synchronized (sPackageNameAndUidCacheLock) {
            if (sPackageNameAndUidCache.containsKey(packageName)) {
                return sPackageNameAndUidCache.get(packageName);
            }
        }

        int uid =
                BatteryUtils.getInstance(mContext).getPackageUidAsUser(packageName, (int) mUserId);
        synchronized (sPackageNameAndUidCacheLock) {
            sPackageNameAndUidCache.put(packageName, uid);
        }
        return uid;
    }

    void loadLabelAndIcon() {
        if (mIsLoaded) {
            return;
        }
        // Checks whether we have cached data or not first before fetching.
        final NameAndIcon nameAndIcon = getCache();
        if (nameAndIcon != null) {
            mAppLabel = nameAndIcon.mName;
            mAppIcon = nameAndIcon.mIcon;
            mAppIconId = nameAndIcon.mIconId;
        }
        Boolean validForRestriction = null;
        synchronized (sValidForRestrictionLock) {
            validForRestriction = sValidForRestriction.get(getKey());
        }
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
        synchronized (sValidForRestrictionLock) {
            sValidForRestriction.put(getKey(), Boolean.valueOf(mValidForRestriction));
        }

        if (getKey() != null && SPECIAL_ENTRY_MAP.containsKey(getKey())) {
            Pair<Integer, Integer> pair = SPECIAL_ENTRY_MAP.get(getKey());
            mAppLabel = mContext.getString(pair.first);
            mAppIconId = pair.second;
            mAppIcon = mContext.getDrawable(mAppIconId);
            putResourceCache(getKey(), new NameAndIcon(mAppLabel, mAppIcon, mAppIconId));
            return;
        }

        // Loads application icon and label based on consumer type.
        switch (mConsumerType) {
            case ConvertUtils.CONSUMER_TYPE_USER_BATTERY:
                final NameAndIcon nameAndIconForUser =
                        BatteryEntry.getNameAndIconFromUserId(mContext, (int) mUserId);
                if (nameAndIconForUser != null) {
                    mAppIcon = nameAndIconForUser.mIcon;
                    mAppLabel = nameAndIconForUser.mName;
                    putResourceCache(
                            getKey(), new NameAndIcon(mAppLabel, mAppIcon, /* iconId= */ 0));
                }
                break;
            case ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY:
                final NameAndIcon nameAndIconForSystem =
                        BatteryEntry.getNameAndIconFromPowerComponent(mContext, mComponentId);
                if (nameAndIconForSystem != null) {
                    mAppLabel = nameAndIconForSystem.mName;
                    if (nameAndIconForSystem.mIconId != 0) {
                        mAppIconId = nameAndIconForSystem.mIconId;
                        mAppIcon = mContext.getDrawable(nameAndIconForSystem.mIconId);
                    }
                    putResourceCache(getKey(), new NameAndIcon(mAppLabel, mAppIcon, mAppIconId));
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
                    putResourceCache(
                            getKey(), new NameAndIcon(mAppLabel, mAppIcon, /* iconId= */ 0));
                }
                break;
        }
    }

    String getKey() {
        return mKey;
    }

    @VisibleForTesting
    void updateRestrictionFlagState() {
        if (isSystemEntry()) {
            mValidForRestriction = false;
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
                    mContext.getPackageManager()
                                    .getPackageInfo(
                                            getPackageName(),
                                            PackageManager.MATCH_DISABLED_COMPONENTS
                                                    | PackageManager.MATCH_ANY_USER
                                                    | PackageManager.GET_SIGNATURES
                                                    | PackageManager.GET_PERMISSIONS)
                            != null;
        } catch (Exception e) {
            Log.e(
                    TAG,
                    String.format(
                            "getPackageInfo() error %s for package=%s",
                            e.getCause(), getPackageName()));
            mValidForRestriction = false;
        }
    }

    private NameAndIcon getCache() {
        final Locale locale = Locale.getDefault();
        if (sCurrentLocale != locale) {
            Log.d(
                    TAG,
                    String.format(
                            "clearCache() locale is changed from %s to %s",
                            sCurrentLocale, locale));
            sCurrentLocale = locale;
            clearCache();
        }
        synchronized (sResourceCacheLock) {
            return sResourceCache.get(getKey());
        }
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

        final int uid = (int) mUid;
        final String[] packages = packageManager.getPackagesForUid(uid);
        // Loads special defined application label and icon if available.
        if (packages == null || packages.length == 0) {
            final NameAndIcon nameAndIcon =
                    BatteryEntry.getNameAndIconFromUid(mContext, mAppLabel, uid);
            mAppLabel = nameAndIcon.mName;
            mAppIcon = nameAndIcon.mIcon;
        }

        final NameAndIcon nameAndIcon =
                BatteryEntry.loadNameAndIcon(
                        mContext, uid, /* batteryEntry= */ null, packageName, mAppLabel, mAppIcon);
        // Clears BatteryEntry internal cache since we will have another one.
        BatteryEntry.clearUidCache();
        if (nameAndIcon != null) {
            mAppLabel = nameAndIcon.mName;
            mAppIcon = nameAndIcon.mIcon;
            mDefaultPackageName = nameAndIcon.mPackageName;
            if (mDefaultPackageName != null
                    && !mDefaultPackageName.equals(nameAndIcon.mPackageName)) {
                Log.w(
                        TAG,
                        String.format(
                                "found different package: %s | %s",
                                mDefaultPackageName, nameAndIcon.mPackageName));
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BatteryDiffEntry{");
        builder.append(
                String.format("\n\tname=%s restrictable=%b", mAppLabel, mValidForRestriction));
        builder.append(
                String.format(
                        "\n\tconsume=%.2f%% %f/%f",
                        mPercentage, mConsumePower, mTotalConsumePower));
        builder.append(
                String.format(
                        "\n\tconsume power= foreground:%f foregroundService:%f",
                        mForegroundUsageConsumePower, mForegroundServiceUsageConsumePower));
        builder.append(
                String.format(
                        "\n\tconsume power= background:%f cached:%f",
                        mBackgroundUsageConsumePower, mCachedUsageConsumePower));
        builder.append(
                String.format(
                        "\n\ttime= foreground:%s foregroundService:%s "
                                + "background:%s screen-on:%s",
                        StringUtil.formatElapsedTime(
                                mContext,
                                (double) mForegroundUsageTimeInMs,
                                /* withSeconds= */ true,
                                /* collapseTimeUnit= */ false),
                        StringUtil.formatElapsedTime(
                                mContext,
                                (double) mForegroundServiceUsageTimeInMs,
                                /* withSeconds= */ true,
                                /* collapseTimeUnit= */ false),
                        StringUtil.formatElapsedTime(
                                mContext,
                                (double) mBackgroundUsageTimeInMs,
                                /* withSeconds= */ true,
                                /* collapseTimeUnit= */ false),
                        StringUtil.formatElapsedTime(
                                mContext,
                                (double) mScreenOnTimeInMs,
                                /* withSeconds= */ true,
                                /* collapseTimeUnit= */ false)));
        builder.append(
                String.format(
                        "\n\tpackage:%s|%s uid:%d userId:%d",
                        mLegacyPackageName, getPackageName(), mUid, mUserId));
        return builder.toString();
    }

    /** Clears all cache data. */
    public static void clearCache() {
        synchronized (sResourceCacheLock) {
            sResourceCache.clear();
        }
        synchronized (sValidForRestrictionLock) {
            sValidForRestriction.clear();
        }
        synchronized (sPackageNameAndUidCacheLock) {
            sPackageNameAndUidCache.clear();
        }
    }

    private static void putResourceCache(String key, NameAndIcon nameAndIcon) {
        synchronized (sResourceCacheLock) {
            sResourceCache.put(key, nameAndIcon);
        }
    }

    private Drawable getBadgeIconForUser(Drawable icon) {
        final int userId = UserHandle.getUserId((int) mUid);
        return userId == UserHandle.USER_OWNER
                ? icon
                : mUserManager.getBadgedIconForUser(icon, new UserHandle(userId));
    }
}
