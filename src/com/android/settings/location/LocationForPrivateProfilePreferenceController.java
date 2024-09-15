/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

public class LocationForPrivateProfilePreferenceController
        extends LocationBasePreferenceController {
    @Nullable private RestrictedSwitchPreference mPreference;
    @Nullable private final UserHandle mPrivateProfileHandle;
    public LocationForPrivateProfilePreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
        mPrivateProfileHandle = Utils.getProfileOfType(mUserManager, ProfileType.PRIVATE);
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            final boolean switchState = mPreference.isChecked();
            mUserManager.setUserRestriction(
                    UserManager.DISALLOW_SHARE_LOCATION,
                    !switchState,
                    mPrivateProfileHandle);
            mPreference.setSummary(switchState
                    ? R.string.switch_on_text : R.string.switch_off_text);
            return true;
        }
        return false;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setEnabled(isPrivateProfileAvailable());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!android.os.Flags.allowPrivateProfile()
                || !android.multiuser.Flags.enablePrivateSpaceFeatures()
                || !android.multiuser.Flags.handleInterleavedSettingsForPrivateSpace()
                || !isPrivateProfileAvailable()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        if ((mPreference != null && !mPreference.isVisible())
                || !isAvailable()
                || !isPrivateProfileAvailable()) {
            return;
        }

        // The profile owner (which is the admin for the child profile) might have added a location
        // sharing restriction.
        final RestrictedLockUtils.EnforcedAdmin admin =
                mLocationEnabler.getShareLocationEnforcedAdmin(
                        mPrivateProfileHandle.getIdentifier());
        if (admin != null) {
            mPreference.setDisabledByAdmin(admin);
        } else {
            final boolean enabled = mLocationEnabler.isEnabled(mode);
            mPreference.setEnabled(enabled);
            int summaryResId;

            final boolean isRestrictedByBase =
                    mLocationEnabler
                            .hasShareLocationRestriction(mPrivateProfileHandle.getIdentifier());
            if (isRestrictedByBase || !enabled) {
                mPreference.setChecked(false);
                summaryResId = enabled ? R.string.switch_off_text
                        : R.string.location_app_permission_summary_location_off;
            } else {
                mPreference.setChecked(true);
                summaryResId = R.string.switch_on_text;
            }
            mPreference.setSummary(summaryResId);
        }
    }

    private boolean isPrivateProfileAvailable() {
        return mPrivateProfileHandle != null
                && !mUserManager.isQuietModeEnabled(mPrivateProfileHandle);
    }
}
