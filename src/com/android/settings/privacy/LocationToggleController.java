/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.privacy;

import android.content.Context;
import android.os.UserHandle;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.location.LocationEnabler;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Controller for location toggle
 */
public class LocationToggleController extends TogglePreferenceController
        implements LocationEnabler.LocationModeChangeListener {

    private final LocationEnabler mLocationEnabler;
    private RestrictedSwitchPreference mPreference;

    private boolean mIsLocationEnabled = true;

    public LocationToggleController(Context context, String preferenceKey, Lifecycle lifecycle) {
        super(context, preferenceKey);
        mLocationEnabler = new LocationEnabler(context, this, lifecycle);
        mLocationEnabler.refreshLocationMode();
    }
    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        if (mPreference == null) {
            return;
        }

        mIsLocationEnabled = mLocationEnabler.isEnabled(mode);
        final int userId = UserHandle.myUserId();
        final RestrictedLockUtils.EnforcedAdmin admin =
                mLocationEnabler.getShareLocationEnforcedAdmin(userId);
        final boolean hasBaseUserRestriction =
                mLocationEnabler.hasShareLocationRestriction(userId);
        // Disable the whole switch bar instead of the switch itself. If we disabled the switch
        // only, it would be re-enabled again if the switch bar is not disabled.
        if (!hasBaseUserRestriction && admin != null) {
            mPreference.setDisabledByAdmin(admin);
        } else {
            mPreference.setEnabled(!restricted);
        }
        updateState(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mIsLocationEnabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mLocationEnabler.setLocationEnabled(isChecked);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mLocationEnabler.refreshLocationMode();
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
