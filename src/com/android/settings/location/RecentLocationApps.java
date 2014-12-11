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

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.IPackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.InstalledAppDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves the information of applications which accessed location recently.
 */
public class RecentLocationApps {
    private static final String TAG = RecentLocationApps.class.getSimpleName();
    private static final String ANDROID_SYSTEM_PACKAGE_NAME = "android";

    private static final int RECENT_TIME_INTERVAL_MILLIS = 15 * 60 * 1000;

    private final SettingsActivity mActivity;
    private final PackageManager mPackageManager;

    public RecentLocationApps(SettingsActivity activity) {
        mActivity = activity;
        mPackageManager = activity.getPackageManager();
    }

    private class PackageEntryClickedListener
            implements Preference.OnPreferenceClickListener {
        private String mPackage;
        private UserHandle mUserHandle;

        public PackageEntryClickedListener(String packageName, UserHandle userHandle) {
            mPackage = packageName;
            mUserHandle = userHandle;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            // start new fragment to display extended information
            Bundle args = new Bundle();
            args.putString(InstalledAppDetails.ARG_PACKAGE_NAME, mPackage);
            mActivity.startPreferencePanelAsUser(InstalledAppDetails.class.getName(), args,
                    R.string.application_info_label, null, mUserHandle);
            return true;
        }
    }

    private DimmableIconPreference createRecentLocationEntry(
            Drawable icon,
            CharSequence label,
            boolean isHighBattery,
            CharSequence contentDescription,
            Preference.OnPreferenceClickListener listener) {
        DimmableIconPreference pref = new DimmableIconPreference(mActivity, contentDescription);
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
     * Fills a list of applications which queried location recently within specified time.
     */
    public List<Preference> getAppList() {
        // Retrieve a location usage list from AppOps
        AppOpsManager aoManager =
                (AppOpsManager) mActivity.getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> appOps = aoManager.getPackagesForOps(new int[] {
                AppOpsManager.OP_MONITOR_LOCATION, AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION, });

        // Process the AppOps list and generate a preference list.
        ArrayList<Preference> prefs = new ArrayList<Preference>();
        final long now = System.currentTimeMillis();
        final UserManager um = (UserManager) mActivity.getSystemService(Context.USER_SERVICE);
        final List<UserHandle> profiles = um.getUserProfiles();

        final int appOpsN = appOps.size();
        for (int i = 0; i < appOpsN; ++i) {
            AppOpsManager.PackageOps ops = appOps.get(i);
            // Don't show the Android System in the list - it's not actionable for the user.
            // Also don't show apps belonging to background users except managed users.
            String packageName = ops.getPackageName();
            int uid = ops.getUid();
            int userId = UserHandle.getUserId(uid);
            boolean isAndroidOs =
                    (uid == Process.SYSTEM_UID) && ANDROID_SYSTEM_PACKAGE_NAME.equals(packageName);
            if (isAndroidOs || !profiles.contains(new UserHandle(userId))) {
                continue;
            }
            Preference preference = getPreferenceFromOps(um, now, ops);
            if (preference != null) {
                prefs.add(preference);
            }
        }

        return prefs;
    }

    /**
     * Creates a Preference entry for the given PackageOps.
     *
     * This method examines the time interval of the PackageOps first. If the PackageOps is older
     * than the designated interval, this method ignores the PackageOps object and returns null.
     * When the PackageOps is fresh enough, this method returns a Preference pointing to the App
     * Info page for that package.
     */
    private Preference getPreferenceFromOps(final UserManager um, long now,
            AppOpsManager.PackageOps ops) {
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

        int uid = ops.getUid();
        int userId = UserHandle.getUserId(uid);

        DimmableIconPreference preference = null;
        try {
            IPackageManager ipm = AppGlobals.getPackageManager();
            ApplicationInfo appInfo =
                    ipm.getApplicationInfo(packageName, PackageManager.GET_META_DATA, userId);
            if (appInfo == null) {
                Log.w(TAG, "Null application info retrieved for package " + packageName
                        + ", userId " + userId);
                return null;
            }
            Resources res = mActivity.getResources();

            final UserHandle userHandle = new UserHandle(userId);
            Drawable appIcon = mPackageManager.getApplicationIcon(appInfo);
            Drawable icon = mPackageManager.getUserBadgedIcon(appIcon, userHandle);
            CharSequence appLabel = mPackageManager.getApplicationLabel(appInfo);
            CharSequence badgedAppLabel = mPackageManager.getUserBadgedLabel(appLabel, userHandle);
            if (appLabel.toString().contentEquals(badgedAppLabel)) {
                // If badged label is not different from original then no need for it as
                // a separate content description.
                badgedAppLabel = null;
            }
            preference = createRecentLocationEntry(icon,
                    appLabel, highBattery, badgedAppLabel,
                    new PackageEntryClickedListener(packageName, userHandle));
        } catch (RemoteException e) {
            Log.w(TAG, "Error while retrieving application info for package " + packageName
                    + ", userId " + userId, e);
        }

        return preference;
    }
}
