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
import android.provider.Settings.Global;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AddUserWhenLockedPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnPause, OnResume {

    private final String mPrefKey;
    private final UserCapabilities mUserCaps;
    private boolean mShouldUpdateUserList;

    public AddUserWhenLockedPreferenceController(Context context, String key, Lifecycle lifecycle) {
        super(context);
        mPrefKey = key;
        mUserCaps = UserCapabilities.create(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void updateState(Preference preference) {
        RestrictedSwitchPreference restrictedSwitchPreference =
                (RestrictedSwitchPreference) preference;
        int value = Global.getInt(mContext.getContentResolver(), Global.ADD_USERS_WHEN_LOCKED, 0);
        restrictedSwitchPreference.setChecked(value == 1);
        restrictedSwitchPreference.setDisabledByAdmin(
                mUserCaps.disallowAddUser() ? mUserCaps.getEnforcedAdmin() : null);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean value = (Boolean) newValue;
        Global.putInt(mContext.getContentResolver(),
                Global.ADD_USERS_WHEN_LOCKED, value != null && value ? 1 : 0);
        return true;
    }

    @Override
    public void onPause() {
        mShouldUpdateUserList = true;
    }

    @Override
    public void onResume() {
        if (mShouldUpdateUserList) {
            mUserCaps.updateAddUserCapabilities(mContext);
        }
    }

    @Override
    public boolean isAvailable() {
        return mUserCaps.isAdmin() &&
                (!mUserCaps.disallowAddUser() || mUserCaps.disallowAddUserSetByAdmin());
    }

    @Override
    public String getPreferenceKey() {
        return mPrefKey;
    }
}
