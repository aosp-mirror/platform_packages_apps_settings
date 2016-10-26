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
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TetherSettings;
import com.android.settings.core.PreferenceController;

import java.util.List;

import static android.os.UserManager.DISALLOW_CONFIG_TETHERING;
import static com.android.settingslib.RestrictedLockUtils.checkIfRestrictionEnforced;
import static com.android.settingslib.RestrictedLockUtils.hasBaseUserRestriction;

public class TetherPreferenceController extends PreferenceController {

    private static final String KEY_TETHER_SETTINGS = "tether_settings";

    private final boolean mAdminDisallowedTetherConfig;
    private final ConnectivityManager mConnectivityManager;
    private final UserManager mUserManager;

    public TetherPreferenceController(Context context) {
        super(context);
        mAdminDisallowedTetherConfig = checkIfRestrictionEnforced(
                context, DISALLOW_CONFIG_TETHERING, UserHandle.myUserId()) != null;
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(KEY_TETHER_SETTINGS);
        if (preference != null && !mAdminDisallowedTetherConfig) {
            preference.setTitle(
                    com.android.settingslib.Utils.getTetheringLabel(mConnectivityManager));

            // Grey out if provisioning is not available.
            preference.setEnabled(!TetherSettings.isProvisioningNeededButUnavailable(mContext));
        }
    }

    @Override
    public boolean isAvailable() {
        final boolean isBlocked =
                (!mConnectivityManager.isTetheringSupported() && !mAdminDisallowedTetherConfig)
                        || hasBaseUserRestriction(mContext, DISALLOW_CONFIG_TETHERING,
                        UserHandle.myUserId());
        return !isBlocked;
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        if (!mUserManager.isAdminUser() || !mConnectivityManager.isTetheringSupported()) {
            keys.add(KEY_TETHER_SETTINGS);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TETHER_SETTINGS;
    }
}
