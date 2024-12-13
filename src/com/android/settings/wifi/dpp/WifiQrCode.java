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

import android.annotation.NonNull;
import android.net.wifi.UriParserResults;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiUriParser;
import android.text.TextUtils;
import android.util.Log;

/**
 * Supports to parse 2 types of QR code
 *
 *     1. Standard Wi-Fi DPP bootstrapping information or
 *     2. ZXing reader library's Wi-Fi Network config format described in
 *     https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11
 *
 * ZXing reader library's Wi-Fi Network config format example:
 *
 *     WIFI:T:WPA;S:mynetwork;P:mypass;;
 *
 * parameter Example    Description
 * T         WPA        Authentication type; can be WEP or WPA, or 'nopass' for no password. Or,
 *                      omit for no password.
 * S         mynetwork  Network SSID. Required. Enclose in double quotes if it is an ASCII name,
 *                      but could be interpreted as hex (i.e. "ABCD")
 * P         mypass     Password, ignored if T is "nopass" (in which case it may be omitted).
 *                      Enclose in double quotes if it is an ASCII name, but could be interpreted as
 *                      hex (i.e. "ABCD")
 * H         true       Optional. True if the network SSID is hidden.
 *
 */
public class WifiQrCode {
    private static final String TAG = "WifiQrCode";
    static final String SCHEME_DPP = "DPP";
    static final String SCHEME_ZXING_WIFI_NETWORK_CONFIG = "WIFI";
    static final String PREFIX_DPP = "DPP:";
    static final String PREFIX_ZXING_WIFI_NETWORK_CONFIG = "WIFI:";

    static final String PREFIX_DPP_PUBLIC_KEY = "K:";
    static final String PREFIX_DPP_INFORMATION = "I:";

    static final String PREFIX_ZXING_SECURITY = "T:";
    static final String PREFIX_ZXING_SSID = "S:";
    static final String PREFIX_ZXING_PASSWORD = "P:";
    static final String PREFIX_ZXING_HIDDEN_SSID = "H:";

    static final String DELIMITER_QR_CODE = ";";

    // Ignores password if security is SECURITY_NO_PASSWORD or absent
    static final String SECURITY_NO_PASSWORD = "nopass"; //open network or OWE
    static final String SECURITY_WEP = "WEP";
    static final String SECURITY_WPA_PSK = "WPA";
    static final String SECURITY_SAE = "SAE";

    private String mQrCode;
    @NonNull
    private UriParserResults mUriParserResults;

    public WifiQrCode(String qrCode) throws IllegalArgumentException {
        if (TextUtils.isEmpty(qrCode)) {
            throw new IllegalArgumentException("Empty QR code");
        }

        mQrCode = qrCode;
        try {
            mUriParserResults = WifiUriParser.parseUri(mQrCode);
            Log.i("WifiQrCode", "mUriParserResults = " + mUriParserResults);
        } catch (IllegalArgumentException ie) {
            throw new IllegalArgumentException("Invalid scheme");
        }
    }

    String getQrCode() {
        return mQrCode;
    }

    /**
     * Uses to check type of QR code
     *
     * SCHEME_DPP for standard Wi-Fi device provision protocol; SCHEME_ZXING_WIFI_NETWORK_CONFIG
     * for ZXing reader library' Wi-Fi Network config format
     */
    public int getScheme() {
        return mUriParserResults.getUriScheme();
    }

    /** Available when {@code getScheme()} returns SCHEME_DPP */
    String getPublicKey() {
        return mUriParserResults.getPublicKey();
    }

    /** May be available when {@code getScheme()} returns SCHEME_DPP */
    public String getInformation() {
        return mUriParserResults.getInformation();
    }

    /** Available when {@code getScheme()} returns SCHEME_ZXING_WIFI_NETWORK_CONFIG */
    WifiConfiguration getWifiConfiguration() {
        return mUriParserResults.getWifiConfiguration();
    }

    static WifiQrCode getValidWifiDppQrCodeOrNull(String qrCode) {
        WifiQrCode wifiQrCode;
        try {
            wifiQrCode = new WifiQrCode(qrCode);
        } catch(IllegalArgumentException e) {
            Log.e(TAG, "Failed to create WifiQrCode!", e);
            return null;
        }
        if (wifiQrCode.getScheme() != UriParserResults.URI_SCHEME_DPP) {
            Log.e(TAG, "wifiQrCode scheme is not DPP!");
            return null;
        }
        return wifiQrCode;
    }
}
