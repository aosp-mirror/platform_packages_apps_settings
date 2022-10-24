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
import android.net.TetheringManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.wifitrackerlib.WifiEntry;

import java.nio.charset.StandardCharsets;

/** A utility class for Wi-Fi functions. */
public class WifiUtils extends com.android.settingslib.wifi.WifiUtils {

    static final String TAG = "WifiUtils";

    private static final int SSID_ASCII_MIN_LENGTH = 1;
    private static final int SSID_ASCII_MAX_LENGTH = 32;

    private static final int PSK_PASSPHRASE_ASCII_MIN_LENGTH = 8;
    private static final int PSK_PASSPHRASE_ASCII_MAX_LENGTH = 63;

    private static Boolean sCanShowWifiHotspotCached;

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
     * Check if the hotspot password is valid.
     */
    public static boolean isHotspotPasswordValid(String password, int securityType) {
        final SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder();
        try {
            if (securityType == SoftApConfiguration.SECURITY_TYPE_WPA2_PSK
                    || securityType == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION) {
                if (password.length() < PSK_PASSPHRASE_ASCII_MIN_LENGTH
                        || password.length() > PSK_PASSPHRASE_ASCII_MAX_LENGTH) {
                    return false;
                }
            }
            configBuilder.setPassphrase(password, securityType);
        } catch (Exception e) {
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
     * {@link ScanResult} or {@link WifiEntry}. Either {@code wifiEntry} or {@code scanResult
     * } input should be not null for retrieving information, otherwise will throw
     * IllegalArgumentException.
     * This method prefers to take {@link WifiEntry} input in priority. Therefore this method
     * will take {@link WifiEntry} input as preferred data extraction source when you input
     * both {@link WifiEntry} and {@link ScanResult}, and ignore {@link ScanResult} input.
     *
     * Duplicated and simplified method from {@link WifiConfigController#getConfig()}.
     * TODO(b/120827021): Should be removed if the there is have a common one in shared place (e.g.
     * SettingsLib).
     *
     * @param wifiEntry Input data for retrieving WifiConfiguration.
     * @param scanResult  Input data for retrieving WifiConfiguration.
     * @return WifiConfiguration obj based on input.
     */
    public static WifiConfiguration getWifiConfig(WifiEntry wifiEntry, ScanResult scanResult) {
        if (wifiEntry == null && scanResult == null) {
            throw new IllegalArgumentException(
                    "At least one of WifiEntry and ScanResult input is required.");
        }

        final WifiConfiguration config = new WifiConfiguration();
        final int security;

        if (wifiEntry == null) {
            config.SSID = "\"" + scanResult.SSID + "\"";
            security = getWifiEntrySecurity(scanResult);
        } else {
            if (wifiEntry.getWifiConfiguration() == null) {
                config.SSID = "\"" + wifiEntry.getSsid() + "\"";
            } else {
                config.networkId = wifiEntry.getWifiConfiguration().networkId;
                config.hiddenSSID = wifiEntry.getWifiConfiguration().hiddenSSID;
            }
            security = wifiEntry.getSecurity();
        }

        switch (security) {
            case WifiEntry.SECURITY_NONE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_OPEN);
                break;

            case WifiEntry.SECURITY_WEP:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_WEP);
                break;

            case WifiEntry.SECURITY_PSK:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_PSK);
                break;

            case WifiEntry.SECURITY_EAP_SUITE_B:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP_SUITE_B);
                break;

            case WifiEntry.SECURITY_EAP:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_EAP);
                break;

            case WifiEntry.SECURITY_SAE:
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
                break;

            case WifiEntry.SECURITY_OWE:
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
     * @param result ScanResult
     * @return Related security value based on {@link WifiEntry}.
     */
    public static int getWifiEntrySecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return WifiEntry.SECURITY_WEP;
        } else if (result.capabilities.contains("SAE")) {
            return WifiEntry.SECURITY_SAE;
        } else if (result.capabilities.contains("PSK")) {
            return WifiEntry.SECURITY_PSK;
        } else if (result.capabilities.contains("EAP_SUITE_B_192")) {
            return WifiEntry.SECURITY_EAP_SUITE_B;
        } else if (result.capabilities.contains("EAP")) {
            return WifiEntry.SECURITY_EAP;
        } else if (result.capabilities.contains("OWE")) {
            return WifiEntry.SECURITY_OWE;
        }

        return WifiEntry.SECURITY_NONE;
    }

    /**
     * Check if Wi-Fi hotspot settings can be displayed.
     *
     * @param context Context of caller
     * @return true if Wi-Fi hotspot settings can be displayed
     */
    public static boolean checkShowWifiHotspot(Context context) {
        if (context == null) return false;

        boolean showWifiHotspotSettings =
                context.getResources().getBoolean(R.bool.config_show_wifi_hotspot_settings);
        if (!showWifiHotspotSettings) {
            Log.w(TAG, "config_show_wifi_hotspot_settings:false");
            return false;
        }

        WifiManager wifiManager = context.getSystemService(WifiManager.class);
        if (wifiManager == null) {
            Log.e(TAG, "WifiManager is null");
            return false;
        }

        TetheringManager tetheringManager = context.getSystemService(TetheringManager.class);
        if (tetheringManager == null) {
            Log.e(TAG, "TetheringManager is null");
            return false;
        }
        String[] wifiRegexs = tetheringManager.getTetherableWifiRegexs();
        if (wifiRegexs == null || wifiRegexs.length == 0) {
            Log.w(TAG, "TetherableWifiRegexs is empty");
            return false;
        }
        return true;
    }

    /**
     * Return the cached result to see if Wi-Fi hotspot settings can be displayed.
     *
     * @param context Context of caller
     * @return true if Wi-Fi hotspot settings can be displayed
     */
    public static boolean canShowWifiHotspot(Context context) {
        if (sCanShowWifiHotspotCached == null) {
            sCanShowWifiHotspotCached = checkShowWifiHotspot(context);
        }
        return sCanShowWifiHotspotCached;
    }

    /**
     * Sets the sCanShowWifiHotspotCached for testing purposes.
     *
     * @param cached Cached value for #canShowWifiHotspot()
     */
    @VisibleForTesting
    public static void setCanShowWifiHotspotCached(Boolean cached) {
        sCanShowWifiHotspotCached = cached;
    }
}
