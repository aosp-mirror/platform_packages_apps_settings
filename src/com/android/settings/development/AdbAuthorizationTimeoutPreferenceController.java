/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller for the developer option to disable the automatic revocation of adb
 * authorizations.
 */
public class AdbAuthorizationTimeoutPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {
    private static final String ADB_AUTHORIZATION_TIMEOUT_KEY = "adb_authorization_timeout";

    private final Context mContext;

    public AdbAuthorizationTimeoutPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return ADB_AUTHORIZATION_TIMEOUT_KEY;
    }

    @Override
    public void updateState(Preference preference) {
        final long authTimeout = Settings.Global.getLong(mContext.getContentResolver(),
                Settings.Global.ADB_ALLOWED_CONNECTION_TIME,
                Settings.Global.DEFAULT_ADB_ALLOWED_CONNECTION_TIME);
        // An authTimeout of 0 indicates this preference is enabled and adb authorizations will not
        // be automatically revoked.
        ((TwoStatePreference) mPreference).setChecked(authTimeout == 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeSetting((boolean) newValue);
        return true;
    }

    @Override
    public void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeSetting(false);
        ((TwoStatePreference) mPreference).setChecked(false);
    }

    private void writeSetting(boolean isEnabled) {
        long authTimeout = 0;
        if (!isEnabled) {
            authTimeout = Settings.Global.DEFAULT_ADB_ALLOWED_CONNECTION_TIME;
        }
        Settings.Global.putLong(mContext.getContentResolver(),
                Settings.Global.ADB_ALLOWED_CONNECTION_TIME,
                authTimeout);
    }
}
