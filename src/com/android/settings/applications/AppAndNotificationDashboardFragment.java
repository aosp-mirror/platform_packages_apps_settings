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

package com.android.settings.applications;

import android.app.settings.SettingsEnums;
import android.app.usage.UsageStats;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.EmergencyBroadcastPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.AppEntitiesHeaderController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class AppAndNotificationDashboardFragment extends DashboardFragment
        implements RecentAppStatsMixin.RecentAppStatsListener {

    private static final String TAG = "AppAndNotifDashboard";

    private RecentAppStatsMixin mRecentAppStatsMixin;
    private RecentAppsPreferenceController mRecentAppsPreferenceController;
    private AllAppsInfoPreferenceController mAllAppsInfoPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_APP_NOTIF_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_apps_and_notifications;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_and_notification;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        use(SpecialAppAccessPreferenceController.class).setSession(getSettingsLifecycle());

        mRecentAppStatsMixin = new RecentAppStatsMixin(context,
                AppEntitiesHeaderController.MAXIMUM_APPS);
        getSettingsLifecycle().addObserver(mRecentAppStatsMixin);
        mRecentAppStatsMixin.addListener(this);

        mRecentAppsPreferenceController = use(RecentAppsPreferenceController.class);
        mRecentAppsPreferenceController.setFragment(this /* fragment */);
        mRecentAppStatsMixin.addListener(mRecentAppsPreferenceController);

        mAllAppsInfoPreferenceController = use(AllAppsInfoPreferenceController.class);
        mRecentAppStatsMixin.addListener(mAllAppsInfoPreferenceController);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setPinnedHeaderView(R.layout.progress_header);
        showPinnedHeader(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        showPinnedHeader(true);
    }

    @Override
    public void onReloadDataCompleted(@NonNull List<UsageStats> recentApps) {
        showPinnedHeader(false);
        if (!recentApps.isEmpty()) {
            Utils.setActionBarShadowAnimation(getActivity(), getSettingsLifecycle(),
                    getListView());
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new EmergencyBroadcastPreferenceController(context,
                "app_and_notif_cell_broadcast_settings"));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.app_and_notification;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }
            };
}
