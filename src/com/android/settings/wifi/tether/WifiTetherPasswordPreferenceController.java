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

import static com.android.settings.AllInOneTetherSettings.DEDUP_POSTFIX;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.WifiUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.UUID;

public class WifiTetherPasswordPreferenceController extends WifiTetherBasePreferenceController
        implements ValidatedEditTextPreference.Validator {

    private static final String PREF_KEY = "wifi_tether_network_password";

    private String mPassword;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    @VisibleForTesting
    WifiTetherPasswordPreferenceController(Context context, OnTetherConfigUpdateListener listener,
            MetricsFeatureProvider provider) {
        super(context, listener);
        mMetricsFeatureProvider = provider;
    }

    public WifiTetherPasswordPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null
                || (config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_WPA2_PSK
                && TextUtils.isEmpty(config.getPassphrase()))) {
            mPassword = generateRandomPassword();
        } else {
            mPassword = config.getPassphrase();
        }
        ((ValidatedEditTextPreference) mPreference).setValidator(this);
        ((ValidatedEditTextPreference) mPreference).setIsPassword(true);
        ((ValidatedEditTextPreference) mPreference).setIsSummaryPassword(true);
        updatePasswordDisplay((EditTextPreference) mPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!TextUtils.equals(mPassword, (String) newValue)) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_SETTINGS_CHANGE_WIFI_HOTSPOT_PASSWORD);
        }
        mPassword = (String) newValue;
        updatePasswordDisplay((EditTextPreference) mPreference);
        mListener.onTetherConfigUpdated(this);
        return true;
    }

    /**
     * This method returns the current password if it is valid for the indicated security type. If
     * the password currently set is invalid it will forcefully set a random password that is valid.
     *
     * @param securityType The security type for the password.
     * @return The current password if it is valid for the indicated security type. A new randomly
     * generated password if it is not.
     */
    public String getPasswordValidated(int securityType) {
        // don't actually overwrite unless we get a new config in case it was accidentally toggled.
        if (securityType == SoftApConfiguration.SECURITY_TYPE_OPEN) {
            return "";
        } else if (!isTextValid(mPassword)) {
            mPassword = generateRandomPassword();
            updatePasswordDisplay((EditTextPreference) mPreference);
        }
        return mPassword;
    }

    public void updateVisibility(int securityType) {
        mPreference.setVisible(securityType != SoftApConfiguration.SECURITY_TYPE_OPEN);
    }

    @Override
    public boolean isTextValid(String value) {
        return WifiUtils.isHotspotWpa2PasswordValid(value);
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
            pref.setVisible(true);
        } else {
            pref.setIsSummaryPassword(false);
            pref.setSummary(R.string.wifi_hotspot_no_password_subtext);
            pref.setVisible(false);
        }
    }
}
