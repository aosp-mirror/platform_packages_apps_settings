/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

public class LocationForWorkPreferenceController extends LocationBasePreferenceController {

    private RestrictedSwitchPreference mPreference;

    public LocationForWorkPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            final boolean switchState = mPreference.isChecked();
            mUserManager.setUserRestriction(UserManager.DISALLOW_SHARE_LOCATION, !switchState,
                    Utils.getManagedProfile(mUserManager));
            mPreference.setSummary(switchState ?
                    R.string.switch_on_text : R.string.switch_off_text);
            return true;
        }
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        // Looking for a managed profile. If there are no managed profiles then we are removing the
        // managed profile category.
        return Utils.getManagedProfile(mUserManager) != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        if (!mPreference.isVisible() || !isAvailable()) {
            return;
        }
        final RestrictedLockUtils.EnforcedAdmin admin =
                mLocationEnabler.getShareLocationEnforcedAdmin(
                        Utils.getManagedProfile(mUserManager).getIdentifier());
        final boolean isRestrictedByBase = mLocationEnabler.isManagedProfileRestrictedByBase();
        if (!isRestrictedByBase && admin != null) {
            mPreference.setDisabledByAdmin(admin);
            mPreference.setChecked(false);
        } else {
            final boolean enabled = mLocationEnabler.isEnabled(mode);
            mPreference.setEnabled(enabled);

            int summaryResId = R.string.switch_off_text;
            if (!enabled) {
                mPreference.setChecked(false);
            } else {
                mPreference.setChecked(!isRestrictedByBase);
                summaryResId = (isRestrictedByBase ?
                        R.string.switch_off_text : R.string.switch_on_text);
            }
            mPreference.setSummary(summaryResId);
        }
    }
}

