/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;

/**
 * Location Setting page for managed profile.
 */
public class LocationWorkProfileSettings extends DashboardFragment {

    private static final String TAG = "LocationWorkProfile";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.LOCATION_WORK;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_settings_workprofile;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        use(AppLocationPermissionPreferenceController.class).init(this);
        use(LocationServiceForWorkPreferenceController.class).init(this);
        use(LocationFooterPreferenceController.class).init(this);
        use(LocationForWorkPreferenceController.class).init(this);

        final int profileType = getArguments().getInt(ProfileSelectFragment.EXTRA_PROFILE);
        final RecentLocationRequestPreferenceController controller = use(
                RecentLocationRequestPreferenceController.class);
        controller.init(this);
        controller.setProfileType(profileType);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }
}
