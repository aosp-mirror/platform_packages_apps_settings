/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import android.content.Intent;
import android.text.TextUtils;

/**
 * Contains the Wi-Fi Network config parameters described in
 * https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11
 *
 * Checks below members of {@code WifiDppUtils} for more information.
 * EXTRA_WIFI_SECURITY / EXTRA_WIFI_SSID / EXTRA_WIFI_PRE_SHARED_KEY / EXTRA_WIFI_HIDDEN_SSID /
 * EXTRA_QR_CODE
 */
public class WifiNetworkConfig {
    private String mSecurity;
    private String mSsid;
    private String mPreSharedKey;
    private boolean mHiddenSsid;

    private WifiNetworkConfig(String security, String ssid, String preSharedKey,
            boolean hiddenSsid) {
        mSecurity = security;
        mSsid = ssid;
        mPreSharedKey = preSharedKey;
        mHiddenSsid = hiddenSsid;
    }

    public WifiNetworkConfig(WifiNetworkConfig config) {
        mSecurity = new String(config.mSecurity);
        mSsid = new String(config.mSsid);
        mPreSharedKey = new String(config.mPreSharedKey);
        mHiddenSsid = config.mHiddenSsid;
    }

    /**
     * Wi-Fi DPP activities should implement this interface for fragments to retrieve the
     * WifiNetworkConfig for configuration
     */
    public interface Retriever {
        public WifiNetworkConfig getWifiNetworkConfig();
        public boolean setWifiNetworkConfig(WifiNetworkConfig config);
    }

    /**
     * Retrieve WifiNetworkConfig from below 2 intents
     *
     * android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR
     * android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_SCANNER
     */
    public static WifiNetworkConfig getValidConfigOrNull(Intent intent) {
        String security = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SECURITY);
        String ssid = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SSID);
        String preSharedKey = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY);
        boolean hiddenSsid = intent.getBooleanExtra(WifiDppUtils.EXTRA_WIFI_HIDDEN_SSID, false);

        if (!isValidConfig(security, ssid, hiddenSsid)) {
            return null;
        }

        if (ssid == null) {
            ssid = "";
        }

        return new WifiNetworkConfig(security, ssid, preSharedKey, hiddenSsid);
    }

    public static boolean isValidConfig(WifiNetworkConfig config) {
        if (config == null) {
            return false;
        } else {
            return isValidConfig(config.mSecurity, config.mSsid, config.mHiddenSsid);
        }
    }

    public static boolean isValidConfig(String security, String ssid, boolean hiddenSsid) {
        if (TextUtils.isEmpty(security)) {
            return false;
        }

        if (!hiddenSsid && TextUtils.isEmpty(ssid)) {
            return false;
        }

        return true;
    }

    public String getSecurity() {
        return new String(mSecurity);
    }

    public String getSsid() {
        return new String(mSsid);
    }

    public String getPreSharedKey() {
        return new String(mPreSharedKey);
    }

    public boolean getHiddenSsid() {
        return mHiddenSsid;
    }
}
