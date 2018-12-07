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

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;

/**
 * Here are the items shared by both WifiDppConfiguratorActivity & WifiDppEnrolleeActivity
 */
public class WifiDppUtils {
    /** The data is from {@code com.android.settingslib.wifi.AccessPoint.securityToString} */
    public static final String EXTRA_WIFI_SECURITY = "security";

    /** The data corresponding to {@code WifiConfiguration} SSID */
    public static final String EXTRA_WIFI_SSID = "ssid";

    /** The data corresponding to {@code WifiConfiguration} preSharedKey */
    public static final String EXTRA_WIFI_PRE_SHARED_KEY = "preSharedKey";

    /** The data corresponding to {@code WifiConfiguration} hiddenSSID */
    public static final String EXTRA_WIFI_HIDDEN_SSID = "hiddenSsid";

    /**
     * Acceptable QR code string may be a standard W-Fi DPP bootstrapping information or the Wi-Fi
     * Network config format described in
     * https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11
     *
     * Wi-Fi Network config format example:
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
     */
    public static final String EXTRA_QR_CODE = "qrCode";

    /**
     * Returns whether the user can share the network represented by this preference with QR code.
     */
    public static boolean isSharingNetworkEnabled(Context context) {
        return FeatureFlagUtils.isEnabled(context,
                com.android.settings.core.FeatureFlags.WIFI_SHARING);
    }

    /**
     * Returns an intent to launch QR code scanner.
     *
     * @param ssid The data corresponding to {@code WifiConfiguration} SSID
     * @return Intent for launching QR code scanner
     */
    public static Intent getConfiguratorQRCodeScannerIntent(String ssid) {
        final Intent intent = new Intent(
                WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        return intent;
    }

    /**
     * Returns an intent to launch QR code generator.
     *
     * @param ssid     The data corresponding to {@code WifiConfiguration} SSID
     * @param Security The data is from {@code AccessPoint.securityToString}
     * @return Intent for launching QR code generator
     */
    public static Intent getConfiguratorQRCodeGeneratorIntent(String ssid, String Security) {
        final Intent intent = new Intent(
                WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        if (!TextUtils.isEmpty(Security)) {
            intent.putExtra(EXTRA_WIFI_SECURITY, Security);
        }
        return intent;
    }
}
