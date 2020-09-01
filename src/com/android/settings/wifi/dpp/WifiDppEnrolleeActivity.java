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
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;

/**
 * To provision "this" device with specified Wi-Fi network.
 *
 * To use intent action {@code ACTION_ENROLLEE_QR_CODE_SCANNER}, specify the SSID string of the
 * Wi-Fi network to be provisioned in {@code WifiDppUtils.EXTRA_WIFI_SSID}.
 */
public class WifiDppEnrolleeActivity extends WifiDppBaseActivity implements
        WifiDppQrCodeScannerFragment.OnScanWifiDppSuccessListener {
    private static final String TAG = "WifiDppEnrolleeActivity";

    static final String ACTION_ENROLLEE_QR_CODE_SCANNER =
            "android.settings.WIFI_DPP_ENROLLEE_QR_CODE_SCANNER";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_ENROLLEE;
    }

    @Override
    protected void handleIntent(Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null) {
            finish();
            return;
        }

        switch (action) {
            case ACTION_ENROLLEE_QR_CODE_SCANNER:
                String ssid = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SSID);
                showQrCodeScannerFragment(ssid);
                break;
            default:
                Log.e(TAG, "Launch with an invalid action");
                finish();
        }
    }

    private void showQrCodeScannerFragment(String ssid) {
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
        fragmentTransaction.commit();
    }

    @Override
    public void onScanWifiDppSuccess(WifiQrCode wifiQrCode) {
        // Do nothing
    }
}
