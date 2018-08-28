/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.accounts;

import static android.provider.Settings.Secure.MANAGED_PROFILE_CONTACT_REMOTE_SEARCH;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.SliceData;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;

public class ContactSearchPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private UserHandle mManagedUser;

    public ContactSearchPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setManagedUser(UserHandle managedUser) {
        mManagedUser = managedUser;
    }

    @Override
    public int getAvailabilityStatus() {
        return (mManagedUser != null) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof RestrictedSwitchPreference) {
            final RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            pref.setChecked(isChecked());
            if (mManagedUser != null) {
                final RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                        RestrictedLockUtilsInternal.checkIfRemoteContactSearchDisallowed(
                                mContext, mManagedUser.getIdentifier());
                pref.setDisabledByAdmin(enforcedAdmin);
            }
        }
    }

    private boolean isChecked() {
        if (mManagedUser == null) {
            return false;
        }
        return 0 != Settings.Secure.getIntForUser(mContext.getContentResolver(),
                MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, 0, mManagedUser.getIdentifier());
    }

    private boolean setChecked(boolean isChecked) {
        if (mManagedUser != null) {
            final int value = isChecked ? 1 : 0;
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    MANAGED_PROFILE_CONTACT_REMOTE_SEARCH, value, mManagedUser.getIdentifier());
        }
        return true;
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        return setChecked((boolean) newValue);
    }

    @Override
    @SliceData.SliceType
    public int getSliceType() {
        return SliceData.SliceType.SWITCH;
    }
}