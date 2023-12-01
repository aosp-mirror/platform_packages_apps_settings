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
import android.content.Intent;
import android.net.wifi.SoftApConfiguration;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class WifiTetherSSIDPreferenceController extends WifiTetherBasePreferenceController
        implements ValidatedEditTextPreference.Validator {

    private static final String TAG = "WifiTetherSsidPref";
    private static final String PREF_KEY = "wifi_tether_network_name";
    @VisibleForTesting
    static final String DEFAULT_SSID = "AndroidAP";

    private String mSSID;
    private WifiDeviceNameTextValidator mWifiDeviceNameTextValidator;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    // This constructor is used for testing.
    @VisibleForTesting
    WifiTetherSSIDPreferenceController(Context context, OnTetherConfigUpdateListener listener,
            MetricsFeatureProvider provider) {
        super(context, listener);
        mWifiDeviceNameTextValidator = new WifiDeviceNameTextValidator();
        mMetricsFeatureProvider = provider;
    }

    public WifiTetherSSIDPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);

        mWifiDeviceNameTextValidator = new WifiDeviceNameTextValidator();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config != null) {
            mSSID = config.getSsid();
        } else {
            mSSID = DEFAULT_SSID;
        }
        ((ValidatedEditTextPreference) mPreference).setValidator(this);

        if (mWifiManager.isWifiApEnabled() && config != null) {
            final Intent intent = WifiDppUtils.getHotspotConfiguratorIntentOrNull(mContext,
                    mWifiManager, config);

            if (intent == null) {
                Log.e(TAG, "Invalid security to share hotspot");
                ((WifiTetherSsidPreference) mPreference).setButtonVisible(false);
            } else {
                ((WifiTetherSsidPreference) mPreference).setButtonOnClickListener(
                        view -> shareHotspotNetwork(intent));
                ((WifiTetherSsidPreference) mPreference).setButtonVisible(true);
            }
        } else {
            ((WifiTetherSsidPreference) mPreference).setButtonVisible(false);
        }

        updateSsidDisplay((EditTextPreference) mPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!TextUtils.equals(mSSID, (String) newValue)) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_SETTINGS_CHANGE_WIFI_HOTSPOT_NAME);
        }
        mSSID = (String) newValue;
        updateSsidDisplay((EditTextPreference) preference);
        mListener.onTetherConfigUpdated(this);
        return true;
    }

    @Override
    public boolean isTextValid(String value) {
        return mWifiDeviceNameTextValidator.isTextValid(value);
    }

    public String getSSID() {
        return mSSID;
    }

    private void updateSsidDisplay(EditTextPreference preference) {
        preference.setText(mSSID);
        preference.setSummary(mSSID);
    }

    private void shareHotspotNetwork(Intent intent) {
        WifiDppUtils.showLockScreen(mContext, () -> {
            mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_SETTINGS_SHARE_WIFI_HOTSPOT_QR_CODE,
                    SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR,
                    /* key */ null,
                    /* value */ Integer.MIN_VALUE);

            mContext.startActivity(intent);
        });
    }

    @VisibleForTesting
    boolean isQrCodeButtonAvailable() {
        return ((WifiTetherSsidPreference) mPreference).isQrCodeButtonAvailable();
    }
}
