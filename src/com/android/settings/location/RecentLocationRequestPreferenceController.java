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
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;
import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settingslib.location.RecentLocationApps;
import com.android.settingslib.widget.AppPreference;

import java.util.ArrayList;
import java.util.List;

public class RecentLocationRequestPreferenceController extends LocationBasePreferenceController {

    public static final int MAX_APPS = 3;
    @VisibleForTesting
    RecentLocationApps mRecentLocationApps;
    private PreferenceCategory mCategoryRecentLocationRequests;
    private int mType = ProfileSelectFragment.ProfileType.ALL;

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

    public RecentLocationRequestPreferenceController(Context context, String key) {
        super(context, key);
        mRecentLocationApps = new RecentLocationApps(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryRecentLocationRequests = screen.findPreference(getPreferenceKey());
        final Context prefContext = mCategoryRecentLocationRequests.getContext();
        final List<RecentLocationApps.Request> recentLocationRequests = new ArrayList<>();
        final UserManager userManager = UserManager.get(mContext);
        final boolean showSystem = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                SystemUiDeviceConfigFlags.PROPERTY_LOCATION_INDICATORS_SMALL_ENABLED, true)
                ? Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_SHOW_SYSTEM_OPS, 1) == 1
                : false;

        for (RecentLocationApps.Request request : mRecentLocationApps.getAppListSorted(
                showSystem)) {
            if (isRequestMatchesProfileType(userManager, request, mType)) {
                recentLocationRequests.add(request);
                if (recentLocationRequests.size() == MAX_APPS) {
                    break;
                }
            }
        }

        if (recentLocationRequests.size() > 0) {
            // Add preferences to container in original order (already sorted by recency).
            for (RecentLocationApps.Request request : recentLocationRequests) {
                mCategoryRecentLocationRequests.addPreference(
                        createAppPreference(prefContext, request, mFragment));
            }
        } else {
            // If there's no item to display, add a "No recent apps" item.
            final Preference banner = new AppPreference(prefContext);
            banner.setTitle(R.string.location_no_recent_apps);
            banner.setSelectable(false);
            mCategoryRecentLocationRequests.addPreference(banner);
        }
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        mCategoryRecentLocationRequests.setEnabled(mLocationEnabler.isEnabled(mode));
    }

    /**
     * Initialize {@link ProfileSelectFragment.ProfileType} of the controller
     *
     * @param type {@link ProfileSelectFragment.ProfileType} of the controller.
     */
    public void setProfileType(@ProfileSelectFragment.ProfileType int type) {
        mType = type;
    }

    /**
     * Create a {@link AppPreference}
     */
    public static AppPreference createAppPreference(Context prefContext,
            RecentLocationApps.Request request, DashboardFragment fragment) {
        final AppPreference pref = new AppPreference(prefContext);
        pref.setIcon(request.icon);
        pref.setTitle(request.label);
        pref.setOnPreferenceClickListener(new PackageEntryClickedListener(
                fragment, request.packageName, request.userHandle));
        return pref;
    }

    /**
     * Return if the {@link RecentLocationApps.Request} matches current UI
     * {@ProfileSelectFragment.ProfileType}
     */
    public static boolean isRequestMatchesProfileType(UserManager userManager,
            RecentLocationApps.Request request, @ProfileSelectFragment.ProfileType int type) {
        final boolean isWorkProfile = userManager.isManagedProfile(
                request.userHandle.getIdentifier());
        if (isWorkProfile && (type & ProfileSelectFragment.ProfileType.WORK) != 0) {
            return true;
        }
        if (!isWorkProfile && (type & ProfileSelectFragment.ProfileType.PERSONAL) != 0) {
            return true;
        }
        return false;
    }
}
