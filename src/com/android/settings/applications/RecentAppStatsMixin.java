/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.Application;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RecentAppStatsMixin implements Comparator<UsageStats>, LifecycleObserver, OnStart {

    private static final String TAG = "RecentAppStatsMixin";
    private static final Set<String> SKIP_SYSTEM_PACKAGES = new ArraySet<>();

    @VisibleForTesting
    final List<UsageStats> mRecentApps;
    private final int mUserId;
    private final int mMaximumApps;
    private final Context mContext;
    private final PackageManager mPm;
    private final PowerManager mPowerManager;;
    private final UsageStatsManager mUsageStatsManager;
    private final ApplicationsState mApplicationsState;
    private final List<RecentAppStatsListener> mAppStatsListeners;
    private Calendar mCalendar;

    static {
        SKIP_SYSTEM_PACKAGES.addAll(Arrays.asList(
                "android",
                "com.android.phone",
                SETTINGS_PACKAGE_NAME,
                "com.android.systemui",
                "com.android.providers.calendar",
                "com.android.providers.media"
        ));
    }

    public RecentAppStatsMixin(Context context, int maximumApps) {
        mContext = context;
        mMaximumApps = maximumApps;
        mUserId = UserHandle.myUserId();
        mPm = mContext.getPackageManager();
        mPowerManager = mContext.getSystemService(PowerManager.class);
        mUsageStatsManager = mContext.getSystemService(UsageStatsManager.class);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) mContext.getApplicationContext());
        mRecentApps = new ArrayList<>();
        mAppStatsListeners = new ArrayList<>();
    }

    @Override
    public void onStart() {
        ThreadUtils.postOnBackgroundThread(() -> {
            loadDisplayableRecentApps(mMaximumApps);
            for (RecentAppStatsListener listener : mAppStatsListeners) {
                ThreadUtils.postOnMainThread(() -> listener.onReloadDataCompleted(mRecentApps));
            }
        });
    }

    @Override
    public final int compare(UsageStats a, UsageStats b) {
        // return by descending order
        return Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed());
    }

    public void addListener(@NonNull RecentAppStatsListener listener) {
        mAppStatsListeners.add(listener);
    }

    @VisibleForTesting
    void loadDisplayableRecentApps(int number) {
        mRecentApps.clear();
        mCalendar = Calendar.getInstance();
        mCalendar.add(Calendar.DAY_OF_YEAR, -1);
        final List<UsageStats> mStats = mPowerManager.isPowerSaveMode()
                ? new ArrayList<>()
                : mUsageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_BEST, mCalendar.getTimeInMillis(),
                        System.currentTimeMillis());

        final Map<String, UsageStats> map = new ArrayMap<>();
        final int statCount = mStats.size();
        for (int i = 0; i < statCount; i++) {
            final UsageStats pkgStats = mStats.get(i);
            if (!shouldIncludePkgInRecents(pkgStats)) {
                continue;
            }
            final String pkgName = pkgStats.getPackageName();
            final UsageStats existingStats = map.get(pkgName);
            if (existingStats == null) {
                map.put(pkgName, pkgStats);
            } else {
                existingStats.add(pkgStats);
            }
        }
        final List<UsageStats> packageStats = new ArrayList<>();
        packageStats.addAll(map.values());
        Collections.sort(packageStats, this /* comparator */);
        int count = 0;
        for (UsageStats stat : packageStats) {
            final ApplicationsState.AppEntry appEntry = mApplicationsState.getEntry(
                    stat.getPackageName(), mUserId);
            if (appEntry == null) {
                continue;
            }
            mRecentApps.add(stat);
            count++;
            if (count >= number) {
                break;
            }
        }
    }

    /**
     * Whether or not the app should be included in recent list.
     */
    private boolean shouldIncludePkgInRecents(UsageStats stat) {
        final String pkgName = stat.getPackageName();
        if (stat.getLastTimeUsed() < mCalendar.getTimeInMillis()) {
            Log.d(TAG, "Invalid timestamp (usage time is more than 24 hours ago), skipping "
                    + pkgName);
            return false;
        }

        if (SKIP_SYSTEM_PACKAGES.contains(pkgName)) {
            Log.d(TAG, "System package, skipping " + pkgName);
            return false;
        }
        if (AppUtils.isHiddenSystemModule(mContext, pkgName)) {
            return false;
        }
        final Intent launchIntent = new Intent().addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(pkgName);

        if (mPm.resolveActivity(launchIntent, 0) == null) {
            // Not visible on launcher -> likely not a user visible app, skip if non-instant.
            final ApplicationsState.AppEntry appEntry =
                    mApplicationsState.getEntry(pkgName, mUserId);
            if (appEntry == null || appEntry.info == null || !AppUtils.isInstant(appEntry.info)) {
                Log.d(TAG, "Not a user visible or instant app, skipping " + pkgName);
                return false;
            }
        }
        return true;
    }

    public interface RecentAppStatsListener {

        void onReloadDataCompleted(List<UsageStats> recentApps);
    }
}
