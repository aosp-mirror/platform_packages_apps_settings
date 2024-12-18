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
package com.android.settings.wifi.dpp;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extension of WifiQrCode to support ADB QR code format.
 * It will be based on the ZXing format:
 *
 * WIFI:T:ADB;S:myname;P:mypassword;;
 */
public class AdbQrCode {
    static final String SECURITY_ADB = "ADB";
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
    /**
     * SCHEME_DPP for standard Wi-Fi device provision protocol; SCHEME_ZXING_WIFI_NETWORK_CONFIG
     * for ZXing reader library' Wi-Fi Network config format
     */
    private String mScheme;
    // Data from parsed Wi-Fi DPP QR code
    private String mPublicKey;
    private String mInformation;
    // Data from parsed ZXing reader library's Wi-Fi Network config format
    private WifiNetworkConfig mAdbConfig;

    public AdbQrCode(String qrCode) throws IllegalArgumentException {
        if (TextUtils.isEmpty(qrCode)) {
            throw new IllegalArgumentException("Empty QR code");
        }

        mQrCode = qrCode;
        if (qrCode.startsWith(PREFIX_DPP)) {
            mScheme = SCHEME_DPP;
            parseWifiDppQrCode(qrCode);
        } else if (qrCode.startsWith(PREFIX_ZXING_WIFI_NETWORK_CONFIG)) {
            mScheme = SCHEME_ZXING_WIFI_NETWORK_CONFIG;
            parseZxingWifiQrCode(qrCode);
        } else {
            throw new IllegalArgumentException("Invalid scheme");
        }

        // Only accept the zxing format.
        if (!SCHEME_ZXING_WIFI_NETWORK_CONFIG.equals(getScheme())) {
            throw new IllegalArgumentException("DPP format not supported for ADB QR code");
        }
        mAdbConfig = getWifiNetworkConfig();

        if (!SECURITY_ADB.equals(mAdbConfig.getSecurity())) {
            throw new IllegalArgumentException("Invalid security type");
        }

        if (TextUtils.isEmpty(mAdbConfig.getSsid())) {
            throw new IllegalArgumentException("Empty service name");
        }

        if (TextUtils.isEmpty(mAdbConfig.getPreSharedKey())) {
            throw new IllegalArgumentException("Empty password");
        }
    }

    public WifiNetworkConfig getAdbNetworkConfig() {
        return mAdbConfig;
    }

    /**
     * Triggers a vibration to notify of a valid QR code.
     *
     * @param context The context to use
     */
    public static void triggerVibrationForQrCodeRecognition(Context context) {
        WifiDppUtils.triggerVibrationForQrCodeRecognition(context);
    }

    /** Parses Wi-Fi DPP QR code string */
    private void parseWifiDppQrCode(String qrCode) throws IllegalArgumentException {
        List<String> keyValueList = getKeyValueList(qrCode, PREFIX_DPP, DELIMITER_QR_CODE);
        String publicKey = getValueOrNull(keyValueList, PREFIX_DPP_PUBLIC_KEY);
        if (TextUtils.isEmpty(publicKey)) {
            throw new IllegalArgumentException("Invalid format");
        }
        mPublicKey = publicKey;
        mInformation = getValueOrNull(keyValueList, PREFIX_DPP_INFORMATION);
    }

    /** Parses ZXing reader library's Wi-Fi Network config format */
    private void parseZxingWifiQrCode(String qrCode) throws IllegalArgumentException {
        List<String> keyValueList = getKeyValueList(qrCode, PREFIX_ZXING_WIFI_NETWORK_CONFIG,
                DELIMITER_QR_CODE);
        String security = getValueOrNull(keyValueList, PREFIX_ZXING_SECURITY);
        String ssid = getValueOrNull(keyValueList, PREFIX_ZXING_SSID);
        String password = getValueOrNull(keyValueList, PREFIX_ZXING_PASSWORD);
        String hiddenSsidString = getValueOrNull(keyValueList, PREFIX_ZXING_HIDDEN_SSID);
        boolean hiddenSsid = "true".equalsIgnoreCase(hiddenSsidString);
        //"\", ";", "," and ":" are escaped with a backslash "\", should remove at first
        security = removeBackSlash(security);
        ssid = removeBackSlash(ssid);
        password = removeBackSlash(password);
        mAdbConfig = WifiNetworkConfig.getValidConfigOrNull(security, ssid, password,
                hiddenSsid, WifiConfiguration.INVALID_NETWORK_ID, /* isHotspot */ false);
        if (mAdbConfig == null) {
            throw new IllegalArgumentException("Invalid format");
        }
    }

    /**
     * Splits key/value pairs from qrCode
     *
     * @param qrCode the QR code raw string
     * @param prefixQrCode the string before all key/value pairs in qrCode
     * @param delimiter the string to split key/value pairs, can't contain a backslash
     * @return a list contains string of key/value (e.g. K:key1)
     */
    private List<String> getKeyValueList(String qrCode, String prefixQrCode,
                String delimiter) {
        String keyValueString = qrCode.substring(prefixQrCode.length());
        // Should not treat \delimiter as a delimiter
        String regex = "(?<!\\\\)" + Pattern.quote(delimiter);
        return Arrays.asList(keyValueString.split(regex));
    }

    private String getValueOrNull(List<String> keyValueList, String prefix) {
        for (String keyValue : keyValueList) {
            String strippedKeyValue = keyValue.stripLeading();
            if (strippedKeyValue.startsWith(prefix)) {
                return strippedKeyValue.substring(prefix.length());
            }
        }
        return null;
    }

    @VisibleForTesting
    String removeBackSlash(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean backSlash = false;
        for (char ch : input.toCharArray()) {
            if (ch != '\\') {
                sb.append(ch);
                backSlash = false;
            } else {
                if (backSlash) {
                    sb.append(ch);
                    backSlash = false;
                    continue;
                }
                backSlash = true;
            }
        }
        return sb.toString();
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
    public String getScheme() {
        return mScheme;
    }

    /** Available when {@code getScheme()} returns SCHEME_DPP */
    @VisibleForTesting
    String getPublicKey() {
        return mPublicKey;
    }

    /** May be available when {@code getScheme()} returns SCHEME_DPP */
    public String getInformation() {
        return mInformation;
    }

    /** Available when {@code getScheme()} returns SCHEME_ZXING_WIFI_NETWORK_CONFIG */
    WifiNetworkConfig getWifiNetworkConfig() {
        if (mAdbConfig == null) {
            return null;
        }
        return new WifiNetworkConfig(mAdbConfig);
    }
}
