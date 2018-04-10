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

package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.WifiUtils;

import java.util.UUID;

public class WifiTetherPasswordPreferenceController extends WifiTetherBasePreferenceController
        implements ValidatedEditTextPreference.Validator {

    private static final String TAG = "WifiTetherPswdPref";
    private static final String PREF_KEY = "wifi_tether_network_password";

    private String mPassword;

    public WifiTetherPasswordPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final WifiConfiguration config = mWifiManager.getWifiApConfiguration();
        if (config != null) {
            mPassword = config.preSharedKey;
            Log.d(TAG, "Updating password in Preference, " + mPassword);
        } else {
            mPassword = generateRandomPassword();
        }
        ((ValidatedEditTextPreference) mPreference).setValidator(this);
        ((ValidatedEditTextPreference) mPreference).setIsSummaryPassword(true);
        updatePasswordDisplay((EditTextPreference) mPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mPassword = (String) newValue;
        updatePasswordDisplay((EditTextPreference) mPreference);
        mListener.onTetherConfigUpdated();
        return true;
    }

    public String getPassword() {
        return mPassword;
    }

    public int getSecuritySettingForPassword() {
        // We should return NONE when no password is set
        if (TextUtils.isEmpty(mPassword)) {
            return WifiConfiguration.KeyMgmt.NONE;
        }
        // Only other currently supported type is WPA2 so we'll try that
        return WifiConfiguration.KeyMgmt.WPA2_PSK;
    }

    @Override
    public boolean isTextValid(String value) {
        return WifiUtils.isHotspotPasswordValid(value);
    }

    private static String generateRandomPassword() {
        String randomUUID = UUID.randomUUID().toString();
        //first 12 chars from xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        return randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
    }

    private void updatePasswordDisplay(EditTextPreference preference) {
        ValidatedEditTextPreference pref = (ValidatedEditTextPreference) preference;
        pref.setText(mPassword);
        if (!TextUtils.isEmpty(mPassword)) {
            pref.setIsSummaryPassword(true);
            pref.setSummary(mPassword);
        } else {
            pref.setIsSummaryPassword(false);
            pref.setSummary(R.string.wifi_hotspot_no_password_subtext);
        }
    }
}
