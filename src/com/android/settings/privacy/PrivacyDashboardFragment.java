/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.privacy;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.view.View;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class PrivacyDashboardFragment extends DashboardFragment {
    private static final String TAG = "PrivacyDashboardFrag";
    private static final String KEY_LOCK_SCREEN_NOTIFICATIONS = "privacy_lock_screen_notifications";
    private static final String KEY_WORK_PROFILE_CATEGORY =
            "privacy_work_profile_notifications_category";
    private static final String KEY_NOTIFICATION_WORK_PROFILE_NOTIFICATIONS =
            "privacy_lock_screen_work_profile_notifications";

    @VisibleForTesting
    View mProgressHeader;
    @VisibleForTesting
    View mProgressAnimation;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TOP_LEVEL_PRIVACY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.privacy_dashboard_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_privacy_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(PermissionBarChartPreferenceController.class).setFragment(this /* fragment */);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Utils.setActionBarShadowAnimation(getActivity(), getSettingsLifecycle(), getListView());
        initLoadingBar();
    }

    @VisibleForTesting
    void initLoadingBar() {
        mProgressHeader = setPinnedHeaderView(R.layout.progress_header);
        mProgressAnimation = mProgressHeader.findViewById(R.id.progress_bar_animation);
        setLoadingEnabled(false);
    }

    @VisibleForTesting
    void setLoadingEnabled(boolean enabled) {
        if (mProgressHeader != null && mProgressAnimation != null) {
            mProgressHeader.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
            mProgressAnimation.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final LockScreenNotificationPreferenceController notificationController =
                new LockScreenNotificationPreferenceController(context,
                        KEY_LOCK_SCREEN_NOTIFICATIONS,
                        KEY_WORK_PROFILE_CATEGORY,
                        KEY_NOTIFICATION_WORK_PROFILE_NOTIFICATIONS);
        if (lifecycle != null) {
            lifecycle.addObserver(notificationController);
        }
        controllers.add(notificationController);

        return controllers;

    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.privacy_dashboard_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}
