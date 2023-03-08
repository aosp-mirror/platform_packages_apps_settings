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

package com.android.settings.applications;

import android.annotation.IntDef;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class for bridging the app battery usage information to ApplicationState.
 */
public class AppStateAppBatteryUsageBridge extends AppStateBaseBridge {
    private static final String TAG = AppStateAppBatteryUsageBridge.class.getSimpleName();
    static final boolean DEBUG = Build.IS_DEBUGGABLE;

    @VisibleForTesting
    Context mContext;
    @VisibleForTesting
    AppOpsManager mAppOpsManager;
    @VisibleForTesting
    PowerAllowlistBackend mPowerAllowlistBackend;

    @VisibleForTesting
    static final int MODE_UNKNOWN = 0;
    @VisibleForTesting
    static final int MODE_UNRESTRICTED = 1;
    @VisibleForTesting
    static final int MODE_OPTIMIZED = 2;
    @VisibleForTesting
    static final int MODE_RESTRICTED = 3;

    @IntDef(
            prefix = {"MODE_"},
            value = {
                    MODE_UNKNOWN,
                    MODE_RESTRICTED,
                    MODE_UNRESTRICTED,
                    MODE_OPTIMIZED,
            })
    @Retention(RetentionPolicy.SOURCE)
    @interface OptimizationMode {
    }

    public AppStateAppBatteryUsageBridge(
            Context context, ApplicationsState appState, Callback callback) {
        super(appState, callback);
        mContext = context;
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mPowerAllowlistBackend = PowerAllowlistBackend.getInstance(mContext);
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = getAppBatteryUsageState(pkg, uid);
    }

    @Override
    protected void loadAllExtraInfo() {
        if (DEBUG) {
            Log.d(TAG, "Start loadAllExtraInfo()");
        }
        mAppSession.getAllApps().stream().forEach(appEntry ->
                updateExtraInfo(appEntry, appEntry.info.packageName, appEntry.info.uid));
        if (DEBUG) {
            Log.d(TAG, "End loadAllExtraInfo()");
        }
    }

    protected Object getAppBatteryUsageState(String pkg, int uid) {
        // Restricted = AppOpsManager.MODE_IGNORED + !allowListed
        // Unrestricted = AppOpsManager.MODE_ALLOWED + allowListed
        // Optimized = AppOpsManager.MODE_ALLOWED + !allowListed

        boolean allowListed = mPowerAllowlistBackend.isAllowlisted(pkg, uid);
        int aomMode =
                mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, uid, pkg);
        @OptimizationMode int mode = MODE_UNKNOWN;
        String modeName = "";
        if (aomMode == AppOpsManager.MODE_IGNORED && !allowListed) {
            mode = MODE_RESTRICTED;
            if (DEBUG) {
                modeName = "RESTRICTED";
            }
        } else if (aomMode == AppOpsManager.MODE_ALLOWED) {
            mode = allowListed ? MODE_UNRESTRICTED : MODE_OPTIMIZED;
            if (DEBUG) {
                modeName = allowListed ? "UNRESTRICTED" : "OPTIMIZED";
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Pkg: " + pkg + ", mode: " + modeName);
        }
        return new AppBatteryUsageDetails(mode);
    }

    @VisibleForTesting
    @OptimizationMode
    static int getAppBatteryUsageDetailsMode(AppEntry entry) {
        if (entry == null || entry.extraInfo == null) {
            return MODE_UNKNOWN;
        }

        return entry.extraInfo instanceof AppBatteryUsageDetails
                ? ((AppBatteryUsageDetails) entry.extraInfo).mMode
                : MODE_UNKNOWN;
    }

    /**
     * Used by {@link com.android.settings.applications.manageapplications.AppFilterRegistry} to
     * determine which apps are unrestricted.
     */
    public static final AppFilter FILTER_BATTERY_UNRESTRICTED_APPS =
            new AppFilter() {
                @Override
                public void init() {}

                @Override
                public boolean filterApp(AppEntry info) {
                    return getAppBatteryUsageDetailsMode(info) == MODE_UNRESTRICTED;
                }
            };

    /**
     * Used by {@link com.android.settings.applications.manageapplications.AppFilterRegistry} to
     * determine which apps are optimized.
     */
    public static final AppFilter FILTER_BATTERY_OPTIMIZED_APPS =
            new AppFilter() {
                @Override
                public void init() {}

                @Override
                public boolean filterApp(AppEntry info) {
                    return getAppBatteryUsageDetailsMode(info) == MODE_OPTIMIZED;
                }
            };

    /**
     * Used by {@link com.android.settings.applications.manageapplications.AppFilterRegistry} to
     * determine which apps are restricted.
     */
    public static final AppFilter FILTER_BATTERY_RESTRICTED_APPS =
            new AppFilter() {
                @Override
                public void init() {}

                @Override
                public boolean filterApp(AppEntry info) {
                    return getAppBatteryUsageDetailsMode(info) == MODE_RESTRICTED;
                }
            };

    /**
     * Extra details for app battery usage data.
     */
    static final class AppBatteryUsageDetails {
        @OptimizationMode
        int mMode;

        AppBatteryUsageDetails(@OptimizationMode int mode) {
            mMode = mode;
        }
    }
}
