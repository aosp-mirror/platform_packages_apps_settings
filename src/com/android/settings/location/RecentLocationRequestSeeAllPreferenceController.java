/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.location;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import com.android.settingslib.widget.apppreference.AppPreference;

import java.util.List;

import com.android.settings.R;

/** Preference controller for preference category displaying all recent location requests. */
public class RecentLocationRequestSeeAllPreferenceController
        extends LocationBasePreferenceController {
    /** Key for preference category "All recent location requests" */
    private static final String KEY_ALL_RECENT_LOCATION_REQUESTS = "all_recent_location_requests";
    private final RecentLocationRequestSeeAllFragment mFragment;
    private PreferenceCategory mCategoryAllRecentLocationRequests;
    private RecentLocationApps mRecentLocationApps;
    private boolean mShowSystem = false;
    private Preference mPreference;

    public RecentLocationRequestSeeAllPreferenceController(
            Context context, Lifecycle lifecycle, RecentLocationRequestSeeAllFragment fragment) {
        this(context, lifecycle, fragment, new RecentLocationApps(context));
    }

    @VisibleForTesting
    RecentLocationRequestSeeAllPreferenceController(
            Context context,
            Lifecycle lifecycle,
            RecentLocationRequestSeeAllFragment fragment,
            RecentLocationApps recentLocationApps) {
        super(context, lifecycle);
        mFragment = fragment;
        mRecentLocationApps = recentLocationApps;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALL_RECENT_LOCATION_REQUESTS;
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        mCategoryAllRecentLocationRequests.setEnabled(mLocationEnabler.isEnabled(mode));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryAllRecentLocationRequests =
                (PreferenceCategory) screen.findPreference(KEY_ALL_RECENT_LOCATION_REQUESTS);
    }

    @Override
    public void updateState(Preference preference) {
        mCategoryAllRecentLocationRequests.removeAll();
        mPreference = preference;
        List<RecentLocationApps.Request> requests = mRecentLocationApps.getAppListSorted(
                mShowSystem);
        if (requests.isEmpty()) {
            // If there's no item to display, add a "No recent apps" item.
            final Preference banner = new AppPreference(mContext);
            banner.setTitle(R.string.location_no_recent_apps);
            banner.setSelectable(false);
            mCategoryAllRecentLocationRequests.addPreference(banner);
        } else {
            for (RecentLocationApps.Request request : requests) {
                Preference appPreference = createAppPreference(preference.getContext(), request);
                mCategoryAllRecentLocationRequests.addPreference(appPreference);
            }
        }
    }

    @VisibleForTesting
    AppPreference createAppPreference(
            Context prefContext, RecentLocationApps.Request request) {
        final AppPreference pref = new AppPreference(prefContext);
        pref.setSummary(request.contentDescription);
        pref.setIcon(request.icon);
        pref.setTitle(request.label);
        pref.setOnPreferenceClickListener(
                new RecentLocationRequestPreferenceController.PackageEntryClickedListener(
                        mFragment, request.packageName, request.userHandle));
        return pref;
    }

    public void setShowSystem(boolean showSystem) {
        mShowSystem = showSystem;
        if (mPreference != null) {
            updateState(mPreference);
        }
    }
}
