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
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settingslib.applications.RecentAppOpsAccess;
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
    RecentAppOpsAccess mRecentLocationApps;
    private PreferenceCategory mCategoryRecentLocationRequests;
    private int mType = ProfileSelectFragment.ProfileType.ALL;
    private boolean mShowSystem = true;
    private boolean mSystemSettingChanged = false;

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
        this(context, key, RecentAppOpsAccess.createForLocation(context));
    }

    @VisibleForTesting
    public RecentLocationAccessPreferenceController(Context context, String key,
            RecentAppOpsAccess recentLocationApps) {
        super(context, key);
        mRecentLocationApps = recentLocationApps;
        mShowSystem = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                SystemUiDeviceConfigFlags.PROPERTY_LOCATION_INDICATORS_SMALL_ENABLED, true)
                ? Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_SHOW_SYSTEM_OPS, 1) == 1
                : false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryRecentLocationRequests = screen.findPreference(getPreferenceKey());
        mLocationEnabler.refreshLocationMode();
        loadRecentAccesses();
    }

    @Override
    public void updateState(Preference preference) {
        // Only reload the recent accesses in updateState if the system setting has changed.
        if (mSystemSettingChanged) {
            loadRecentAccesses();
            mSystemSettingChanged = false;
        }
    }

    private void loadRecentAccesses() {
        mCategoryRecentLocationRequests.removeAll();
        final Context prefContext = mCategoryRecentLocationRequests.getContext();
        final List<RecentAppOpsAccess.Access> recentLocationAccesses = new ArrayList<>();
        final UserManager userManager = UserManager.get(mContext);
        for (RecentAppOpsAccess.Access access : mRecentLocationApps.getAppListSorted(mShowSystem)) {
            if (isRequestMatchesProfileType(userManager, access, mType)) {
                recentLocationAccesses.add(access);
                if (recentLocationAccesses.size() == MAX_APPS) {
                    break;
                }
            }
        }

        if (recentLocationAccesses.size() > 0) {
            // Add preferences to container in original order (already sorted by recency).
            for (RecentAppOpsAccess.Access access : recentLocationAccesses) {
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
     * Clears the list of apps which recently accessed location from the screen.
     */
    public void clearPreferenceList() {
        if (mCategoryRecentLocationRequests != null) {
            mCategoryRecentLocationRequests.removeAll();
        }
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
            RecentAppOpsAccess.Access access, DashboardFragment fragment) {
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
     * Return if the {@link RecentAppOpsAccess.Access} matches current UI
     * {@link ProfileSelectFragment.ProfileType}
     */
    public static boolean isRequestMatchesProfileType(UserManager userManager,
            RecentAppOpsAccess.Access access, @ProfileSelectFragment.ProfileType int type) {

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

    /**
     * Update the state of the showSystem setting flag and load the new results.
     */
    void updateShowSystem() {
        mSystemSettingChanged = true;
        mShowSystem = !mShowSystem;
        clearPreferenceList();
        loadRecentAccesses();
    }
}
