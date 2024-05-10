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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.repository.WifiHotspotRepository;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Controller for logic pertaining to the password of Wi-Fi tethering.
 */
public class WifiTetherPasswordPreferenceController extends WifiTetherBasePreferenceController
        implements ValidatedEditTextPreference.Validator {

    private static final String PREF_KEY = "wifi_tether_network_password";

    private String mPassword;
    private int mSecurityType;

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final WifiHotspotRepository mWifiHotspotRepository;

    @VisibleForTesting
    WifiTetherPasswordPreferenceController(Context context, OnTetherConfigUpdateListener listener,
            MetricsFeatureProvider provider) {
        super(context, listener);
        FeatureFactory featureFactory = FeatureFactory.getFeatureFactory();
        mMetricsFeatureProvider = (provider != null) ? provider
                : featureFactory.getMetricsFeatureProvider();
        mWifiHotspotRepository = featureFactory.getWifiFeatureProvider().getWifiHotspotRepository();
        mWifiHotspotRepository.queryLastPasswordIfNeeded();
    }

    public WifiTetherPasswordPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        this(context, listener, null /* MetricsFeatureProvider */);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_OPEN
                && TextUtils.isEmpty(config.getPassphrase())) {
            mPassword = mWifiHotspotRepository.generatePassword();
        } else {
            mPassword = config.getPassphrase();
        }
        mSecurityType = config.getSecurityType();
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
        } else if (!WifiUtils.isHotspotPasswordValid(mPassword, securityType)) {
            mPassword = mWifiHotspotRepository.generatePassword();
            updatePasswordDisplay((EditTextPreference) mPreference);
        }
        return mPassword;
    }

    /**
     * This method set the security type of user selection. Then the controller will based on the
     * security type changed to update the password changed on the preference.
     *
     * @param securityType The security type of SoftApConfiguration.
     */
    public void setSecurityType(int securityType) {
        mSecurityType = securityType;
        mPreference.setVisible(securityType != SoftApConfiguration.SECURITY_TYPE_OPEN);
    }

    @Override
    public boolean isTextValid(String value) {
        return WifiUtils.isHotspotPasswordValid(value, mSecurityType);
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
