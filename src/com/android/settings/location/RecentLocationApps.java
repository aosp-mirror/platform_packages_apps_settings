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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.fuelgauge.BatterySipper;
import com.android.settings.fuelgauge.BatteryStatsHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieves the information of applications which accessed location recently.
 */
public class RecentLocationApps {
    private static final String TAG = RecentLocationApps.class.getSimpleName();

    private static final int RECENT_TIME_INTERVAL_MILLIS = 15 * 60 * 1000;

    private final PreferenceActivity mActivity;
    private final BatteryStatsHelper mStatsHelper;
    private final PackageManager mPackageManager;

    // Stores all the packages that requested location within the designated interval
    // key - package name of the app
    // value - whether the app has requested high power location

    public RecentLocationApps(PreferenceActivity activity, BatteryStatsHelper sipperUtil) {
        mActivity = activity;
        mPackageManager = activity.getPackageManager();
        mStatsHelper = sipperUtil;
    }

    private class UidEntryClickedListener
            implements Preference.OnPreferenceClickListener {
        private BatterySipper mSipper;

        public UidEntryClickedListener(BatterySipper sipper) {
            mSipper = sipper;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            mStatsHelper.startBatteryDetailPage(mActivity, mSipper, false);
            return true;
        }
    }

    private class PackageEntryClickedListener
            implements Preference.OnPreferenceClickListener {
        private String mPackage;

        public PackageEntryClickedListener(String packageName) {
            mPackage = packageName;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            // start new fragment to display extended information
            Bundle args = new Bundle();
            args.putString(InstalledAppDetails.ARG_PACKAGE_NAME, mPackage);
            mActivity.startPreferencePanel(InstalledAppDetails.class.getName(), args,
                    R.string.application_info_label, null, null, 0);
            return true;
        }
    }

    /**
     * Fills a list of applications which queried location recently within
     * specified time.
     */
    public void fillAppList(PreferenceCategory container) {
        HashMap<String, Boolean> packageMap = new HashMap<String, Boolean>();
        AppOpsManager aoManager =
                (AppOpsManager) mActivity.getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> appOps = aoManager.getPackagesForOps(
                new int[] {
                    AppOpsManager.OP_MONITOR_LOCATION,
                    AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION,
                });
        PreferenceManager preferenceManager = container.getPreferenceManager();
        long now = System.currentTimeMillis();
        for (AppOpsManager.PackageOps ops : appOps) {
            processPackageOps(now, container, preferenceManager, ops, packageMap);
        }

        mStatsHelper.refreshStats();
        List<BatterySipper> usageList = mStatsHelper.getUsageList();
        for (BatterySipper sipper : usageList) {
            sipper.loadNameAndIcon();
            String[] packages = sipper.getPackages();
            if (packages == null) {
                continue;
            }
            for (String curPackage : packages) {
                if (packageMap.containsKey(curPackage)) {
                    PreferenceScreen screen = preferenceManager.createPreferenceScreen(mActivity);
                    screen.setIcon(sipper.getIcon());
                    screen.setTitle(sipper.getLabel());
                    if (packageMap.get(curPackage)) {
                        screen.setSummary(R.string.location_high_battery_use);
                    } else {
                        screen.setSummary(R.string.location_low_battery_use);
                    }
                    container.addPreference(screen);
                    screen.setOnPreferenceClickListener(new UidEntryClickedListener(sipper));
                    packageMap.remove(curPackage);
                    break;
                }
            }
        }

        // Typically there shouldn't be any entry left in the HashMap. But if there are any, add
        // them to the list and link them to the app info page.
        for (Map.Entry<String, Boolean> entry : packageMap.entrySet()) {
            try {
                PreferenceScreen screen = preferenceManager.createPreferenceScreen(mActivity);
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                        entry.getKey(), PackageManager.GET_META_DATA);
                screen.setIcon(mPackageManager.getApplicationIcon(appInfo));
                screen.setTitle(mPackageManager.getApplicationLabel(appInfo));
                // if used both high and low battery within the time interval, show as "high
                // battery"
                if (entry.getValue()) {
                    screen.setSummary(R.string.location_high_battery_use);
                } else {
                    screen.setSummary(R.string.location_low_battery_use);
                }
                screen.setOnPreferenceClickListener(
                        new PackageEntryClickedListener(entry.getKey()));
                container.addPreference(screen);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore the current app and move on to the next.
            }
        }
    }

    private void processPackageOps(
            long now,
            PreferenceCategory container,
            PreferenceManager preferenceManager,
            AppOpsManager.PackageOps ops,
            HashMap<String, Boolean> packageMap) {
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

        packageMap.put(packageName, highBattery);
    }
}
