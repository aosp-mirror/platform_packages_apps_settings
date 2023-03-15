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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;

import java.util.List;

/**
 * To provision "other" device with specified Wi-Fi network.
 *
 * Uses different intents to specify different provisioning ways.
 *
 * For intent action {@code ACTION_CONFIGURATOR_QR_CODE_SCANNER} and
 * {@code android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR}, specify the Wi-Fi network to be
 * provisioned in:
 *
 * {@code WifiDppUtils.EXTRA_WIFI_SECURITY}
 * {@code WifiDppUtils.EXTRA_WIFI_SSID}
 * {@code WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY}
 * {@code WifiDppUtils.EXTRA_WIFI_HIDDEN_SSID}
 *
 * For intent action {@link Settings#ACTION_PROCESS_WIFI_EASY_CONNECT_URI}, specify Wi-Fi
 * Easy Connect bootstrapping information string in Intent's data URI.
 */
public class WifiDppConfiguratorActivity extends WifiDppBaseActivity implements
        WifiNetworkConfig.Retriever,
        WifiDppQrCodeScannerFragment.OnScanWifiDppSuccessListener,
        WifiDppAddDeviceFragment.OnClickChooseDifferentNetworkListener,
        WifiNetworkListFragment.OnChooseNetworkListener {

    private static final String TAG = "WifiDppConfiguratorActivity";

    static final String ACTION_CONFIGURATOR_QR_CODE_SCANNER =
            "android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_SCANNER";
    static final String ACTION_CONFIGURATOR_QR_CODE_GENERATOR =
            "android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR";

    // Key for Bundle usage
    private static final String KEY_QR_CODE = "key_qr_code";
    private static final String KEY_WIFI_SECURITY = "key_wifi_security";
    private static final String KEY_WIFI_SSID = "key_wifi_ssid";
    private static final String KEY_WIFI_PRESHARED_KEY = "key_wifi_preshared_key";
    private static final String KEY_WIFI_HIDDEN_SSID = "key_wifi_hidden_ssid";
    private static final String KEY_WIFI_NETWORK_ID = "key_wifi_network_id";
    private static final String KEY_IS_HOTSPOT = "key_is_hotspot";

    /** The Wi-Fi network which will be configured */
    private WifiNetworkConfig mWifiNetworkConfig;

    /** The Wi-Fi DPP QR code from intent ACTION_PROCESS_WIFI_EASY_CONNECT_URI */
    private WifiQrCode mWifiDppQrCode;

    /**
     * The remote device's band support obtained as an (optional) extra
     * EXTRA_EASY_CONNECT_BAND_LIST from the intent ACTION_PROCESS_WIFI_EASY_CONNECT_URI.
     *
     * The band support is provided as IEEE 802.11 Global Operating Classes. There may be a single
     * or multiple operating classes specified. The array may also be a null if the extra wasn't
     * specified.
     */
    private int[] mWifiDppRemoteBandSupport;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        if (savedInstanceState != null) {
            String qrCode = savedInstanceState.getString(KEY_QR_CODE);

            mWifiDppQrCode = WifiQrCode.getValidWifiDppQrCodeOrNull(qrCode);

            final String security = savedInstanceState.getString(KEY_WIFI_SECURITY);
            final String ssid = savedInstanceState.getString(KEY_WIFI_SSID);
            final String preSharedKey = savedInstanceState.getString(KEY_WIFI_PRESHARED_KEY);
            final boolean hiddenSsid = savedInstanceState.getBoolean(KEY_WIFI_HIDDEN_SSID);
            final int networkId = savedInstanceState.getInt(KEY_WIFI_NETWORK_ID);
            final boolean isHotspot = savedInstanceState.getBoolean(KEY_IS_HOTSPOT);

            mWifiNetworkConfig = WifiNetworkConfig.getValidConfigOrNull(security, ssid,
                    preSharedKey, hiddenSsid, networkId, isHotspot);
        }
    }

    @Override
    protected void handleIntent(Intent intent) {
        if (isGuestUser(getApplicationContext())) {
            Log.e(TAG, "Guest user is not allowed to configure Wi-Fi!");
            EventLog.writeEvent(0x534e4554, "224772890", -1 /* UID */, "User is a guest");
            finish();
            return;
        }

        String action = intent != null ? intent.getAction() : null;
        if (action == null) {
            finish();
            return;
        }

        boolean cancelActivity = false;
        WifiNetworkConfig config;
        switch (action) {
            case ACTION_CONFIGURATOR_QR_CODE_SCANNER:
                config = WifiNetworkConfig.getValidConfigOrNull(intent);
                if (config == null) {
                    cancelActivity = true;
                } else {
                    mWifiNetworkConfig = config;
                    showQrCodeScannerFragment();
                }
                break;
            case ACTION_CONFIGURATOR_QR_CODE_GENERATOR:
                config = WifiNetworkConfig.getValidConfigOrNull(intent);
                if (config == null) {
                    cancelActivity = true;
                } else {
                    mWifiNetworkConfig = config;
                    showQrCodeGeneratorFragment();
                }
                break;
            case Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI:
                WifiDppUtils.showLockScreen(this,
                        () -> handleActionProcessWifiEasyConnectUriIntent(intent));
                break;
            default:
                cancelActivity = true;
                Log.e(TAG, "Launch with an invalid action");
        }

        if (cancelActivity) {
            finish();
        }
    }

    private void handleActionProcessWifiEasyConnectUriIntent(Intent intent) {
        final Uri uri = intent.getData();
        final String uriString = (uri == null) ? null : uri.toString();
        mWifiDppQrCode = WifiQrCode.getValidWifiDppQrCodeOrNull(uriString);
        mWifiDppRemoteBandSupport = intent.getIntArrayExtra(
                Settings.EXTRA_EASY_CONNECT_BAND_LIST); // returns null if none
        final boolean isDppSupported = WifiDppUtils.isWifiDppEnabled(this);
        if (!isDppSupported) {
            Log.e(TAG,
                    "ACTION_PROCESS_WIFI_EASY_CONNECT_URI for a device that doesn't "
                            + "support Wifi DPP - use WifiManager#isEasyConnectSupported");
        }
        if (mWifiDppQrCode == null) {
            Log.e(TAG, "ACTION_PROCESS_WIFI_EASY_CONNECT_URI with null URI!");
        }
        if (mWifiDppQrCode == null || !isDppSupported) {
            finish();
        } else {
            final WifiNetworkConfig connectedConfig = getConnectedWifiNetworkConfigOrNull();
            if (connectedConfig == null || !connectedConfig.isSupportWifiDpp(this)) {
                showChooseSavedWifiNetworkFragment(/* addToBackStack */ false);
            } else {
                mWifiNetworkConfig = connectedConfig;
                showAddDeviceFragment(/* addToBackStack */ false);
            }
        }
    }

    @VisibleForTesting
    void showQrCodeScannerFragment() {
        WifiDppQrCodeScannerFragment fragment =
                (WifiDppQrCodeScannerFragment) mFragmentManager.findFragmentByTag(
                        WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER);

        if (fragment == null) {
            fragment = new WifiDppQrCodeScannerFragment();
        } else {
            if (fragment.isVisible()) {
                return;
            }

            // When the fragment in back stack but not on top of the stack, we can simply pop
            // stack because current fragment transactions are arranged in an order
            mFragmentManager.popBackStackImmediate();
            return;
        }
        final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER);
        fragmentTransaction.commit();
    }

    private void showQrCodeGeneratorFragment() {
        WifiDppQrCodeGeneratorFragment fragment =
                (WifiDppQrCodeGeneratorFragment) mFragmentManager.findFragmentByTag(
                        WifiDppUtils.TAG_FRAGMENT_QR_CODE_GENERATOR);

        if (fragment == null) {
            fragment = new WifiDppQrCodeGeneratorFragment();
        } else {
            if (fragment.isVisible()) {
                return;
            }

            // When the fragment in back stack but not on top of the stack, we can simply pop
            // stack because current fragment transactions are arranged in an order
            mFragmentManager.popBackStackImmediate();
            return;
        }
        final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_QR_CODE_GENERATOR);
        fragmentTransaction.commit();
    }

    private void showChooseSavedWifiNetworkFragment(boolean addToBackStack) {
        WifiDppChooseSavedWifiNetworkFragment fragment =
                (WifiDppChooseSavedWifiNetworkFragment) mFragmentManager.findFragmentByTag(
                        WifiDppUtils.TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK);

        if (fragment == null) {
            fragment = new WifiDppChooseSavedWifiNetworkFragment();
        } else {
            if (fragment.isVisible()) {
                return;
            }

            // When the fragment in back stack but not on top of the stack, we can simply pop
            // stack because current fragment transactions are arranged in an order
            mFragmentManager.popBackStackImmediate();
            return;
        }
        final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(/* name */ null);
        }
        fragmentTransaction.commit();
    }

    private void showAddDeviceFragment(boolean addToBackStack) {
        WifiDppAddDeviceFragment fragment =
                (WifiDppAddDeviceFragment) mFragmentManager.findFragmentByTag(
                        WifiDppUtils.TAG_FRAGMENT_ADD_DEVICE);

        if (fragment == null) {
            fragment = new WifiDppAddDeviceFragment();
        } else {
            if (fragment.isVisible()) {
                return;
            }

            // When the fragment in back stack but not on top of the stack, we can simply pop
            // stack because current fragment transactions are arranged in an order
            mFragmentManager.popBackStackImmediate();
            return;
        }
        final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_ADD_DEVICE);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(/* name */ null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public WifiNetworkConfig getWifiNetworkConfig() {
        return mWifiNetworkConfig;
    }

    WifiQrCode getWifiDppQrCode() {
        return mWifiDppQrCode;
    }

    @VisibleForTesting
    boolean setWifiNetworkConfig(WifiNetworkConfig config) {
        if(!WifiNetworkConfig.isValidConfig(config)) {
            return false;
        } else {
            mWifiNetworkConfig = new WifiNetworkConfig(config);
            return true;
        }
    }

    @VisibleForTesting
    boolean setWifiDppQrCode(WifiQrCode wifiQrCode) {
        if (wifiQrCode == null) {
            return false;
        }

        if (!WifiQrCode.SCHEME_DPP.equals(wifiQrCode.getScheme())) {
            return false;
        }

        mWifiDppQrCode = new WifiQrCode(wifiQrCode.getQrCode());
        return true;
    }

    @Override
    public void onScanWifiDppSuccess(WifiQrCode wifiQrCode) {
        mWifiDppQrCode = wifiQrCode;

        showAddDeviceFragment(/* addToBackStack */ true);
    }

    @Override
    public void onClickChooseDifferentNetwork() {
        showChooseSavedWifiNetworkFragment(/* addToBackStack */ true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mWifiDppQrCode != null) {
            outState.putString(KEY_QR_CODE, mWifiDppQrCode.getQrCode());
        }

        if (mWifiNetworkConfig != null) {
            outState.putString(KEY_WIFI_SECURITY, mWifiNetworkConfig.getSecurity());
            outState.putString(KEY_WIFI_SSID, mWifiNetworkConfig.getSsid());
            outState.putString(KEY_WIFI_PRESHARED_KEY, mWifiNetworkConfig.getPreSharedKey());
            outState.putBoolean(KEY_WIFI_HIDDEN_SSID, mWifiNetworkConfig.getHiddenSsid());
            outState.putInt(KEY_WIFI_NETWORK_ID, mWifiNetworkConfig.getNetworkId());
            outState.putBoolean(KEY_IS_HOTSPOT, mWifiNetworkConfig.isHotspot());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onChooseNetwork(WifiNetworkConfig wifiNetworkConfig) {
        mWifiNetworkConfig = new WifiNetworkConfig(wifiNetworkConfig);

        showAddDeviceFragment(/* addToBackStack */ true);
    }

    private WifiNetworkConfig getConnectedWifiNetworkConfigOrNull() {
        final WifiManager wifiManager = getSystemService(WifiManager.class);
        if (!wifiManager.isWifiEnabled()) {
            return null;
        }

        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo == null) {
            return null;
        }

        final int connectionNetworkId = connectionInfo.getNetworkId();
        final List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : configs) {
            if (wifiConfiguration.networkId == connectionNetworkId) {
                return WifiNetworkConfig.getValidConfigOrNull(
                    WifiDppUtils.getSecurityString(wifiConfiguration),
                    wifiConfiguration.getPrintableSsid(),
                    wifiConfiguration.preSharedKey,
                    wifiConfiguration.hiddenSSID,
                    wifiConfiguration.networkId,
                    /* isHotspot */ false);
            }
        }

        return null;
    }

    private static boolean isGuestUser(Context context) {
        if (context == null) return false;
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) return false;
        return userManager.isGuestUser();
    }
}
