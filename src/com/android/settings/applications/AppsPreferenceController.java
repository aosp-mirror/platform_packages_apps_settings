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

import android.app.Application;
import android.app.usage.UsageStats;
import android.content.Context;
import android.icu.text.RelativeDateTimeFormatter;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.AppPreference;

import java.util.List;
import java.util.Map;

/**
 * This controller displays up to four recently used apps.
 * If there is no recently used app, we only show up an "App Info" preference.
 */
public class AppsPreferenceController extends BasePreferenceController implements
        LifecycleObserver {

    public static final int SHOW_RECENT_APP_COUNT = 4;

    @VisibleForTesting
    static final String KEY_RECENT_APPS_CATEGORY = "recent_apps_category";
    @VisibleForTesting
    static final String KEY_GENERAL_CATEGORY = "general_category";
    @VisibleForTesting
    static final String KEY_ALL_APP_INFO = "all_app_infos";
    @VisibleForTesting
    static final String KEY_SEE_ALL = "see_all_apps";

    private final ApplicationsState mApplicationsState;

    @VisibleForTesting
    List<RecentAppStatsMixin.UsageStatsWrapper> mRecentApps;
    @VisibleForTesting
    PreferenceCategory mRecentAppsCategory;
    @VisibleForTesting
    PreferenceCategory mGeneralCategory;
    @VisibleForTesting
    Preference mAllAppsInfoPref;
    @VisibleForTesting
    Preference mSeeAllPref;

    private Fragment mHost;
    private boolean mInitialLaunch = false;

    public AppsPreferenceController(Context context) {
        super(context, KEY_RECENT_APPS_CATEGORY);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) mContext.getApplicationContext());
    }

    public void setFragment(Fragment fragment) {
        mHost = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        initPreferences(screen);
        refreshUi();
        mInitialLaunch = true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!mInitialLaunch) {
            refreshUi();
        }
    }

    /**
     * Called when the apps page pauses.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mInitialLaunch = false;
    }

    @VisibleForTesting
    void refreshUi() {
        loadAllAppsCount();
        mRecentApps = loadRecentApps();
        if (!mRecentApps.isEmpty()) {
            displayRecentApps();
            mAllAppsInfoPref.setVisible(false);
            mRecentAppsCategory.setVisible(true);
            mGeneralCategory.setVisible(true);
            mSeeAllPref.setVisible(true);
        } else {
            mAllAppsInfoPref.setVisible(true);
            mRecentAppsCategory.setVisible(false);
            mGeneralCategory.setVisible(false);
            mSeeAllPref.setVisible(false);
        }
    }

    @VisibleForTesting
    void loadAllAppsCount() {
        // Show total number of installed apps as See all's summary.
        new InstalledAppCounter(mContext, InstalledAppCounter.IGNORE_INSTALL_REASON,
                mContext.getPackageManager()) {
            @Override
            protected void onCountComplete(int num) {
                if (!mRecentApps.isEmpty()) {
                    mSeeAllPref.setTitle(StringUtil.getIcuPluralsString(mContext, num,
                            R.string.see_all_apps_title));
                } else {
                    mAllAppsInfoPref.setSummary(mContext.getString(R.string.apps_summary, num));
                }
            }
        }.execute();
    }

    @VisibleForTesting
    List<RecentAppStatsMixin.UsageStatsWrapper> loadRecentApps() {
        final RecentAppStatsMixin recentAppStatsMixin = new RecentAppStatsMixin(mContext,
                SHOW_RECENT_APP_COUNT);
        recentAppStatsMixin.loadDisplayableRecentApps(SHOW_RECENT_APP_COUNT);
        return recentAppStatsMixin.mRecentApps;
    }

    private void initPreferences(PreferenceScreen screen) {
        mRecentAppsCategory = screen.findPreference(KEY_RECENT_APPS_CATEGORY);
        mGeneralCategory = screen.findPreference(KEY_GENERAL_CATEGORY);
        mAllAppsInfoPref = screen.findPreference(KEY_ALL_APP_INFO);
        mSeeAllPref = screen.findPreference(KEY_SEE_ALL);
        mRecentAppsCategory.setVisible(false);
        mGeneralCategory.setVisible(false);
        mAllAppsInfoPref.setVisible(false);
        mSeeAllPref.setVisible(false);
    }

    private void displayRecentApps() {
        if (mRecentAppsCategory != null) {
            final Map<String, Preference> existedAppPreferences = new ArrayMap<>();
            final int prefCount = mRecentAppsCategory.getPreferenceCount();
            for (int i = 0; i < prefCount; i++) {
                final Preference pref = mRecentAppsCategory.getPreference(i);
                final String key = pref.getKey();
                if (!TextUtils.equals(key, KEY_SEE_ALL)) {
                    existedAppPreferences.put(key, pref);
                }
            }

            int showAppsCount = 0;
            for (RecentAppStatsMixin.UsageStatsWrapper statsWrapper : mRecentApps) {
                final UsageStats stats = statsWrapper.mUsageStats;
                final String pkgName = statsWrapper.mUsageStats.getPackageName();
                final String key = pkgName + statsWrapper.mUserId;
                final ApplicationsState.AppEntry appEntry =
                        mApplicationsState.getEntry(pkgName, statsWrapper.mUserId);
                if (appEntry == null) {
                    continue;
                }

                boolean rebindPref = true;
                Preference pref = existedAppPreferences.remove(key);
                if (pref == null) {
                    pref = new AppPreference(mContext);
                    rebindPref = false;
                }

                pref.setKey(key);
                pref.setTitle(appEntry.label);
                pref.setIcon(Utils.getBadgedIcon(mContext, appEntry.info));
                pref.setSummary(StringUtil.formatRelativeTime(mContext,
                        System.currentTimeMillis() - stats.getLastTimeUsed(), false,
                        RelativeDateTimeFormatter.Style.SHORT));
                pref.setOrder(showAppsCount++);
                pref.setOnPreferenceClickListener(preference -> {
                    AppInfoBase.startAppInfoFragment(AppInfoDashboardFragment.class,
                            mContext.getString(R.string.application_info_label),
                            pkgName, appEntry.info.uid,
                            mHost, 1001 /*RequestCode*/, getMetricsCategory());
                    return true;
                });

                if (!rebindPref) {
                    mRecentAppsCategory.addPreference(pref);
                }
            }

            // Remove unused preferences from pref category.
            for (Preference unusedPref : existedAppPreferences.values()) {
                mRecentAppsCategory.removePreference(unusedPref);
            }
        }
    }
}
