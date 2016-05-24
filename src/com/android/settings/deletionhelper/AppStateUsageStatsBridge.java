/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.deletionhelper;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Connects data from the UsageStatsManager to the ApplicationsState.
 */
public class AppStateUsageStatsBridge extends AppStateBaseBridge {
    private static final String TAG = "AppStateUsageStatsBridge";
    private UsageStatsManager mUsageStatsManager;
    private PackageManager mPm;
    public static final long NEVER_USED = -1;
    public static final long UNKNOWN_LAST_USE = -2;

    public AppStateUsageStatsBridge(Context context, ApplicationsState appState,
                                    Callback callback) {
        super(appState, callback);
        mUsageStatsManager =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        mPm = context.getPackageManager();
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<AppEntry> apps = mAppSession.getAllApps();
        if (apps == null) return;

        final Map<String, UsageStats> map = mUsageStatsManager.queryAndAggregateUsageStats(0,
                System.currentTimeMillis());
        for (AppEntry entry : apps) {
            UsageStats usageStats = map.get(entry.info.packageName);
            entry.extraInfo = new UsageStatsState(getDaysSinceLastUse(usageStats),
                    getDaysSinceInstalled(entry.info.packageName));
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        Map<String, UsageStats> map = mUsageStatsManager.queryAndAggregateUsageStats(0,
                System.currentTimeMillis());
        UsageStats usageStats = map.get(app.info.packageName);
        app.extraInfo = new UsageStatsState(getDaysSinceLastUse(usageStats),
                getDaysSinceInstalled(app.info.packageName));
    }

    private long getDaysSinceLastUse(UsageStats stats) {
        if (stats == null) {
            return NEVER_USED;
        }
        long lastUsed = stats.getLastTimeUsed();
        // Sometimes, a usage is recorded without a time and we don't know when the use was.
        if (lastUsed == 0) {
            return UNKNOWN_LAST_USE;
        }
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUsed);
    }

    private long getDaysSinceInstalled(String packageName) {
        PackageInfo pi = null;
        try {
            pi = mPm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, packageName + " was not found.");
        }

        if (pi == null) {
            return NEVER_USED;
        }

        return (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - pi.firstInstallTime));
    }

    /**
     * Filters only non-system apps which haven't been used in the last 60 days. If an app's last
     * usage is unknown, it is skipped.
     */
    public static final AppFilter FILTER_USAGE_STATS = new AppFilter() {
        private long UNUSED_DAYS_DELETION_THRESHOLD = 60;

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            if (info == null) return false;
            return isExtraInfoValid(info.extraInfo) && !isBundled(info)
                    && !isPersistentProcess(info);
        }

        private boolean isExtraInfoValid(Object extraInfo) {
            if (extraInfo == null || !(extraInfo instanceof UsageStatsState)) {
                return false;
            }

            UsageStatsState state = (UsageStatsState) extraInfo;
            long mostRecentUse = Math.max(state.daysSinceFirstInstall, state.daysSinceLastUse);
            return mostRecentUse >= UNUSED_DAYS_DELETION_THRESHOLD || mostRecentUse == NEVER_USED;
        }

        private boolean isBundled(AppEntry info) {
            return (info.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }

        private boolean isPersistentProcess(AppEntry info) {
            return (info.info.flags & ApplicationInfo.FLAG_PERSISTENT) != 0;
        }
    };

    /**
     * UsageStatsState contains the days since the last use and first install of a given app.
     */
    public static class UsageStatsState {
        public long daysSinceLastUse;
        public long daysSinceFirstInstall;

        public UsageStatsState(long daysSinceLastUse, long daysSinceFirstInstall) {
            this.daysSinceLastUse = daysSinceLastUse;
            this.daysSinceFirstInstall = daysSinceFirstInstall;
        }
    }
}
