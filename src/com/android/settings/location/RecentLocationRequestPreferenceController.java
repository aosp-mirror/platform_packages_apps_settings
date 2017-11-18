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
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.util.FeatureFlagUtils;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.AppInfoDashboardFragment;
import com.android.settings.core.FeatureFlags;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;

import java.util.ArrayList;
import java.util.List;

public class RecentLocationRequestPreferenceController extends LocationBasePreferenceController {

    /** Key for preference category "Recent location requests" */
    private static final String KEY_RECENT_LOCATION_REQUESTS = "recent_location_requests";
    private final LocationSettings mFragment;
    private final RecentLocationApps mRecentLocationApps;
    private PreferenceCategory mCategoryRecentLocationRequests;

    @VisibleForTesting
    static class PackageEntryClickedListener implements Preference.OnPreferenceClickListener {
        private final LocationSettings mFragment;
        private final String mPackage;
        private final UserHandle mUserHandle;

        public PackageEntryClickedListener(LocationSettings fragment, String packageName,
                UserHandle userHandle) {
            mFragment = fragment;
            mPackage = packageName;
            mUserHandle = userHandle;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            // start new fragment to display extended information
            final Bundle args = new Bundle();
            if (FeatureFlagUtils.isEnabled(mFragment.getActivity(), FeatureFlags.APP_INFO_V2)) {
                args.putString(AppInfoDashboardFragment.ARG_PACKAGE_NAME, mPackage);
                ((SettingsActivity) mFragment.getActivity()).startPreferencePanelAsUser(
                        mFragment,
                        AppInfoDashboardFragment.class.getName(), args,
                        R.string.application_info_label, null, mUserHandle);
            } else {
                args.putString(InstalledAppDetails.ARG_PACKAGE_NAME, mPackage);
                ((SettingsActivity) mFragment.getActivity()).startPreferencePanelAsUser(
                        mFragment,
                        InstalledAppDetails.class.getName(), args,
                        R.string.application_info_label, null, mUserHandle);
            }
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
                mRecentLocationApps.getAppList();

        final List<Preference> recentLocationPrefs = new ArrayList<>(recentLocationRequests.size());
        for (final RecentLocationApps.Request request : recentLocationRequests) {
            recentLocationPrefs.add(createAppPreference(prefContext, request));
        }
        if (recentLocationRequests.size() > 0) {
            LocationSettings.addPreferencesSorted(
                    recentLocationPrefs, mCategoryRecentLocationRequests);
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
        final AppPreference pref =  createAppPreference(prefContext);
        pref.setSummary(request.contentDescription);
        pref.setIcon(request.icon);
        pref.setTitle(request.label);
        pref.setOnPreferenceClickListener(new PackageEntryClickedListener(
                mFragment, request.packageName, request.userHandle));
        return pref;
    }
}
