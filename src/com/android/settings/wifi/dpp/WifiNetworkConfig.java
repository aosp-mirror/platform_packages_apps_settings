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

import androidx.annotation.Keep;

/**
 * Wraps the parameters of ZXing reader library's Wi-Fi Network config format.
 * Please check {@code WifiQrCode} for detail of the format.
 *
 * Checks below members of {@code WifiDppUtils} for more information.
 * EXTRA_WIFI_SECURITY / EXTRA_WIFI_SSID / EXTRA_WIFI_PRE_SHARED_KEY / EXTRA_WIFI_HIDDEN_SSID /
 * EXTRA_QR_CODE
 */
public class WifiNetworkConfig {
    // Ignores password if security is NO_PASSWORD or absent
    public static final String NO_PASSWORD = "nopass";

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
        if (config.mSecurity != null) {
            mSecurity = new String(config.mSecurity);
        }

        if (config.mSsid != null) {
            mSsid = new String(config.mSsid);
        }

        if (config.mPreSharedKey != null) {
            mPreSharedKey = new String(config.mPreSharedKey);
        }

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

        return getValidConfigOrNull(security, ssid, preSharedKey, hiddenSsid);
    }

    public static WifiNetworkConfig getValidConfigOrNull(String security, String ssid,
            String preSharedKey, boolean hiddenSsid) {
        if (!isValidConfig(security, ssid, preSharedKey, hiddenSsid)) {
            return null;
        }

        return new WifiNetworkConfig(security, ssid, preSharedKey, hiddenSsid);
    }

    public static boolean isValidConfig(WifiNetworkConfig config) {
        if (config == null) {
            return false;
        } else {
            return isValidConfig(config.mSecurity, config.mSsid, config.mPreSharedKey,
                    config.mHiddenSsid);
        }
    }

    public static boolean isValidConfig(String security, String ssid, String preSharedKey,
            boolean hiddenSsid) {
        if (!TextUtils.isEmpty(security) && !NO_PASSWORD.equals(security)) {
            if (TextUtils.isEmpty(preSharedKey)) {
                return false;
            }
        }

        if (!hiddenSsid && TextUtils.isEmpty(ssid)) {
            return false;
        }

        return true;
    }

    /**
     * Escaped special characters "\", ";", ":", "," with a backslash
     * See https://github.com/zxing/zxing/wiki/Barcode-Contents
     */
    private String escapeSpecialCharacters(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch =='\\' || ch == ',' || ch == ';' || ch == ':') {
                buf.append('\\');
            }
            buf.append(ch);
        }

        return buf.toString();
    }

    /**
     * Construct a barcode string for WiFi network login.
     * See https://en.wikipedia.org/wiki/QR_code#WiFi_network_login
     */
    public String getQrCode() {
        final String empty = "";
        String barcode = new StringBuilder("WIFI:")
                .append("S:")
                .append(escapeSpecialCharacters(mSsid))
                .append(";")
                .append("T:")
                .append(TextUtils.isEmpty(mSecurity) ? empty : mSecurity)
                .append(";")
                .append("P:")
                .append(TextUtils.isEmpty(mPreSharedKey) ? empty
                        : escapeSpecialCharacters(mPreSharedKey))
                .append(";")
                .append("H:")
                .append(mHiddenSsid)
                .append(";;")
                .toString();
        return barcode;
    }

    @Keep
    public String getSecurity() {
        return mSecurity;
    }

    @Keep
    public String getSsid() {
        return mSsid;
    }

    @Keep
    public String getPreSharedKey() {
        return mPreSharedKey;
    }

    @Keep
    public boolean getHiddenSsid() {
        return mHiddenSsid;
    }
}
