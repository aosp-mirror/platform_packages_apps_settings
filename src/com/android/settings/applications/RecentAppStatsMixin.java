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
import static com.android.settings.Utils.SYSTEMUI_PACKAGE_NAME;

import android.app.Application;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A helper class that loads recent app data in the background and sends it in a callback to a
 * listener.
 */
public class RecentAppStatsMixin implements LifecycleObserver, OnStart {

    private static final String TAG = "RecentAppStatsMixin";
    private static final Set<String> SKIP_SYSTEM_PACKAGES = new ArraySet<>();

    @VisibleForTesting
    List<UsageStatsWrapper> mRecentApps;

    private final int mMaximumApps;
    private final Context mContext;
    private final PackageManager mPm;
    private final UserManager mUserManager;
    private final PowerManager mPowerManager;
    private final ApplicationsState mApplicationsState;
    private final List<RecentAppStatsListener> mAppStatsListeners;
    private Calendar mCalendar;

    static {
        SKIP_SYSTEM_PACKAGES.addAll(Arrays.asList(
                "android",
                "com.android.phone",
                SETTINGS_PACKAGE_NAME,
                SYSTEMUI_PACKAGE_NAME,
                "com.android.providers.calendar",
                "com.android.providers.media"
        ));
    }

    public RecentAppStatsMixin(Context context, int maximumApps) {
        mContext = context;
        mMaximumApps = maximumApps;
        mPm = mContext.getPackageManager();
        mPowerManager = mContext.getSystemService(PowerManager.class);
        mUserManager = mContext.getSystemService(UserManager.class);
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

    public void addListener(@NonNull RecentAppStatsListener listener) {
        mAppStatsListeners.add(listener);
    }

    @VisibleForTesting
    void loadDisplayableRecentApps(int limit) {
        mRecentApps.clear();
        mCalendar = Calendar.getInstance();
        mCalendar.add(Calendar.DAY_OF_YEAR, -1);

        List<UsageStatsWrapper> usageStatsAllUsers = new ArrayList<>();

        List<UserHandle> profiles = mUserManager.getUserProfiles();
        for (UserHandle userHandle : profiles) {
            int userId = userHandle.getIdentifier();

            final Optional<UsageStatsManager> usageStatsManager;
            if (userHandle.getIdentifier() == UserHandle.myUserId()) {
                usageStatsManager = Optional.ofNullable(userHandle).map(
                        handle -> mContext.getSystemService(UsageStatsManager.class));
            } else {
                usageStatsManager = Optional.ofNullable(userHandle).map(
                        handle -> mContext.createContextAsUser(handle, /* flags */ 0)
                                .getSystemService(UsageStatsManager.class));
            }

            List<UsageStats> profileStats = usageStatsManager
                    .map(statsManager -> getRecentAppsStats(statsManager, userId))
                    .orElse(new ArrayList<>());
            usageStatsAllUsers.addAll(profileStats.stream()
                        .map(usageStats-> new UsageStatsWrapper(usageStats, userId))
                        .collect(Collectors.toList()));
        }

        // Sort apps by latest timestamp.
        usageStatsAllUsers.sort(
                Comparator.comparingLong(a -> -1 * a.mUsageStats.getLastTimeUsed()));
        mRecentApps.addAll(usageStatsAllUsers.stream().limit(limit).collect(Collectors.toList()));
    }

    private List<UsageStats> getRecentAppsStats(UsageStatsManager usageStatsManager, int userId) {
        final List<UsageStats> recentAppStats = mPowerManager.isPowerSaveMode()
                ? new ArrayList<>()
                : usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_BEST, mCalendar.getTimeInMillis(),
                        System.currentTimeMillis());

        final Map<String, UsageStats> map = new ArrayMap<>();
        for (final UsageStats pkgStats : recentAppStats) {
            if (!shouldIncludePkgInRecents(pkgStats, userId)) {
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
        final List<UsageStats> packageStats = new ArrayList<>(map.values());
        packageStats.sort(Comparator.comparingLong(UsageStats::getLastTimeUsed).reversed());
        return packageStats;
    }

    /**
     * Whether or not the app should be included in recent list.
     */
    private boolean shouldIncludePkgInRecents(UsageStats stat, int userId) {
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

        final ApplicationsState.AppEntry appEntry = mApplicationsState.getEntry(pkgName, userId);
        if (appEntry == null) {
            return false;
        }

        final Intent launchIntent = new Intent().addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(pkgName);
        if (mPm.resolveActivityAsUser(launchIntent, 0, userId) == null) {
            // Not visible on launcher -> likely not a user visible app, skip if non-instant.
            if (appEntry.info == null || !AppUtils.isInstant(appEntry.info)) {
                Log.d(TAG, "Not a user visible or instant app, skipping " + pkgName);
                return false;
            }
        }

        return true;
    }

    public interface RecentAppStatsListener {

        /** A callback after loading the recent app data. */
        void onReloadDataCompleted(List<UsageStatsWrapper> recentApps);
    }

    static class UsageStatsWrapper {

        public final UsageStats mUsageStats;
        public final int mUserId;

        UsageStatsWrapper(UsageStats usageStats, int userId) {
            mUsageStats = usageStats;
            mUserId = userId;
        }

        @Override
        public String toString() {
            return String.format("UsageStatsWrapper(pkg:%s,uid:%s)",
                    mUsageStats.getPackageName(), mUserId);
        }
    }
}
