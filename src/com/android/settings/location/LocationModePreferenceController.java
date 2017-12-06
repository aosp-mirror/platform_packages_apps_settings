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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class LocationModePreferenceController extends LocationBasePreferenceController {

    /** Key for preference screen "Mode" */
    private static final String KEY_LOCATION_MODE = "location_mode";

    private final LocationSettings mParentFragment;
    private Preference mPreference;

    public LocationModePreferenceController(Context context, LocationSettings parent,
            Lifecycle lifecycle) {
        super(context, lifecycle);
        mParentFragment = parent;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCATION_MODE;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_location_mode_available);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_LOCATION_MODE);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_LOCATION_MODE.equals(preference.getKey())) {
            final SettingsActivity activity = (SettingsActivity) mParentFragment.getActivity();
            activity.startPreferencePanel(mParentFragment, LocationMode.class.getName(), null,
                    R.string.location_mode_screen_title, null, mParentFragment, 0);
            return true;
        }
        return false;
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        final int modeDescription = LocationPreferenceController.getLocationString(mode);
        if (modeDescription != 0) {
            mPreference.setSummary(modeDescription);
        }
        // Restricted user can't change the location mode, so disable the master switch. But in some
        // corner cases, the location might still be enabled. In such case the master switch should
        // be disabled but checked.
        mPreference.setEnabled(mLocationEnabler.isEnabled(mode));
    }
}
