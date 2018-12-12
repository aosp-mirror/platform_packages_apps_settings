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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.core.InstrumentedActivity;
import com.android.settings.R;

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
 * For intent action {@code ACTION_CONFIGURATOR_CHOOSE_SAVED_WIFI_NETWORK}, specify Wi-Fi (DPP)
 * QR code in {@code WifiDppUtils.EXTRA_QR_CODE}
 */
public class WifiDppConfiguratorActivity extends InstrumentedActivity implements
        WifiNetworkConfig.Retriever,
        WifiDppQrCodeGeneratorFragment.OnQrCodeGeneratorFragmentAddButtonClickedListener {
    private static final String TAG = "WifiDppConfiguratorActivity";

    public static final String ACTION_CONFIGURATOR_QR_CODE_SCANNER =
            "android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_SCANNER";
    public static final String ACTION_CONFIGURATOR_QR_CODE_GENERATOR =
            "android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR";
    public static final String ACTION_CONFIGURATOR_CHOOSE_SAVED_WIFI_NETWORK =
            "android.settings.WIFI_DPP_CONFIGURATOR_CHOOSE_SAVED_WIFI_NETWORK";

    private FragmentManager mFragmentManager;

    /** The Wi-Fi network which will be configured */
    private WifiNetworkConfig mWifiNetworkConfig;

    @Override
    public int getMetricsCategory() {
        //TODO:Should we use a new metrics category for Wi-Fi DPP?
        return MetricsProto.MetricsEvent.WIFI_NETWORK_DETAILS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.wifi_dpp_activity);
        mFragmentManager = getSupportFragmentManager();

        handleIntent(getIntent());

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void handleIntent(Intent intent) {
        boolean cancelActivity = false;
        WifiNetworkConfig config;
        switch (intent.getAction()) {
            case ACTION_CONFIGURATOR_QR_CODE_SCANNER:
                config = WifiNetworkConfig.getValidConfigOrNull(intent);
                if (config == null) {
                    cancelActivity = true;
                } else {
                    mWifiNetworkConfig = config;
                    showQrCodeScannerFragment(/* addToBackStack= */ false);
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
            case ACTION_CONFIGURATOR_CHOOSE_SAVED_WIFI_NETWORK:
                showChooseSavedWifiNetworkFragment(/* addToBackStack */ false);
                break;
            default:
                cancelActivity = true;
                Log.e(TAG, "Launch with an invalid action");
        }

        if (cancelActivity) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void showQrCodeScannerFragment(boolean addToBackStack) {
        // Avoid to replace the same fragment during configuration change
        if (mFragmentManager.findFragmentByTag(WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER) != null) {
            return;
        }

        WifiDppQrCodeScannerFragment fragment = new WifiDppQrCodeScannerFragment();
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(/* name */ null);
        }
        fragmentTransaction.commit();
    }

    private void showQrCodeGeneratorFragment() {
        // Avoid to replace the same fragment during configuration change
        if (mFragmentManager.findFragmentByTag(
                WifiDppUtils.TAG_FRAGMENT_QR_CODE_GENERATOR) != null) {
            return;
        }

        WifiDppQrCodeGeneratorFragment fragment = new WifiDppQrCodeGeneratorFragment();
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_QR_CODE_GENERATOR);
        fragmentTransaction.commit();
    }

    private void showChooseSavedWifiNetworkFragment(boolean addToBackStack) {
        // Avoid to replace the same fragment during configuration change
        if (mFragmentManager.findFragmentByTag(
                WifiDppUtils.TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK) != null) {
            return;
        }

        WifiDppChooseSavedWifiNetworkFragment fragment =
                new WifiDppChooseSavedWifiNetworkFragment();
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(/* name */ null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public WifiNetworkConfig getWifiNetworkConfig() {
        return mWifiNetworkConfig;
    }

    @Override
    public boolean setWifiNetworkConfig(WifiNetworkConfig config) {
        if(!WifiNetworkConfig.isValidConfig(config)) {
            return false;
        } else {
            mWifiNetworkConfig = new WifiNetworkConfig(config);
            return true;
        }
    }

    @Override
    public boolean onNavigateUp(){
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment instanceof WifiDppQrCodeGeneratorFragment) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        } else if (fragment instanceof WifiDppQrCodeScannerFragment) {
            mFragmentManager.popBackStackImmediate();
        }

        return false;
    }

    @Override public void onQrCodeGeneratorFragmentAddButtonClicked() {
        showQrCodeScannerFragment(/* addToBackStack */ true);
    }
}
