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

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

/**
 * Preference controller that handles the "See All" button for recent location access.
 */
public class RecentLocationAccessSeeAllButtonPreferenceController extends
        LocationBasePreferenceController {

    private Preference mPreference;

    /**
     * Constructor of {@link RecentLocationAccessSeeAllButtonPreferenceController}.
     */
    public RecentLocationAccessSeeAllButtonPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mLocationEnabler.refreshLocationMode();
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        boolean enabled = mLocationEnabler.isEnabled(mode);
        mPreference.setVisible(enabled);
    }
}
