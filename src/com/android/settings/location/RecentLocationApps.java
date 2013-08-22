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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.fuelgauge.BatterySipper;
import com.android.settings.fuelgauge.BatteryStatsHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    private Preference createRecentLocationEntry(
            Drawable icon,
            CharSequence label,
            boolean isHighBattery,
            Preference.OnPreferenceClickListener listener) {
        Preference pref = new Preference(mActivity);
        pref.setIcon(icon);
        pref.setTitle(label);
        if (isHighBattery) {
            pref.setSummary(R.string.location_high_battery_use);
        } else {
            pref.setSummary(R.string.location_low_battery_use);
        }
        pref.setOnPreferenceClickListener(listener);
        return pref;
    }

    /**
     * Stores a BatterySipper object and records whether the sipper has been used.
     */
    private static final class BatterySipperWrapper {
        private BatterySipper mSipper;
        private boolean mUsed;

        public BatterySipperWrapper(BatterySipper sipper) {
            mSipper = sipper;
            mUsed = false;
        }

        public BatterySipper batterySipper() {
            return mSipper;
        }

        public boolean used() {
            return mUsed;
        }

        public void setUsed() {
            mUsed = true;
        }
    }

    /**
     * Fills a list of applications which queried location recently within
     * specified time.
     */
    public List<Preference> getAppList() {
        // Retrieve Uid-based battery blaming info and generate a package to BatterySipper HashMap
        // for later faster looking up.
        mStatsHelper.refreshStats();
        List<BatterySipper> usageList = mStatsHelper.getUsageList();
        // Key: package Uid. Value: BatterySipperWrapper.
        HashMap<Integer, BatterySipperWrapper> sipperMap =
                new HashMap<Integer, BatterySipperWrapper>(usageList.size());
        for (BatterySipper sipper: usageList) {
            int uid = sipper.getUid();
            if (uid != 0) {
                sipperMap.put(uid, new BatterySipperWrapper(sipper));
            }
        }

        // Retrieve a location usage list from AppOps
        AppOpsManager aoManager =
                (AppOpsManager) mActivity.getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> appOps = aoManager.getPackagesForOps(
                new int[] {
                    AppOpsManager.OP_MONITOR_LOCATION,
                    AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION,
                });

        // Process the AppOps list and generate a preference list.
        ArrayList<Preference> prefs = new ArrayList<Preference>();
        long now = System.currentTimeMillis();
        for (AppOpsManager.PackageOps ops : appOps) {
            BatterySipperWrapper wrapper = sipperMap.get(ops.getUid());
            Preference pref = getPreferenceFromOps(now, ops, wrapper);
            if (pref != null) {
                prefs.add(pref);
            }
        }

        return prefs;
    }

    /**
     * Creates a Preference entry for the given PackageOps.
     *
     * This method examines the time interval of the PackageOps first. If the PackageOps is older
     * than the designated interval, this method ignores the PackageOps object and returns null.
     *
     * When the PackageOps is fresh enough, if the package has a corresponding battery blaming entry
     * in the Uid-based battery sipper list, this method returns a Preference pointing to the Uid
     * battery blaming page. If the package doesn't have a battery sipper entry (typically shouldn't
     * happen), this method returns a Preference pointing to the App Info page for that package.
     */
    private Preference getPreferenceFromOps(
            long now,
            AppOpsManager.PackageOps ops,
            BatterySipperWrapper wrapper) {
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
            return null;
        }

        // The package is fresh enough, continue.

        Preference pref = null;
        if (wrapper != null) {
            // Contains sipper. Link to Battery Blaming page.

            // We're listing by UID rather than package. Check whether the entry has been used
            // before to prevent the same UID from showing up twice.
            if (!wrapper.used()) {
                BatterySipper sipper = wrapper.batterySipper();
                sipper.loadNameAndIcon();
                pref = createRecentLocationEntry(
                        sipper.getIcon(),
                        sipper.getLabel(),
                        highBattery,
                        new UidEntryClickedListener(sipper));
                wrapper.setUsed();
            }
        } else {
            // No corresponding sipper. Link to App Info page.

            // This is grouped by package rather than UID, but that's OK because this branch
            // shouldn't happen in practice.
            try {
                ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                        packageName, PackageManager.GET_META_DATA);
                pref = createRecentLocationEntry(
                        mPackageManager.getApplicationIcon(appInfo),
                        mPackageManager.getApplicationLabel(appInfo),
                        highBattery,
                        new PackageEntryClickedListener(packageName));
            } catch (PackageManager.NameNotFoundException e) {
                Log.wtf(TAG, "Package not found: " + packageName);
            }
        }

        return pref;
    }
}
