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

import static android.Manifest.permission_group.LOCATION;

import android.content.Context;
import android.content.Intent;
import android.icu.text.RelativeDateTimeFormatter;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settingslib.location.RecentLocationAccesses;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.AppPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller that handles the display of apps that access locations.
 */
public class RecentLocationAccessPreferenceController extends LocationBasePreferenceController {
    public static final int MAX_APPS = 3;
    @VisibleForTesting
    RecentLocationAccesses mRecentLocationApps;
    private PreferenceCategory mCategoryRecentLocationRequests;
    private int mType = ProfileSelectFragment.ProfileType.ALL;

    private static class PackageEntryClickedListener implements
            Preference.OnPreferenceClickListener {
        private final Context mContext;
        private final String mPackage;
        private final UserHandle mUserHandle;

        PackageEntryClickedListener(Context context, String packageName,
                UserHandle userHandle) {
            mContext = context;
            mPackage = packageName;
            mUserHandle = userHandle;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSION);
            intent.putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, LOCATION);
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mPackage);
            intent.putExtra(Intent.EXTRA_USER, mUserHandle);
            mContext.startActivity(intent);
            return true;
        }
    }

    public RecentLocationAccessPreferenceController(Context context, String key) {
        this(context, key, new RecentLocationAccesses(context));
    }

    @VisibleForTesting
    public RecentLocationAccessPreferenceController(Context context, String key,
            RecentLocationAccesses recentLocationApps) {
        super(context, key);
        mRecentLocationApps = recentLocationApps;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryRecentLocationRequests = screen.findPreference(getPreferenceKey());
        final Context prefContext = mCategoryRecentLocationRequests.getContext();
        final List<RecentLocationAccesses.Access> recentLocationAccesses = new ArrayList<>();
        final UserManager userManager = UserManager.get(mContext);
        for (RecentLocationAccesses.Access access : mRecentLocationApps.getAppListSorted(
                /* showSystemApps= */ false)) {
            if (isRequestMatchesProfileType(userManager, access, mType)) {
                recentLocationAccesses.add(access);
                if (recentLocationAccesses.size() == MAX_APPS) {
                    break;
                }
            }
        }

        if (recentLocationAccesses.size() > 0) {
            // Add preferences to container in original order (already sorted by recency).
            for (RecentLocationAccesses.Access access : recentLocationAccesses) {
                mCategoryRecentLocationRequests.addPreference(
                        createAppPreference(prefContext, access, mFragment));
            }
        } else {
            // If there's no item to display, add a "No recent apps" item.
            final Preference banner = new AppPreference(prefContext);
            banner.setTitle(R.string.location_no_recent_accesses);
            banner.setSelectable(false);
            mCategoryRecentLocationRequests.addPreference(banner);
        }
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        boolean enabled = mLocationEnabler.isEnabled(mode);
        mCategoryRecentLocationRequests.setVisible(enabled);
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
            RecentLocationAccesses.Access access, DashboardFragment fragment) {
        final AppPreference pref = new AppPreference(prefContext);
        pref.setIcon(access.icon);
        pref.setTitle(access.label);
        pref.setSummary(StringUtil.formatRelativeTime(prefContext,
                System.currentTimeMillis() - access.accessFinishTime, false,
                RelativeDateTimeFormatter.Style.SHORT));
        pref.setOnPreferenceClickListener(new PackageEntryClickedListener(
                fragment.getContext(), access.packageName, access.userHandle));
        return pref;
    }

    /**
     * Return if the {@link RecentLocationAccesses.Access} matches current UI
     * {@ProfileSelectFragment.ProfileType}
     */
    public static boolean isRequestMatchesProfileType(UserManager userManager,
            RecentLocationAccesses.Access access, @ProfileSelectFragment.ProfileType int type) {

        final boolean isWorkProfile = userManager.isManagedProfile(
                access.userHandle.getIdentifier());
        if (isWorkProfile && (type & ProfileSelectFragment.ProfileType.WORK) != 0) {
            return true;
        }
        if (!isWorkProfile && (type & ProfileSelectFragment.ProfileType.PERSONAL) != 0) {
            return true;
        }
        return false;
    }
}
