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
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.core.InstrumentedActivity;
import com.android.settings.R;

public class WifiDppConfiguratorActivity extends InstrumentedActivity {
    private static final String TAG = "WifiDppConfiguratorActivity";

    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;

    public static final String EXTRA_LAUNCH_MODE =
            "com.android.settings.wifi.dpp.EXTRA_LAUNCH_MODE";
    public static final String EXTRA_SSID = "com.android.settings.wifi.dpp.EXTRA_SSID";

    public enum LaunchMode {
        LAUNCH_MODE_QR_CODE_SCANNER(1),
        LAUNCH_MODE_QR_CODE_GENERATOR(2),
        LAUNCH_MODE_CHOOSE_SAVED_WIFI_NETWORK(3),
        LAUNCH_MODE_NOT_DEFINED(-1);

        private int mMode;

        LaunchMode(int mode) {
            this.mMode = mode;
        }

        public int getMode() {
            return mMode;
        }
    }

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

        final int launchMode = getIntent().getIntExtra(EXTRA_LAUNCH_MODE,
            LaunchMode.LAUNCH_MODE_NOT_DEFINED.getMode());
        if (launchMode == LaunchMode.LAUNCH_MODE_QR_CODE_SCANNER.getMode()) {
            addQrCodeScannerFragment();
        } else if (launchMode == LaunchMode.LAUNCH_MODE_QR_CODE_GENERATOR.getMode()) {
            addQrCodeGeneratorFragment();
        } else if (launchMode == LaunchMode.LAUNCH_MODE_CHOOSE_SAVED_WIFI_NETWORK.getMode()) {
            addChooseSavedWifiNetworkFragment();
        } else {
            Log.e(TAG, "Launch with an invalid mode extra");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void addQrCodeScannerFragment() {
        final WifiDppQrCodeScannerFragment fragment = new WifiDppQrCodeScannerFragment();
        mFragmentTransaction.add(R.id.fragment_container, fragment);
        mFragmentTransaction.addToBackStack(/* name */ null);
        mFragmentTransaction.commit();
    }

    private void addQrCodeGeneratorFragment() {
        final WifiDppQrCodeGeneratorFragment fragment = new WifiDppQrCodeGeneratorFragment();
        mFragmentTransaction.add(R.id.fragment_container, fragment);
        mFragmentTransaction.addToBackStack(/* name */ null);
        mFragmentTransaction.commit();
    }

    private void addChooseSavedWifiNetworkFragment() {
        final ActionBar action = getActionBar();
        if (action != null) {
            action.hide();
        }

        WifiDppChooseSavedWifiNetworkFragment fragment =
                new WifiDppChooseSavedWifiNetworkFragment();
        mFragmentTransaction.add(R.id.fragment_container, fragment);
        mFragmentTransaction.addToBackStack(/* name */ null);
        mFragmentTransaction.commit();
    }

    @Override
    protected void onStop() {
        final Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            // Remove it to prevent stacking multiple fragments after screen rotated.
            mFragmentManager.beginTransaction().remove(fragment).commit();
        }

        super.onStop();
    }
}
