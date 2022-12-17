/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.annotation.IntDef;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** A utility class for application usage operation. */
public class BatteryOptimizeUtils {
    private static final String TAG = "BatteryOptimizeUtils";
    private static final String UNKNOWN_PACKAGE = "unknown";

    @VisibleForTesting AppOpsManager mAppOpsManager;
    @VisibleForTesting BatteryUtils mBatteryUtils;
    @VisibleForTesting PowerAllowlistBackend mPowerAllowListBackend;
    @VisibleForTesting int mMode;
    @VisibleForTesting boolean mAllowListed;

    private final String mPackageName;
    private final int mUid;

    // If current user is admin, match apps from all users. Otherwise, only match the currect user.
    private static final int RETRIEVE_FLAG_ADMIN =
            PackageManager.MATCH_ANY_USER
                | PackageManager.MATCH_DISABLED_COMPONENTS
                | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;
    private static final int RETRIEVE_FLAG =
            PackageManager.MATCH_DISABLED_COMPONENTS
                | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;

    // Optimization modes.
    static final int MODE_UNKNOWN = 0;
    static final int MODE_RESTRICTED = 1;
    static final int MODE_UNRESTRICTED = 2;
    static final int MODE_OPTIMIZED = 3;

    @IntDef(prefix = {"MODE_"}, value = {
        MODE_UNKNOWN,
        MODE_RESTRICTED,
        MODE_UNRESTRICTED,
        MODE_OPTIMIZED,
    })
    @Retention(RetentionPolicy.SOURCE)
    static @interface OptimizationMode {}

    public BatteryOptimizeUtils(Context context, int uid, String packageName) {
        mUid = uid;
        mPackageName = packageName;
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mBatteryUtils = BatteryUtils.getInstance(context);
        mPowerAllowListBackend = PowerAllowlistBackend.getInstance(context);
        mMode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mPackageName);
        mAllowListed = mPowerAllowListBackend.isAllowlisted(mPackageName);
    }

    /** Gets the {@link OptimizationMode} based on mode and allowed list. */
    @OptimizationMode
    public static int getAppOptimizationMode(int mode, boolean isAllowListed) {
        if (!isAllowListed && mode == AppOpsManager.MODE_IGNORED) {
            return MODE_RESTRICTED;
        } else if (isAllowListed && mode == AppOpsManager.MODE_ALLOWED) {
            return MODE_UNRESTRICTED;
        } else if (!isAllowListed && mode == AppOpsManager.MODE_ALLOWED) {
            return MODE_OPTIMIZED;
        } else {
            return MODE_UNKNOWN;
        }
    }

    /** Gets the {@link OptimizationMode} for associated app. */
    @OptimizationMode
    public int getAppOptimizationMode() {
        refreshState();
        return getAppOptimizationMode(mMode, mAllowListed);
    }

    /** Resets optimization mode for all applications. */
    public static void resetAppOptimizationMode(
            Context context, IPackageManager ipm, AppOpsManager aom) {
        resetAppOptimizationMode(context, ipm, aom,
                PowerAllowlistBackend.getInstance(context), BatteryUtils.getInstance(context));
    }

    /** Sets the {@link OptimizationMode} for associated app. */
    public void setAppUsageState(@OptimizationMode int mode) {
        if (getAppOptimizationMode(mMode, mAllowListed) == mode) {
            Log.w(TAG, "set the same optimization mode for: " + mPackageName);
            return;
        }
        setAppUsageStateInternal(mode, mUid, mPackageName, mBatteryUtils, mPowerAllowListBackend);
    }

    /**
     * Return {@code true} if package name is valid (can get an uid).
     */
    public boolean isValidPackageName() {
        return mBatteryUtils.getPackageUid(mPackageName) != BatteryUtils.UID_NULL;
    }

    /**
     * Return {@code true} if this package is system or default active app.
     */
    public boolean isSystemOrDefaultApp() {
        mPowerAllowListBackend.refreshList();
        return isSystemOrDefaultApp(mPowerAllowListBackend, mPackageName);
    }

    /**
      * Gets the list of installed applications.
      */
    public static ArraySet<ApplicationInfo> getInstalledApplications(
            Context context, IPackageManager ipm) {
        final ArraySet<ApplicationInfo> applications = new ArraySet<>();
        final UserManager um = context.getSystemService(UserManager.class);
        for (UserInfo userInfo : um.getProfiles(UserHandle.myUserId())) {
            try {
                @SuppressWarnings("unchecked")
                final ParceledListSlice<ApplicationInfo> infoList = ipm.getInstalledApplications(
                        userInfo.isAdmin() ? RETRIEVE_FLAG_ADMIN : RETRIEVE_FLAG,
                        userInfo.id);
                if (infoList != null) {
                    applications.addAll(infoList.getList());
                }
            } catch (Exception e) {
                Log.e(TAG, "getInstalledApplications() is failed", e);
                return null;
            }
        }
        // Removes the application which is disabled by the system.
        applications.removeIf(
                info -> info.enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    && !info.enabled);
        return applications;
    }

    @VisibleForTesting
    static void resetAppOptimizationMode(
            Context context, IPackageManager ipm, AppOpsManager aom,
            PowerAllowlistBackend allowlistBackend, BatteryUtils batteryUtils) {
        final ArraySet<ApplicationInfo> applications = getInstalledApplications(context, ipm);
        if (applications == null || applications.isEmpty()) {
            Log.w(TAG, "no data found in the getInstalledApplications()");
            return;
        }

        allowlistBackend.refreshList();
        // Resets optimization mode for each application.
        for (ApplicationInfo info : applications) {
            final int mode = aom.checkOpNoThrow(
                    AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, info.uid, info.packageName);
            @OptimizationMode
            final int optimizationMode = getAppOptimizationMode(
                    mode, allowlistBackend.isAllowlisted(info.packageName));
            // Ignores default optimized/unknown state or system/default apps.
            if (optimizationMode == MODE_OPTIMIZED
                    || optimizationMode == MODE_UNKNOWN
                    || isSystemOrDefaultApp(allowlistBackend, info.packageName)) {
                continue;
            }

            // Resets to the default mode: MODE_OPTIMIZED.
            setAppUsageStateInternal(MODE_OPTIMIZED, info.uid, info.packageName, batteryUtils,
                    allowlistBackend);
        }
    }

    String getPackageName() {
        return mPackageName == null ? UNKNOWN_PACKAGE : mPackageName;
    }

    private static boolean isSystemOrDefaultApp(
            PowerAllowlistBackend powerAllowlistBackend, String packageName) {
        return powerAllowlistBackend.isSysAllowlisted(packageName)
                || powerAllowlistBackend.isDefaultActiveApp(packageName);
    }

    private static void setAppUsageStateInternal(
            @OptimizationMode int mode, int uid, String packageName, BatteryUtils batteryUtils,
            PowerAllowlistBackend powerAllowlistBackend) {
        if (mode == MODE_UNKNOWN) {
            Log.d(TAG, "set unknown app optimization mode.");
            return;
        }

        // MODE_RESTRICTED = AppOpsManager.MODE_IGNORED + !allowListed
        // MODE_UNRESTRICTED = AppOpsManager.MODE_ALLOWED + allowListed
        // MODE_OPTIMIZED = AppOpsManager.MODE_ALLOWED + !allowListed
        final int appOpsManagerMode =
                mode == MODE_RESTRICTED ? AppOpsManager.MODE_IGNORED : AppOpsManager.MODE_ALLOWED;
        final boolean allowListed = mode == MODE_UNRESTRICTED;

        setAppOptimizationModeInternal(appOpsManagerMode, allowListed, uid, packageName,
                    batteryUtils, powerAllowlistBackend);
    }

    private static void setAppOptimizationModeInternal(
            int appStandbyMode, boolean allowListed, int uid, String packageName,
            BatteryUtils batteryUtils, PowerAllowlistBackend powerAllowlistBackend) {
        try {
            batteryUtils.setForceAppStandby(uid, packageName, appStandbyMode);
            if (allowListed) {
                powerAllowlistBackend.addApp(packageName);
            } else {
                powerAllowlistBackend.removeApp(packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "set OPTIMIZATION MODE failed for " + packageName, e);
        }
    }

    private void refreshState() {
        mPowerAllowListBackend.refreshList();
        mAllowListed = mPowerAllowListBackend.isAllowlisted(mPackageName);
        mMode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mPackageName);
        Log.d(TAG, String.format("refresh %s state, allowlisted = %s, mode = %d",
                mPackageName, mAllowListed, mMode));
    }
}
