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

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
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
    private static final String ANDROID_SYSTEM_PACKAGE_NAME = "android";
    private static final String GOOGLE_SERVICES_SHARED_UID = "com.google.uid.shared";
    private static final String GCORE_PACKAGE_NAME = "com.google.android.gms";

    private static final int RECENT_TIME_INTERVAL_MILLIS = 15 * 60 * 1000;

    private final PreferenceActivity mActivity;
    private final BatteryStatsHelper mStatsHelper;
    private final PackageManager mPackageManager;
    private final Drawable mGCoreIcon;

    // Stores all the packages that requested location within the designated interval
    // key - package name of the app
    // value - whether the app has requested high power location

    public RecentLocationApps(PreferenceActivity activity, BatteryStatsHelper sipperUtil) {
        mActivity = activity;
        mPackageManager = activity.getPackageManager();
        mStatsHelper = sipperUtil;
        Drawable icon = null;
        try {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                    GCORE_PACKAGE_NAME, PackageManager.GET_META_DATA);
            icon = mPackageManager.getApplicationIcon(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "GCore not installed");
            }
            icon = null;
        }
        mGCoreIcon = icon;
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
        mStatsHelper.refreshStats(true);
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
            // Don't show the Android System in the list - it's not actionable for the user.
            // Also don't show apps belonging to background users.
            int uid = ops.getUid();
            boolean isAndroidOs = (uid == Process.SYSTEM_UID)
                    && ANDROID_SYSTEM_PACKAGE_NAME.equals(ops.getPackageName());
            if (!isAndroidOs && ActivityManager.getCurrentUser() == UserHandle.getUserId(uid)) {
                BatterySipperWrapper wrapper = sipperMap.get(uid);
                Preference pref = getPreferenceFromOps(now, ops, wrapper);
                if (pref != null) {
                    prefs.add(pref);
                }
            }
        }

        return prefs;
    }

    /**
     * Retrieves the icon for given BatterySipper object.
     *
     * The icons on location blaming page are actually Uid-based rather than package based. For
     * those packages that share the same Uid, BatteryStatsHelper picks the one with the most CPU
     * usage. Both "Contact Sync" and GCore belong to "Google Services" and they share the same Uid.
     * As a result, sometimes Contact icon may be chosen to represent "Google Services" by
     * BatteryStatsHelper.
     *
     * In order to avoid displaying Contact icon for "Google Services", we hack this method to
     * always return Puzzle icon for all packages that share the Uid of "Google Services".
     */
    private Drawable getIcon(BatterySipper sipper, AppOpsManager.PackageOps ops) {
        Drawable icon = null;
        if (mGCoreIcon != null) {
            try {
                PackageInfo info = mPackageManager.getPackageInfo(
                        ops.getPackageName(), PackageManager.GET_META_DATA);
                if (info != null && GOOGLE_SERVICES_SHARED_UID.equals(info.sharedUserId)) {
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "shareUserId matches GCore, force using puzzle icon");
                    }
                    icon = mGCoreIcon;
                }
            } catch (PackageManager.NameNotFoundException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, e.toString());
                }
            }
        }
        if (icon == null) {
            icon = sipper.getIcon();
        }
        return icon;
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
        // Earliest time for a location request to end and still be shown in list.
        long recentLocationCutoffTime = now - RECENT_TIME_INTERVAL_MILLIS;
        for (AppOpsManager.OpEntry entry : entries) {
            if (entry.isRunning() || entry.getTime() >= recentLocationCutoffTime) {
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
                        getIcon(sipper, ops),
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
                // Multiple users can install the same package. Each user gets a different Uid for
                // the same package.
                //
                // Here we retrieve the Uid with package name, that will be the Uid for that package
                // associated with the current active user. If the Uid differs from the Uid in ops,
                // that means this entry belongs to another inactive user and we should ignore that.
                if (appInfo.uid == ops.getUid()) {
                    pref = createRecentLocationEntry(
                            mPackageManager.getApplicationIcon(appInfo),
                            mPackageManager.getApplicationLabel(appInfo),
                            highBattery,
                            new PackageEntryClickedListener(packageName));
                } else if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "package " + packageName + " with Uid " + ops.getUid() +
                            " belongs to another inactive account, ignored.");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.wtf(TAG, "Package not found: " + packageName, e);
            }
        }

        return pref;
    }
}
