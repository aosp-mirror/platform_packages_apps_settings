/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.location;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import com.android.settingslib.widget.apppreference.AppPreference;

import java.util.List;

public class RecentLocationRequestPreferenceController extends LocationBasePreferenceController {
    /** Key for preference category "Recent location requests" */
    private static final String KEY_RECENT_LOCATION_REQUESTS = "recent_location_requests";
    @VisibleForTesting
    static final String KEY_SEE_ALL_BUTTON = "recent_location_requests_see_all_button";
    private final LocationSettings mFragment;
    private final RecentLocationApps mRecentLocationApps;
    private PreferenceCategory mCategoryRecentLocationRequests;

    /** Used in this class and {@link RecentLocationRequestSeeAllPreferenceController} */
    static class PackageEntryClickedListener implements Preference.OnPreferenceClickListener {
        private final DashboardFragment mFragment;
        private final String mPackage;
        private final UserHandle mUserHandle;

        public PackageEntryClickedListener(DashboardFragment fragment, String packageName,
                UserHandle userHandle) {
            mFragment = fragment;
            mPackage = packageName;
            mUserHandle = userHandle;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            // start new fragment to display extended information
            final Bundle args = new Bundle();
            args.putString(AppInfoDashboardFragment.ARG_PACKAGE_NAME, mPackage);
            new SubSettingLauncher(mFragment.getContext())
                    .setDestination(AppInfoDashboardFragment.class.getName())
                    .setArguments(args)
                    .setTitleRes(R.string.application_info_label)
                    .setUserHandle(mUserHandle)
                    .setSourceMetricsCategory(mFragment.getMetricsCategory())
                    .launch();
            return true;
        }
    }

    public RecentLocationRequestPreferenceController(Context context, LocationSettings fragment,
            Lifecycle lifecycle) {
        this(context, fragment, lifecycle, new RecentLocationApps(context));
    }

    @VisibleForTesting
    RecentLocationRequestPreferenceController(Context context, LocationSettings fragment,
            Lifecycle lifecycle, RecentLocationApps recentApps) {
        super(context, lifecycle);
        mFragment = fragment;
        mRecentLocationApps = recentApps;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RECENT_LOCATION_REQUESTS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryRecentLocationRequests =
                (PreferenceCategory) screen.findPreference(KEY_RECENT_LOCATION_REQUESTS);
    }

    @Override
    public void updateState(Preference preference) {
        mCategoryRecentLocationRequests.removeAll();
        final Context prefContext = preference.getContext();
        final List<RecentLocationApps.Request> recentLocationRequests =
                mRecentLocationApps.getAppListSorted(false);
        if (recentLocationRequests.size() > 3) {
            // Display the top 3 preferences to container in original order.
            for (int i = 0; i < 3; i++) {
                mCategoryRecentLocationRequests.addPreference(
                        createAppPreference(prefContext, recentLocationRequests.get(i)));
            }
        } else if (recentLocationRequests.size() > 0) {
            // Add preferences to container in original order (already sorted by recency).
            for (RecentLocationApps.Request request : recentLocationRequests) {
                mCategoryRecentLocationRequests.addPreference(
                        createAppPreference(prefContext, request));
            }
        } else {
            // If there's no item to display, add a "No recent apps" item.
            final Preference banner = createAppPreference(prefContext);
            banner.setTitle(R.string.location_no_recent_apps);
            banner.setSelectable(false);
            mCategoryRecentLocationRequests.addPreference(banner);
        }
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        mCategoryRecentLocationRequests.setEnabled(mLocationEnabler.isEnabled(mode));
    }

    @VisibleForTesting
    AppPreference createAppPreference(Context prefContext) {
        return new AppPreference(prefContext);
    }

    @VisibleForTesting
    AppPreference createAppPreference(Context prefContext, RecentLocationApps.Request request) {
        final AppPreference pref = createAppPreference(prefContext);
        pref.setSummary(request.contentDescription);
        pref.setIcon(request.icon);
        pref.setTitle(request.label);
        pref.setOnPreferenceClickListener(new PackageEntryClickedListener(
                mFragment, request.packageName, request.userHandle));
        return pref;
    }
}
