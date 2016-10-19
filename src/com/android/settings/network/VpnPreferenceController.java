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
package com.android.settings.network;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceController;
import com.android.settingslib.RestrictedLockUtils;


public class VpnPreferenceController extends PreferenceController {

    private static final String KEY_VPN_SETTINGS = "vpn_settings";

    private final String mToggleable;
    private final boolean mIsSecondaryUser;

    public VpnPreferenceController(Context context) {
        super(context);
        mToggleable = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        mIsSecondaryUser = !UserManager.get(context).isAdminUser();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        // Manually set dependencies for Wifi when not toggleable.
        if (mToggleable == null || !mToggleable.contains(Settings.Global.RADIO_WIFI)) {
            final Preference pref = screen.findPreference(KEY_VPN_SETTINGS);
            if (pref != null) {
                pref.setDependency(AirplaneModePreferenceController.KEY_TOGGLE_AIRPLANE);
            }
        }
    }

    @Override
    protected boolean isAvailable() {
        // TODO: http://b/23693383
        return mIsSecondaryUser || RestrictedLockUtils.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_CONFIG_VPN, UserHandle.myUserId());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VPN_SETTINGS;
    }
}
