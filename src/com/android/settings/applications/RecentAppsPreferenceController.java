/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.app.settings.SettingsEnums;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.AppEntitiesHeaderController;
import com.android.settingslib.widget.AppEntityInfo;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This controller displays up to three recently used apps.
 * If there is no recently used app, we only show up an "App Info" preference.
 */
public class RecentAppsPreferenceController extends BasePreferenceController
        implements Comparator<UsageStats> {

    @VisibleForTesting
    static final String KEY_DIVIDER = "recent_apps_divider";

    private static final String TAG = "RecentAppsCtrl";
    private static final Set<String> SKIP_SYSTEM_PACKAGES = new ArraySet<>();

    @VisibleForTesting
    AppEntitiesHeaderController mAppEntitiesController;
    @VisibleForTesting
    LayoutPreference mRecentAppsPreference;
    @VisibleForTesting
    Preference mDivider;

    private final PackageManager mPm;
    private final UsageStatsManager mUsageStatsManager;
    private final ApplicationsState mApplicationsState;
    private final int mUserId;
    private final IconDrawableFactory mIconDrawableFactory;
    private final PowerManager mPowerManager;

    private Fragment mHost;
    private Calendar mCal;
    private List<UsageStats> mStats;
    private List<UsageStats> mRecentApps;
    private boolean mHasRecentApps;

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

    public RecentAppsPreferenceController(Context context, String key) {
        super(context, key);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) mContext.getApplicationContext());
        mUserId = UserHandle.myUserId();
        mPm = mContext.getPackageManager();
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
        mPowerManager = mContext.getSystemService(PowerManager.class);
        mUsageStatsManager = mContext.getSystemService(UsageStatsManager.class);
        mRecentApps = new ArrayList<>();
        reloadData();
    }

    public void setFragment(Fragment fragment) {
        mHost = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return mRecentApps.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mDivider = screen.findPreference(KEY_DIVIDER);
        mRecentAppsPreference = (LayoutPreference) screen.findPreference(getPreferenceKey());
        final View view = mRecentAppsPreference.findViewById(R.id.app_entities_header);
        mAppEntitiesController = AppEntitiesHeaderController.newInstance(mContext, view)
                .setHeaderTitleRes(R.string.recent_app_category_title)
                .setHeaderDetailsClickListener((View v) -> {
                    new SubSettingLauncher(mContext)
                            .setDestination(ManageApplications.class.getName())
                            .setArguments(null /* arguments */)
                            .setTitleRes(R.string.application_info_label)
                            .setSourceMetricsCategory(SettingsEnums.SETTINGS_APP_NOTIF_CATEGORY)
                            .launch();
                });

        refreshUi();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        refreshUi();
        // Show total number of installed apps as See all's summary.
        new InstalledAppCounter(mContext, InstalledAppCounter.IGNORE_INSTALL_REASON,
                mContext.getPackageManager()) {
            @Override
            protected void onCountComplete(int num) {
                mAppEntitiesController.setHeaderDetails(
                        mContext.getString(R.string.see_all_apps_title, num));
                mAppEntitiesController.apply();
            }
        }.execute();
    }

    @Override
    public final int compare(UsageStats a, UsageStats b) {
        // return by descending order
        return Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed());
    }

    List<UsageStats> getRecentApps() {
        return mRecentApps;
    }

    @VisibleForTesting
    void refreshUi() {
        if (mRecentApps != null && !mRecentApps.isEmpty()) {
            displayRecentApps();
        } else {
            mDivider.setVisible(false);
        }
    }

    @VisibleForTesting
    void reloadData() {
        mCal = Calendar.getInstance();
        mCal.add(Calendar.DAY_OF_YEAR, -1);
        mStats = mPowerManager.isPowerSaveMode()
                ? new ArrayList<>()
                : mUsageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_BEST, mCal.getTimeInMillis(),
                        System.currentTimeMillis());

        updateDisplayableRecentAppList();
    }

    private void displayRecentApps() {
        int showAppsCount = 0;

        for (UsageStats stat : mRecentApps) {
            final AppEntityInfo appEntityInfoInfo = createAppEntity(stat);
            if (appEntityInfoInfo != null) {
                mAppEntitiesController.setAppEntity(showAppsCount++, appEntityInfoInfo);
            }

            if (showAppsCount == AppEntitiesHeaderController.MAXIMUM_APPS) {
                break;
            }
        }
        mAppEntitiesController.apply();
        mDivider.setVisible(true);
    }

    private AppEntityInfo createAppEntity(UsageStats stat) {
        final String pkgName = stat.getPackageName();
        final ApplicationsState.AppEntry appEntry =
                mApplicationsState.getEntry(pkgName, mUserId);
        if (appEntry == null) {
            return null;
        }

        return new AppEntityInfo.Builder()
                .setIcon(mIconDrawableFactory.getBadgedIcon(appEntry.info))
                .setTitle(appEntry.label)
                .setSummary(StringUtil.formatRelativeTime(mContext,
                        System.currentTimeMillis() - stat.getLastTimeUsed(), false))
                .setOnClickListener(v ->
                        AppInfoBase.startAppInfoFragment(AppInfoDashboardFragment.class,
                                R.string.application_info_label, pkgName, appEntry.info.uid,
                                mHost, 1001 /*RequestCode*/,
                                SettingsEnums.SETTINGS_APP_NOTIF_CATEGORY))
                .build();
    }

    private void updateDisplayableRecentAppList() {
        mRecentApps.clear();
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
            if (count >= AppEntitiesHeaderController.MAXIMUM_APPS) {
                break;
            }
        }
    }


    /**
     * Whether or not the app should be included in recent list.
     */
    private boolean shouldIncludePkgInRecents(UsageStats stat) {
        final String pkgName = stat.getPackageName();
        if (stat.getLastTimeUsed() < mCal.getTimeInMillis()) {
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
}
