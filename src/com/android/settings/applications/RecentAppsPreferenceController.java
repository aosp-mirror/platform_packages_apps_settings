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

import android.app.Application;
import android.app.usage.UsageStats;
import android.content.Context;
import android.icu.text.RelativeDateTimeFormatter;
import android.os.UserHandle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.AppEntitiesHeaderController;
import com.android.settingslib.widget.AppEntityInfo;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

/**
 * This controller displays up to three recently used apps.
 * If there is no recently used app, we only show up an "App Info" preference.
 */
public class RecentAppsPreferenceController extends BasePreferenceController
        implements RecentAppStatsMixin.RecentAppStatsListener {

    @VisibleForTesting
    static final String KEY_DIVIDER = "recent_apps_divider";

    @VisibleForTesting
    AppEntitiesHeaderController mAppEntitiesController;
    @VisibleForTesting
    LayoutPreference mRecentAppsPreference;
    @VisibleForTesting
    Preference mDivider;

    private final ApplicationsState mApplicationsState;
    private final int mUserId;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private Fragment mHost;
    private List<UsageStats> mRecentApps;

    public RecentAppsPreferenceController(Context context, String key) {
        super(context, key);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) mContext.getApplicationContext());
        mUserId = UserHandle.myUserId();
        mMetricsFeatureProvider = FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();
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

        mDivider = screen.findPreference(KEY_DIVIDER);
        mRecentAppsPreference = screen.findPreference(getPreferenceKey());
        final View view = mRecentAppsPreference.findViewById(R.id.app_entities_header);
        mAppEntitiesController = AppEntitiesHeaderController.newInstance(mContext, view)
                .setHeaderTitleRes(R.string.recent_app_category_title)
                .setHeaderDetailsClickListener((View v) -> {
                    mMetricsFeatureProvider.logClickedPreference(mRecentAppsPreference,
                            getMetricsCategory());
                    new SubSettingLauncher(mContext)
                            .setDestination(ManageApplications.class.getName())
                            .setArguments(null /* arguments */)
                            .setTitleRes(R.string.application_info_label)
                            .setSourceMetricsCategory(getMetricsCategory())
                            .launch();
                });
    }

    @Override
    public void onReloadDataCompleted(@NonNull List<UsageStats> recentApps) {
        mRecentApps = recentApps;
        refreshUi();
        // Show total number of installed apps as See all's summary.
        new InstalledAppCounter(mContext, InstalledAppCounter.IGNORE_INSTALL_REASON,
                mContext.getPackageManager()) {
            @Override
            protected void onCountComplete(int num) {
                mAppEntitiesController.setHeaderDetails(
                        mContext.getResources().getQuantityString(R.plurals.see_all_apps_title,
                                num, num));
                mAppEntitiesController.apply();
            }
        }.execute();
    }

    private void refreshUi() {
        if (!mRecentApps.isEmpty()) {
            displayRecentApps();
            mRecentAppsPreference.setVisible(true);
            mDivider.setVisible(true);
        } else {
            mDivider.setVisible(false);
            mRecentAppsPreference.setVisible(false);
        }
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
    }

    private AppEntityInfo createAppEntity(UsageStats stat) {
        final String pkgName = stat.getPackageName();
        final ApplicationsState.AppEntry appEntry =
                mApplicationsState.getEntry(pkgName, mUserId);
        if (appEntry == null) {
            return null;
        }

        return new AppEntityInfo.Builder()
                .setIcon(Utils.getBadgedIcon(mContext, appEntry.info))
                .setTitle(appEntry.label)
                .setSummary(StringUtil.formatRelativeTime(mContext,
                        System.currentTimeMillis() - stat.getLastTimeUsed(), false,
                        RelativeDateTimeFormatter.Style.SHORT))
                .setOnClickListener(v -> {
                    mMetricsFeatureProvider.logClickedPreference(mRecentAppsPreference,
                            getMetricsCategory());
                    AppInfoBase.startAppInfoFragment(AppInfoDashboardFragment.class,
                            R.string.application_info_label, pkgName, appEntry.info.uid,
                            mHost, 1001 /*RequestCode*/, getMetricsCategory());
                })
                .build();
    }
}
