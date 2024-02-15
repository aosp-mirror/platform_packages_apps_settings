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

package com.android.settings.dashboard.profileselector;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.location.LocationPersonalSettings;
import com.android.settings.location.LocationSwitchBarController;
import com.android.settings.location.LocationWorkProfileSettings;
import com.android.settings.widget.SettingsMainSwitchBar;

/**
 * Location Setting page for personal/managed profile.
 */
public class ProfileSelectLocationFragment extends ProfileSelectFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
        switchBar.setTitle(getContext().getString(R.string.location_settings_primary_switch_title));
        final LocationSwitchBarController switchBarController = new LocationSwitchBarController(
                activity, switchBar, getSettingsLifecycle());
        switchBar.show();
    }

    @Override
    public Fragment[] getFragments() {
        return ProfileSelectFragment.getFragments(
                getContext(),
                null /* bundle */,
                LocationPersonalSettings::new,
                LocationWorkProfileSettings::new,
                LocationPersonalSettings::new);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_settings_header;
    }
}
