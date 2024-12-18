/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.users;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.RestrictedSwitchPreference;

public class AddUserWhenLockedPreferenceController extends TogglePreferenceController {

    private final UserCapabilities mUserCaps;

    public AddUserWhenLockedPreferenceController(Context context, String key) {
        super(context, key);
        mUserCaps = UserCapabilities.create(context);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mUserCaps.updateAddUserCapabilities(mContext);
        final RestrictedSwitchPreference restrictedSwitchPreference =
                (RestrictedSwitchPreference) preference;
        if (!isAvailable()) {
            restrictedSwitchPreference.setVisible(false);
        } else {
            if (android.multiuser.Flags.newMultiuserSettingsUx()) {
                restrictedSwitchPreference.setVisible(true);
                if (mUserCaps.mDisallowAddUserSetByAdmin) {
                    restrictedSwitchPreference.setDisabledByAdmin(mUserCaps.mEnforcedAdmin);
                } else if (mUserCaps.mDisallowAddUser) {
                    restrictedSwitchPreference.setVisible(false);
                }
            } else {
                restrictedSwitchPreference.setDisabledByAdmin(
                        mUserCaps.disallowAddUser() ? mUserCaps.getEnforcedAdmin() : null);
                restrictedSwitchPreference.setVisible(mUserCaps.mUserSwitcherEnabled);
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mUserCaps.isAdmin()) {
            return DISABLED_FOR_USER;
        } else if (android.multiuser.Flags.newMultiuserSettingsUx()) {
            return AVAILABLE;
        } else if (mUserCaps.disallowAddUser() || mUserCaps.disallowAddUserSetByAdmin()) {
            return DISABLED_FOR_USER;
        } else {
            return mUserCaps.mUserSwitcherEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADD_USERS_WHEN_LOCKED, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADD_USERS_WHEN_LOCKED, isChecked ? 1 : 0);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
