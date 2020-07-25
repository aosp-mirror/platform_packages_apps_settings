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

import static com.android.settings.wifi.dpp.WifiQrCode.SECURITY_NO_PASSWORD;
import static com.android.settings.wifi.dpp.WifiQrCode.SECURITY_SAE;
import static com.android.settings.wifi.dpp.WifiQrCode.SECURITY_WEP;
import static com.android.settings.wifi.dpp.WifiQrCode.SECURITY_WPA_PSK;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the parameters of ZXing reader library's Wi-Fi Network config format.
 * Please check {@code WifiQrCode} for detail of the format.
 *
 * Checks below members of {@code WifiDppUtils} for more information.
 * EXTRA_WIFI_SECURITY / EXTRA_WIFI_SSID / EXTRA_WIFI_PRE_SHARED_KEY / EXTRA_WIFI_HIDDEN_SSID /
 * EXTRA_QR_CODE
 */
public class WifiNetworkConfig {

    static final String FAKE_SSID = "fake network";
    static final String FAKE_PASSWORD = "password";
    private static final String TAG = "WifiNetworkConfig";

    private String mSecurity;
    private String mSsid;
    private String mPreSharedKey;
    private boolean mHiddenSsid;
    private int mNetworkId;
    private boolean mIsHotspot;

    @VisibleForTesting
    WifiNetworkConfig(String security, String ssid, String preSharedKey,
            boolean hiddenSsid, int networkId, boolean isHotspot) {
        mSecurity = security;
        mSsid = ssid;
        mPreSharedKey = preSharedKey;
        mHiddenSsid = hiddenSsid;
        mNetworkId = networkId;
        mIsHotspot = isHotspot;
    }

    public WifiNetworkConfig(WifiNetworkConfig config) {
        mSecurity = config.mSecurity;
        mSsid = config.mSsid;
        mPreSharedKey = config.mPreSharedKey;
        mHiddenSsid = config.mHiddenSsid;
        mNetworkId = config.mNetworkId;
        mIsHotspot = config.mIsHotspot;
    }

    /**
     * Wi-Fi DPP activities should implement this interface for fragments to retrieve the
     * WifiNetworkConfig for configuration
     */
    public interface Retriever {
        WifiNetworkConfig getWifiNetworkConfig();
    }

    /**
     * Retrieve WifiNetworkConfig from below 2 intents
     *
     * android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR
     * android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_SCANNER
     */
    static WifiNetworkConfig getValidConfigOrNull(Intent intent) {
        final String security = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SECURITY);
        final String ssid = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SSID);
        final String preSharedKey = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY);
        final boolean hiddenSsid = intent.getBooleanExtra(WifiDppUtils.EXTRA_WIFI_HIDDEN_SSID,
                false);
        final int networkId = intent.getIntExtra(WifiDppUtils.EXTRA_WIFI_NETWORK_ID,
                WifiConfiguration.INVALID_NETWORK_ID);
        final boolean isHotspot = intent.getBooleanExtra(WifiDppUtils.EXTRA_IS_HOTSPOT, false);

        return getValidConfigOrNull(security, ssid, preSharedKey, hiddenSsid, networkId, isHotspot);
    }

    static WifiNetworkConfig getValidConfigOrNull(String security, String ssid,
            String preSharedKey, boolean hiddenSsid, int networkId, boolean isHotspot) {
        if (!isValidConfig(security, ssid, preSharedKey, hiddenSsid)) {
            return null;
        }

        return new WifiNetworkConfig(security, ssid, preSharedKey, hiddenSsid, networkId,
                isHotspot);
    }

    static boolean isValidConfig(WifiNetworkConfig config) {
        if (config == null) {
            return false;
        } else {
            return isValidConfig(config.mSecurity, config.mSsid, config.mPreSharedKey,
                    config.mHiddenSsid);
        }
    }

    static boolean isValidConfig(String security, String ssid, String preSharedKey,
            boolean hiddenSsid) {
        if (!TextUtils.isEmpty(security) && !SECURITY_NO_PASSWORD.equals(security)) {
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
    String getQrCode() {
        final String empty = "";
        return new StringBuilder("WIFI:")
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
    }

    public String getSecurity() {
        return mSecurity;
    }

    public String getSsid() {
        return mSsid;
    }

    public String getPreSharedKey() {
        return mPreSharedKey;
    }

    public boolean getHiddenSsid() {
        return mHiddenSsid;
    }

    public int getNetworkId() {
        return mNetworkId;
    }

    public boolean isHotspot() {
        return mIsHotspot;
    }

    public boolean isSupportWifiDpp(Context context) {
        if (!WifiDppUtils.isWifiDppEnabled(context)) {
            return false;
        }

        if (TextUtils.isEmpty(mSecurity)) {
            return false;
        }

        // DPP 1.0 only supports SAE and PSK.
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);
        switch (mSecurity) {
            case SECURITY_SAE:
                if (wifiManager.isWpa3SaeSupported()) {
                    return true;
                }
                break;
            case SECURITY_WPA_PSK:
                return true;
            default:
        }
        return false;
    }

    /**
     * This is a simplified method from {@code WifiConfigController.getConfig()}
     *
     * @return When it's a open network, returns 2 WifiConfiguration in the List, the 1st is
     *         open network and the 2nd is enhanced open network. Returns 1 WifiConfiguration in the
     *         List for all other supported Wi-Fi securities.
     */
    List<WifiConfiguration> getWifiConfigurations() {
        final List<WifiConfiguration> wifiConfigurations = new ArrayList<>();

        if (!isValidConfig(this)) {
            return wifiConfigurations;
        }

        if (TextUtils.isEmpty(mSecurity) || SECURITY_NO_PASSWORD.equals(mSecurity)) {
            // TODO (b/129835824): we add both open network and enhanced open network to WifiManager
            //                     for android Q, should improve it in the future.
            final WifiConfiguration openNetworkWifiConfiguration = getBasicWifiConfiguration();
            openNetworkWifiConfiguration.allowedKeyManagement.set(KeyMgmt.NONE);
            wifiConfigurations.add(openNetworkWifiConfiguration);

            final WifiConfiguration enhancedOpenNetworkWifiConfiguration =
                    getBasicWifiConfiguration();
            enhancedOpenNetworkWifiConfiguration
                    .setSecurityParams(WifiConfiguration.SECURITY_TYPE_OWE);
            wifiConfigurations.add(enhancedOpenNetworkWifiConfiguration);
            return wifiConfigurations;
        }

        final WifiConfiguration wifiConfiguration = getBasicWifiConfiguration();
        if (mSecurity.startsWith(SECURITY_WEP)) {
            wifiConfiguration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WEP);

            // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
            final int length = mPreSharedKey.length();
            if ((length == 10 || length == 26 || length == 58)
                    && mPreSharedKey.matches("[0-9A-Fa-f]*")) {
                wifiConfiguration.wepKeys[0] = mPreSharedKey;
            } else {
                wifiConfiguration.wepKeys[0] = addQuotationIfNeeded(mPreSharedKey);
            }
        } else if (mSecurity.startsWith(SECURITY_WPA_PSK)) {
            wifiConfiguration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_PSK);

            if (mPreSharedKey.matches("[0-9A-Fa-f]{64}")) {
                wifiConfiguration.preSharedKey = mPreSharedKey;
            } else {
                wifiConfiguration.preSharedKey = addQuotationIfNeeded(mPreSharedKey);
            }
        } else if (mSecurity.startsWith(SECURITY_SAE)) {
            wifiConfiguration.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
            if (mPreSharedKey.length() != 0) {
                wifiConfiguration.preSharedKey = addQuotationIfNeeded(mPreSharedKey);
            }
        } else {
            Log.w(TAG, "Unsupported security");
            return wifiConfigurations;
        }

        wifiConfigurations.add(wifiConfiguration);
        return wifiConfigurations;
    }

    private WifiConfiguration getBasicWifiConfiguration() {
        final WifiConfiguration wifiConfiguration = new WifiConfiguration();

        wifiConfiguration.SSID = addQuotationIfNeeded(mSsid);
        wifiConfiguration.hiddenSSID = mHiddenSsid;
        wifiConfiguration.networkId = mNetworkId;
        return wifiConfiguration;
    }

    private String addQuotationIfNeeded(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(input).append("\"");
        return sb.toString();
    }
}
