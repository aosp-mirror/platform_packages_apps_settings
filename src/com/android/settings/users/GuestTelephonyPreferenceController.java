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

package com.android.settings.users;

import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controls the preference on the user settings screen which determines whether the guest user
 * should have access to telephony or not.
 */
public class GuestTelephonyPreferenceController extends TogglePreferenceController {

    private final UserManager mUserManager;
    private final UserCapabilities mUserCaps;
    private Bundle mDefaultGuestRestrictions;

    public GuestTelephonyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUserManager = context.getSystemService(UserManager.class);
        mUserCaps = UserCapabilities.create(context);
        mDefaultGuestRestrictions = mUserManager.getDefaultGuestRestrictions();
        mDefaultGuestRestrictions.putBoolean(UserManager.DISALLOW_SMS, true);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mUserCaps.isAdmin() || !mUserCaps.mCanAddGuest) {
            return DISABLED_FOR_USER;
        } else {
            return mUserCaps.mUserSwitcherEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public boolean isChecked() {
        return !mDefaultGuestRestrictions.getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mDefaultGuestRestrictions.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS, !isChecked);
        mUserManager.setDefaultGuestRestrictions(mDefaultGuestRestrictions);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mUserCaps.updateAddUserCapabilities(mContext);
        preference.setVisible(isAvailable() && mUserCaps.mUserSwitcherEnabled);
    }
}
