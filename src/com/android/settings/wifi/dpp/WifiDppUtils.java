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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;

import com.android.settingslib.wifi.AccessPoint;

import java.util.List;

/**
 * Here are the items shared by both WifiDppConfiguratorActivity & WifiDppEnrolleeActivity
 */
public class WifiDppUtils {
    /**
     * The fragment tag specified to FragmentManager for container activities to manage fragments.
     */
    public static final String TAG_FRAGMENT_QR_CODE_SCANNER = "qr_code_scanner_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    public static final String TAG_FRAGMENT_QR_CODE_GENERATOR = "qr_code_generator_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    public static final String TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK =
            "choose_saved_wifi_network_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    public static final String TAG_FRAGMENT_ADD_DEVICE = "add_device_fragment";

    /** The data is from {@code com.android.settingslib.wifi.AccessPoint.securityToString} */
    public static final String EXTRA_WIFI_SECURITY = "security";

    /** The data corresponding to {@code WifiConfiguration} SSID */
    public static final String EXTRA_WIFI_SSID = "ssid";

    /** The data corresponding to {@code WifiConfiguration} preSharedKey */
    public static final String EXTRA_WIFI_PRE_SHARED_KEY = "preSharedKey";

    /** The data corresponding to {@code WifiConfiguration} hiddenSSID */
    public static final String EXTRA_WIFI_HIDDEN_SSID = "hiddenSsid";

    /** @see WifiQrCode */
    public static final String EXTRA_QR_CODE = "qrCode";

    /**
     * Returns whether the user can share the network represented by this preference with QR code.
     */
    public static boolean isSharingNetworkEnabled(Context context) {
        return FeatureFlagUtils.isEnabled(context,
                com.android.settings.core.FeatureFlags.WIFI_SHARING);
    }

    /**
     * Returns an intent to launch QR code scanner for Wi-Fi DPP enrollee.
     *
     * @param ssid The data corresponding to {@code WifiConfiguration} SSID
     * @return Intent for launching QR code scanner
     */
    public static Intent getEnrolleeQrCodeScannerIntent(String ssid) {
        final Intent intent = new Intent(
                WifiDppEnrolleeActivity.ACTION_ENROLLEE_QR_CODE_SCANNER);
        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        return intent;
    }

    private static String getPresharedKey(WifiManager wifiManager, WifiConfiguration config) {
        String preSharedKey = config.preSharedKey;

        final List<WifiConfiguration> wifiConfigs = wifiManager.getPrivilegedConfiguredNetworks();
        for (WifiConfiguration wifiConfig : wifiConfigs) {
            if (wifiConfig.networkId == config.networkId) {
                preSharedKey = wifiConfig.preSharedKey;
                break;
            }
        }

        return preSharedKey;
    }

    private static String removeFirstAndLastDoubleQuotes(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        int begin = 0;
        int end = str.length() - 1;
        if (str.charAt(begin) == '\"') {
            begin++;
        }
        if (str.charAt(end) == '\"') {
            end--;
        }
        return str.substring(begin, end+1);
    }

    private static String getSecurityString(AccessPoint accessPoint) {
        switch(accessPoint.getSecurity()) {
            case AccessPoint.SECURITY_WEP:
                return "WEP";
            case AccessPoint.SECURITY_PSK:
                return "WPA";
            default:
                return "nopass";
        }
    }

    /**
     * Returns an intent to launch QR code generator.
     *
     * @param context     The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param accessPoint An instance of {@link AccessPoint}
     * @return Intent for launching QR code generator
     */
    public static Intent getConfiguratorQrCodeGeneratorIntent(Context context,
            WifiManager wifiManager, AccessPoint accessPoint) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);

        final WifiConfiguration wifiConfig = accessPoint.getConfig();
        final String ssid = removeFirstAndLastDoubleQuotes(wifiConfig.SSID);
        final String security = getSecurityString(accessPoint);
        String preSharedKey = wifiConfig.preSharedKey;

        if (preSharedKey != null) {
            // When the value of this key is read, the actual key is not returned, just a "*".
            // Call privileged system API to obtain actual key.
            preSharedKey = removeFirstAndLastDoubleQuotes(getPresharedKey(wifiManager, wifiConfig));
        }

        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        if (!TextUtils.isEmpty(security)) {
            intent.putExtra(EXTRA_WIFI_SECURITY, security);
        }
        if (!TextUtils.isEmpty(preSharedKey)) {
            intent.putExtra(EXTRA_WIFI_PRE_SHARED_KEY, preSharedKey);
        }

        return intent;
    }
}
