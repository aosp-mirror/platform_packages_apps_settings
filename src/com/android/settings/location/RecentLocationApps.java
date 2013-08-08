/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.location;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;

import java.util.List;

/**
 * Retrieves the information of applications which accessed location recently.
 */
public class RecentLocationApps {
    private static final String TAG = RecentLocationApps.class.getSimpleName();

    private static final int RECENT_TIME_INTERVAL_MILLIS = 15 * 60 * 1000;

    private Context mContext;
    PackageManager mPackageManager;

    public RecentLocationApps(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
    }

    /**
     * Fills a list of applications which queried location recently within
     * specified time.
     */
    public void fillAppList(PreferenceCategory container) {
        AppOpsManager aoManager =
                (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> appOps = aoManager.getPackagesForOps(
                new int[] {
                    AppOpsManager.OP_MONITOR_LOCATION,
                AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION,
                });
        PreferenceManager preferenceManager = container.getPreferenceManager();
        long now = System.currentTimeMillis();
        for (AppOpsManager.PackageOps ops : appOps) {
            processPackageOps(now, container, preferenceManager, ops);
        }
    }

    private void processPackageOps(
            long now,
            PreferenceCategory container,
            PreferenceManager preferenceManager,
            AppOpsManager.PackageOps ops) {
        String packageName = ops.getPackageName();
        List<AppOpsManager.OpEntry> entries = ops.getOps();
        boolean highBattery = false;
        boolean normalBattery = false;
        for (AppOpsManager.OpEntry entry : entries) {
            // If previous location activity is older than designated interval, ignore this app.
            if (now - entry.getTime() <= RECENT_TIME_INTERVAL_MILLIS) {
                switch (entry.getOp()) {
                    case AppOpsManager.OP_MONITOR_LOCATION:
                        normalBattery = true;
                        break;
                    case AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION:
                        highBattery = true;
                        break;
                    default:
                        break;
                }
            }
        }

        if (!highBattery && !normalBattery) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, packageName + " hadn't used location within the time interval.");
            }
            return;
        }

        try {
            PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            screen.setIcon(mPackageManager.getApplicationIcon(appInfo));
            screen.setTitle(mPackageManager.getApplicationLabel(appInfo));
            // if used both high and low battery within the time interval, show as "high
            // battery"
            if (highBattery) {
                screen.setSummary(R.string.location_high_battery_use);
            } else {
                screen.setSummary(R.string.location_low_battery_use);
            }
            container.addPreference(screen);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore the current app and move on to the next.
        }
    }
}
