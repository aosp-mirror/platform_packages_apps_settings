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

package com.android.settings.wifi;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiConfiguration;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.Utils;
import com.android.settingslib.wifi.AccessPoint;

import java.nio.charset.StandardCharsets;

public class WifiUtils {

    private static final int SSID_ASCII_MIN_LENGTH = 1;
    private static final int SSID_ASCII_MAX_LENGTH = 32;


    public static boolean isSSIDTooLong(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return false;
        }
        return ssid.getBytes(StandardCharsets.UTF_8).length > SSID_ASCII_MAX_LENGTH;
    }

    public static boolean isSSIDTooShort(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return true;
        }
        return ssid.length() < SSID_ASCII_MIN_LENGTH;
    }

    /**
     * Check if the WPA2-PSK hotspot password is valid.
     */
    public static boolean isHotspotWpa2PasswordValid(String password) {
        final SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder();
        try {
            configBuilder.setPassphrase(password, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * This method is a stripped and negated version of WifiConfigStore.canModifyNetwork.
     *
     * @param context Context of caller
     * @param config  The WiFi config.
     * @return true if Settings cannot modify the config due to lockDown.
     */
    public static boolean isNetworkLockedDown(Context context, WifiConfiguration config) {
        if (config == null) {
            return false;
        }

        final DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final PackageManager pm = context.getPackageManager();
        final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);

        // Check if device has DPM capability. If it has and dpm is still null, then we
        // treat this case with suspicion and bail out.
        if (pm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) && dpm == null) {
            return true;
        }

        boolean isConfigEligibleForLockdown = false;
        if (dpm != null) {
            final ComponentName deviceOwner = dpm.getDeviceOwnerComponentOnAnyUser();
            if (deviceOwner != null) {
                final int deviceOwnerUserId = dpm.getDeviceOwnerUserId();
                try {
                    final int deviceOwnerUid = pm.getPackageUidAsUser(deviceOwner.getPackageName(),
                            deviceOwnerUserId);
                    isConfigEligibleForLockdown = deviceOwnerUid == config.creatorUid;
                } catch (PackageManager.NameNotFoundException e) {
                    // don't care
                }
            } else if (dpm.isOrganizationOwnedDeviceWithManagedProfile()) {
                int profileOwnerUserId = Utils.getManagedProfileId(um, UserHandle.myUserId());
                final ComponentName profileOwner = dpm.getProfileOwnerAsUser(profileOwnerUserId);
                if (profileOwner != null) {
                    try {
                        final int profileOwnerUid = pm.getPackageUidAsUser(
                                profileOwner.getPackageName(), profileOwnerUserId);
                        isConfigEligibleForLockdown = profileOwnerUid == config.creatorUid;
                    } catch (PackageManager.NameNotFoundException e) {
                        // don't care
                    }
                }
            }
        }
        if (!isConfigEligibleForLockdown) {
            return false;
        }

        final ContentResolver resolver = context.getContentResolver();
        final boolean isLockdownFeatureEnabled = Settings.Global.getInt(resolver,
                Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 0) != 0;
        return isLockdownFeatureEnabled;
    }

    /** Returns true if the provided NetworkCapabilities indicate a captive portal network. */
    public static boolean canSignIntoNetwork(NetworkCapabilities capabilities) {
        return (capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL));
    }

    /**
     * Provides a simple way to generate a new {@link WifiConfiguration} obj from
     * {@link ScanResult} or {@link AccessPoint}. Either {@code accessPoint} or {@code scanResult
     * } input should be not null for retrieving information, otherwise will throw
     * IllegalArgumentException.
     * This method prefers to take {@link AccessPoint} input in priority. Therefore this method
     * will take {@link AccessPoint} input as preferred data extraction source when you input
     * both {@link AccessPoint} and {@link ScanResult}, and ignore {@link ScanResult} input.
     *
     * Duplicated and simplified method from {@link WifiConfigController#getConfig()}.
     * TODO(b/120827021): Should be removed if the there is have a common one in shared place (e.g.
     * SettingsLib).
     *
     * @param accessPoint Input data for retrieving WifiConfiguration.
     * @param scanResult  Input data for retrieving WifiConfiguration.
     * @return WifiConfiguration obj based on input.
     */
    public static WifiConfiguration getWifiConfig(AccessPoint accessPoint, ScanResult scanResult,
            String password) {
        if (accessPoint == null && scanResult == null) {
            throw new IllegalArgumentException(
                    "At least one of AccessPoint and ScanResult input is required.");
        }

        final WifiConfiguration config = new WifiConfiguration();
        final int security;

        if (accessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(scanResult.SSID);
            security = getAccessPointSecurity(scanResult);
        } else {
            if (!accessPoint.isSaved()) {
                config.SSID = AccessPoint.convertToQuotedString(
                        accessPoint.getSsidStr());
            } else {
                config.networkId = accessPoint.getConfig().networkId;
                config.hiddenSSID = accessPoint.getConfig().hiddenSSID;
            }
            security = accessPoint.getSecurity();
        }

        switch (security) {
            case AccessPoint.SECURITY_NONE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
                break;

            case AccessPoint.SECURITY_WEP:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WEP);
                if (!TextUtils.isEmpty(password)) {
                    int length = password.length();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58)
                            && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_PSK:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_PSK);
                if (!TextUtils.isEmpty(password)) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
            case AccessPoint.SECURITY_EAP_SUITE_B:
                if (security == AccessPoint.SECURITY_EAP_SUITE_B) {
                    // allowedSuiteBCiphers will be set according to certificate type
                    config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP_SUITE_B);
                } else {
                    config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP);
                }

                if (!TextUtils.isEmpty(password)) {
                    config.enterpriseConfig.setPassword(password);
                }
                break;
            case AccessPoint.SECURITY_SAE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
                if (!TextUtils.isEmpty(password)) {
                    config.preSharedKey = '"' + password + '"';
                }
                break;

            case AccessPoint.SECURITY_OWE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OWE);
                break;

            default:
                break;
        }

        return config;
    }


    /**
     * Gets security value from ScanResult.
     *
     * Duplicated method from {@link AccessPoint#getSecurity(ScanResult)}.
     * TODO(b/120827021): Should be removed if the there is have a common one in shared place (e.g.
     * SettingsLib).
     *
     * @param result ScanResult
     * @return Related security value based on {@link AccessPoint}.
     */
    public static int getAccessPointSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return AccessPoint.SECURITY_WEP;
        } else if (result.capabilities.contains("SAE")) {
            return AccessPoint.SECURITY_SAE;
        } else if (result.capabilities.contains("PSK")) {
            return AccessPoint.SECURITY_PSK;
        } else if (result.capabilities.contains("EAP_SUITE_B_192")) {
            return AccessPoint.SECURITY_EAP_SUITE_B;
        } else if (result.capabilities.contains("EAP")) {
            return AccessPoint.SECURITY_EAP;
        } else if (result.capabilities.contains("OWE")) {
            return AccessPoint.SECURITY_OWE;
        }

        return AccessPoint.SECURITY_NONE;
    }


    public static final int CONNECT_TYPE_OTHERS = 0;
    public static final int CONNECT_TYPE_OPEN_NETWORK = 1;
    public static final int CONNECT_TYPE_SAVED_NETWORK = 2;
    public static final int CONNECT_TYPE_OSU_PROVISION = 3;

    /**
     * Gets the connecting type of {@link AccessPoint}.
     */
    public static int getConnectingType(AccessPoint accessPoint) {
        final WifiConfiguration config = accessPoint.getConfig();
        if (accessPoint.isOsuProvider()) {
            return CONNECT_TYPE_OSU_PROVISION;
        } else if ((accessPoint.getSecurity() == AccessPoint.SECURITY_NONE) ||
                (accessPoint.getSecurity() == AccessPoint.SECURITY_OWE)) {
            return CONNECT_TYPE_OPEN_NETWORK;
        } else if (accessPoint.isSaved() && config != null
                && config.getNetworkSelectionStatus() != null
                && config.getNetworkSelectionStatus().hasEverConnected()) {
            return CONNECT_TYPE_SAVED_NETWORK;
        } else if (accessPoint.isPasspoint()) {
            // Access point provided by an installed Passpoint provider, connect using
            // the associated config.
            return CONNECT_TYPE_SAVED_NETWORK;
        } else {
            return CONNECT_TYPE_OTHERS;
        }
    }
}
