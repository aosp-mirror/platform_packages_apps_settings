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
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settings.core.InstrumentedActivity;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * To provision "this" device with specified Wi-Fi network.
 *
 * To use intent action {@code ACTION_ENROLLEE_QR_CODE_SCANNER}, specify the SSID string of the
 * Wi-Fi network to be provisioned in {@code WifiDppUtils.EXTRA_WIFI_SSID}.
 */
public class WifiDppEnrolleeActivity extends InstrumentedActivity implements
        WifiDppQrCodeScannerFragment.OnScanWifiDppSuccessListener {
    private static final String TAG = "WifiDppEnrolleeActivity";

    public static final String ACTION_ENROLLEE_QR_CODE_SCANNER =
            "android.settings.WIFI_DPP_ENROLLEE_QR_CODE_SCANNER";

    private FragmentManager mFragmentManager;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_ENROLLEE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            setTheme(R.style.LightTheme_SettingsBase_SetupWizard);
        }

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
        // Do nothing
    }
}
