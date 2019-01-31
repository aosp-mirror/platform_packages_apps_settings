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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settings.core.InstrumentedActivity;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * To provision "this" device with specified Wi-Fi network.
 *
 * To use intent action {@code ACTION_ENROLLEE_QR_CODE_SCANNER}, specify the SSID string of the
 * Wi-Fi network to be provisioned in {@code WifiDppUtils.EXTRA_WIFI_SSID}.
 */
public class WifiDppEnrolleeActivity extends InstrumentedActivity implements
        WifiManager.ActionListener,
        WifiDppQrCodeScannerFragment.OnScanWifiDppSuccessListener,
        WifiDppQrCodeScannerFragment.OnScanZxingWifiFormatSuccessListener {
    private static final String TAG = "WifiDppEnrolleeActivity";

    public static final String ACTION_ENROLLEE_QR_CODE_SCANNER =
            "android.settings.WIFI_DPP_ENROLLEE_QR_CODE_SCANNER";

    private FragmentManager mFragmentManager;

    private class EasyConnectStatusCallback extends android.net.wifi.EasyConnectStatusCallback {
        @Override
        public void onEnrolleeSuccess(int newNetworkId) {
            // Connect to the new network.
            final WifiManager wifiManager = getSystemService(WifiManager.class);
            final List<WifiConfiguration> wifiConfigs =
                    wifiManager.getPrivilegedConfiguredNetworks();
            for (WifiConfiguration wifiConfig : wifiConfigs) {
                if (wifiConfig.networkId == newNetworkId) {
                    wifiManager.connect(wifiConfig, WifiDppEnrolleeActivity.this);
                    return;
                }
            }
            Log.e(TAG, "Invalid networkId " + newNetworkId);
            WifiDppEnrolleeActivity.this.onFailure(WifiManager.ERROR_AUTHENTICATING);
        }

        @Override
        public void onConfiguratorSuccess(int code) {
            // Do nothing
        }

        @Override
        public void onFailure(int code) {
            //TODO(b/122429170): Show DPP enrollee error state UI
            Log.d(TAG, "EasyConnectStatusCallback.onFailure " + code);
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_ENROLLEE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.wifi_dpp_activity);
        mFragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void handleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_ENROLLEE_QR_CODE_SCANNER:
                String ssid = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SSID);
                showQrCodeScannerFragment(/* addToBackStack */ false, ssid);
                break;
            default:
                Log.e(TAG, "Launch with an invalid action");
                finish();
        }
    }

    private void showQrCodeScannerFragment(boolean addToBackStack, String ssid) {
        WifiDppQrCodeScannerFragment fragment =
                (WifiDppQrCodeScannerFragment) mFragmentManager.findFragmentByTag(
                        WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER);

        if (fragment == null) {
            fragment = new WifiDppQrCodeScannerFragment(ssid);
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
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(/* name */ null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public boolean onNavigateUp(){
        finish();
        return true;
    }

    @Override
    public void onScanWifiDppSuccess(WifiQrCode wifiQrCode) {
        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.startEasyConnectAsEnrolleeInitiator(wifiQrCode.getQrCode(), getMainExecutor(),
                new EasyConnectStatusCallback());
    }

    @Override
    public void onScanZxingWifiFormatSuccess(WifiNetworkConfig wifiNetworkConfig) {
        wifiNetworkConfig.connect(/* context */ this, /* listener */ this);
    }

    @Override
    public void onSuccess() {
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onFailure(int reason) {
        Log.d(TAG, "Wi-Fi connect onFailure reason - " + reason);

        final Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment instanceof WifiDppQrCodeScannerFragment) {
            ((WifiDppQrCodeScannerFragment)fragment).showErrorMessage(true);
        }
    }
}
