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

package com.android.settings.applications;

import static android.app.usage.UsageStatsManager.INTERVAL_MONTHLY;
import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.apphibernation.AppHibernationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;
import android.util.ArrayMap;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A preference controller handling the logic for updating summary of hibernated apps.
 */
public final class HibernatedAppsPreferenceController extends BasePreferenceController {
    private static final String TAG = "HibernatedAppsPrefController";
    private static final String PROPERTY_HIBERNATION_UNUSED_THRESHOLD_MILLIS =
            "auto_revoke_unused_threshold_millis2";
    private static final long DEFAULT_UNUSED_THRESHOLD_MS = TimeUnit.DAYS.toMillis(90);

    public HibernatedAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return isHibernationEnabled() && getNumHibernated() > 0
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final int numHibernated = getNumHibernated();
        return mContext.getResources().getQuantityString(
                R.plurals.unused_apps_summary, numHibernated, numHibernated);
    }

    private int getNumHibernated() {
        // TODO(b/187465752): Find a way to export this logic from PermissionController module
        final PackageManager pm = mContext.getPackageManager();
        final AppHibernationManager ahm = mContext.getSystemService(AppHibernationManager.class);
        final List<String> hibernatedPackages = ahm.getHibernatingPackagesForUser();
        int numHibernated = hibernatedPackages.size();

        // Also need to count packages that are auto revoked but not hibernated.
        final UsageStatsManager usm = mContext.getSystemService(UsageStatsManager.class);
        final long now = System.currentTimeMillis();
        final long unusedThreshold = DeviceConfig.getLong(DeviceConfig.NAMESPACE_PERMISSIONS,
                PROPERTY_HIBERNATION_UNUSED_THRESHOLD_MILLIS, DEFAULT_UNUSED_THRESHOLD_MS);
        final List<UsageStats> usageStatsList = usm.queryUsageStats(INTERVAL_MONTHLY,
                now - unusedThreshold, now);
        final Map<String, UsageStats> recentlyUsedPackages = new ArrayMap<>();
        for (UsageStats us : usageStatsList) {
            recentlyUsedPackages.put(us.mPackageName, us);
        }
        final List<PackageInfo> packages = pm.getInstalledPackages(
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.GET_PERMISSIONS);
        for (PackageInfo pi : packages) {
            final String packageName = pi.packageName;
            final UsageStats usageStats = recentlyUsedPackages.get(packageName);
            // Only count packages that have not been used recently as auto-revoked permissions may
            // stay revoked even after use if the user has not regranted them.
            final boolean usedRecently = (usageStats != null
                    && (now - usageStats.getLastTimeAnyComponentUsed() < unusedThreshold
                        || now - usageStats.getLastTimeVisible() < unusedThreshold));
            if (!hibernatedPackages.contains(packageName)
                    && pi.requestedPermissions != null
                    && !usedRecently) {
                for (String perm : pi.requestedPermissions) {
                    if ((pm.getPermissionFlags(perm, packageName, mContext.getUser())
                            & PackageManager.FLAG_PERMISSION_AUTO_REVOKED) != 0) {
                        numHibernated++;
                        break;
                    }
                }
            }
        }
        return numHibernated;
    }

    private static boolean isHibernationEnabled() {
        return DeviceConfig.getBoolean(
                NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED, false);
    }
}
