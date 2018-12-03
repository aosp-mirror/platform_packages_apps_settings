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

public class WifiDppConfiguratorActivity extends InstrumentedActivity implements
        WifiNetworkConfig.Retriever {
    private static final String TAG = "WifiDppConfiguratorActivity";

    public static final String ACTION_CONFIGURATOR_QR_CODE_SCANNER =
            "android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_SCANNER";
    public static final String ACTION_CONFIGURATOR_QR_CODE_GENERATOR =
            "android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR";
    public static final String ACTION_CONFIGURATOR_CHOOSE_SAVED_WIFI_NETWORK =
            "android.settings.WIFI_DPP_CONFIGURATOR_CHOOSE_SAVED_WIFI_NETWORK";

    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;

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
        mFragmentTransaction = getSupportFragmentManager().beginTransaction();

        Intent intent = getIntent();
        boolean cancelActivity = false;
        WifiNetworkConfig config;
        switch (intent.getAction()) {
            case ACTION_CONFIGURATOR_QR_CODE_SCANNER:
                config = WifiNetworkConfig.getValidConfigOrNull(intent);
                if (config == null) {
                    cancelActivity = true;
                } else {
                    mWifiNetworkConfig = config;
                    addQrCodeScannerFragment(/* addToBackStack= */ false);
                }
                break;
            case ACTION_CONFIGURATOR_QR_CODE_GENERATOR:
                config = WifiNetworkConfig.getValidConfigOrNull(intent);
                if (config == null) {
                    cancelActivity = true;
                } else {
                    mWifiNetworkConfig = config;
                    addQrCodeGeneratorFragment();
                }
                break;
            case ACTION_CONFIGURATOR_CHOOSE_SAVED_WIFI_NETWORK:
                addChooseSavedWifiNetworkFragment(/* addToBackStack */ false);
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

    private void addQrCodeScannerFragment(boolean addToBackStack) {
        WifiDppQrCodeScannerFragment fragment = new WifiDppQrCodeScannerFragment();
        mFragmentTransaction.add(R.id.fragment_container, fragment);
        if (addToBackStack) {
            mFragmentTransaction.addToBackStack(/* name */ null);
        }
        mFragmentTransaction.commit();
    }

    private void addQrCodeGeneratorFragment() {
        WifiDppQrCodeGeneratorFragment fragment = new WifiDppQrCodeGeneratorFragment();
        mFragmentTransaction.add(R.id.fragment_container, fragment);
        mFragmentTransaction.commit();
    }

    private void addChooseSavedWifiNetworkFragment(boolean addToBackStack) {
        ActionBar action = getActionBar();
        if (action != null) {
            action.hide();
        }

        WifiDppChooseSavedWifiNetworkFragment fragment =
                new WifiDppChooseSavedWifiNetworkFragment();
        mFragmentTransaction.add(R.id.fragment_container, fragment);
        if (addToBackStack) {
            mFragmentTransaction.addToBackStack(/* name */ null);
        }
        mFragmentTransaction.commit();
    }

    @Override
    protected void onStop() {
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            // Remove it to prevent stacking multiple fragments after screen rotated.
            mFragmentManager.beginTransaction().remove(fragment).commit();
        }

        super.onStop();
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
}
