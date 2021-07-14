/*
 * Copyright 2021 The Android Open Source Project
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
package com.android.settings.location;

import static com.android.settings.location.RecentLocationAccessPreferenceController.createAppPreference;
import static com.android.settings.location.RecentLocationAccessPreferenceController.isRequestMatchesProfileType;

import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settingslib.location.RecentLocationAccesses;
import com.android.settingslib.widget.AppPreference;

import java.util.ArrayList;
import java.util.List;

/** Preference controller for preference category displaying all recent location access (apps). */
public class RecentLocationAccessSeeAllPreferenceController
        extends LocationBasePreferenceController {

    private PreferenceScreen mCategoryAllRecentLocationAccess;
    private final RecentLocationAccesses mRecentLocationAccesses;
    private boolean mShowSystem = false;
    private Preference mPreference;
    private int mType = ProfileSelectFragment.ProfileType.ALL;

    public RecentLocationAccessSeeAllPreferenceController(Context context, String key) {
        super(context, key);
        mRecentLocationAccesses = new RecentLocationAccesses(context);
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        mCategoryAllRecentLocationAccess.setEnabled(mLocationEnabler.isEnabled(mode));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryAllRecentLocationAccess = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        mCategoryAllRecentLocationAccess.removeAll();
        mPreference = preference;

        final UserManager userManager = UserManager.get(mContext);

        final List<RecentLocationAccesses.Access> recentLocationAccesses = new ArrayList<>();
        for (RecentLocationAccesses.Access access : mRecentLocationAccesses.getAppListSorted(
                mShowSystem)) {
            if (isRequestMatchesProfileType(userManager, access, mType)) {
                recentLocationAccesses.add(access);
            }
        }

        if (recentLocationAccesses.isEmpty()) {
            // If there's no item to display, add a "No recent apps" item.
            final Preference banner = new AppPreference(mContext);
            banner.setTitle(R.string.location_no_recent_apps);
            banner.setSelectable(false);
            mCategoryAllRecentLocationAccess.addPreference(banner);
        } else {
            for (RecentLocationAccesses.Access request : recentLocationAccesses) {
                final Preference appPreference = createAppPreference(
                        preference.getContext(),
                        request, mFragment);
                mCategoryAllRecentLocationAccess.addPreference(appPreference);
            }
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
     * Set the value of {@link #mShowSystem}.
     */
    public void setShowSystem(boolean showSystem) {
        mShowSystem = showSystem;
        if (mPreference != null) {
            updateState(mPreference);
        }
    }
}
