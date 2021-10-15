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

    /** Sets the {@link OptimizationMode} for associated app. */
    public void setAppOptimizationMode(@OptimizationMode int mode) {
        if (getAppOptimizationMode(mMode, mAllowListed) == mode) {
            Log.w(TAG, "set the same optimization mode for: " + mPackageName);
            return;
        }
        switch (mode) {
            case MODE_RESTRICTED:
                mBatteryUtils.setForceAppStandby(mUid, mPackageName, AppOpsManager.MODE_IGNORED);
                mPowerAllowListBackend.removeApp(mPackageName);
                break;
            case MODE_UNRESTRICTED:
                mBatteryUtils.setForceAppStandby(mUid, mPackageName, AppOpsManager.MODE_ALLOWED);
                mPowerAllowListBackend.addApp(mPackageName);
                break;
            case MODE_OPTIMIZED:
                mBatteryUtils.setForceAppStandby(mUid, mPackageName, AppOpsManager.MODE_ALLOWED);
                mPowerAllowListBackend.removeApp(mPackageName);
                break;
            default:
                Log.d(TAG, "set unknown app optimization mode.");
        }
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

        return mPowerAllowListBackend.isSysAllowlisted(mPackageName)
                || mPowerAllowListBackend.isDefaultActiveApp(mPackageName);
    }

    String getPackageName() {
        return mPackageName == null ? UNKNOWN_PACKAGE : mPackageName;
    }

    private void refreshState() {
        mPowerAllowListBackend.refreshList();
        mAllowListed = mPowerAllowListBackend.isAllowlisted(mPackageName);
        mMode = mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, mUid, mPackageName);
        Log.d(TAG, String.format("refresh %s state, allowlisted = %s, mode = %d",
                mPackageName,
                mAllowListed,
                mMode));
    }
}
