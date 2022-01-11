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

import android.app.AppOpsManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

/** A utility class for application usage operation. */
public class BatteryOptimizeUtils {
    private static final String TAG = "BatteryOptimizeUtils";
    private static final String UNKNOWN_PACKAGE = "unknown";

    @VisibleForTesting AppOpsManager mAppOpsManager;
    @VisibleForTesting BatteryUtils mBatteryUtils;
    @VisibleForTesting PowerAllowlistBackend mPowerAllowListBackend;
    private final String mPackageName;
    private final int mUid;

    private int mMode;
    private boolean mAllowListed;

    /**
     * Usage type of application.
     */
    public enum AppUsageState {
        UNKNOWN,
        RESTRICTED,
        UNRESTRICTED,
        OPTIMIZED,
    }

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

    public AppUsageState getAppUsageState() {
        refreshState();
        if (!mAllowListed && mMode == AppOpsManager.MODE_IGNORED) {
            return AppUsageState.RESTRICTED;
        } else if (mAllowListed && mMode == AppOpsManager.MODE_ALLOWED) {
            return AppUsageState.UNRESTRICTED;
        } else if (!mAllowListed && mMode == AppOpsManager.MODE_ALLOWED) {
            return AppUsageState.OPTIMIZED;
        } else {
            Log.d(TAG, "get unknown app usage state.");
            return AppUsageState.UNKNOWN;
        }
    }

    public void setAppUsageState(AppUsageState state) {
        try {
            setAppUsageStateInternal(state);
        } catch (Exception e) {
            Log.e(TAG, "setAppUsageState() is failed for " + mPackageName, e);
        }
    }

    private void setAppUsageStateInternal(AppUsageState state) {
        switch (state) {
            case RESTRICTED:
                mBatteryUtils.setForceAppStandby(mUid, mPackageName, AppOpsManager.MODE_IGNORED);
                mPowerAllowListBackend.removeApp(mPackageName);
                break;
            case UNRESTRICTED:
                mBatteryUtils.setForceAppStandby(mUid, mPackageName, AppOpsManager.MODE_ALLOWED);
                mPowerAllowListBackend.addApp(mPackageName);
                break;
            case OPTIMIZED:
                mBatteryUtils.setForceAppStandby(mUid, mPackageName, AppOpsManager.MODE_ALLOWED);
                mPowerAllowListBackend.removeApp(mPackageName);
                break;
            default:
                Log.d(TAG, "set unknown app usage state.");
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
