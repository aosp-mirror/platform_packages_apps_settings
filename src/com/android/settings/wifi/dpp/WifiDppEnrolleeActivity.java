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
 * To provision "this" device with specified Wi-Fi network.
 *
 * To use intent action {@code ACTION_ENROLLEE_QR_CODE_SCANNER}, specify the SSID string of the
 * Wi-Fi network to be provisioned in {@code WifiDppUtils.EXTRA_WIFI_SSID}.
 */
public class WifiDppEnrolleeActivity extends InstrumentedActivity {
    private static final String TAG = "WifiDppEnrolleeActivity";

    public static final String ACTION_ENROLLEE_QR_CODE_SCANNER =
            "android.settings.WIFI_DPP_ENROLLEE_QR_CODE_SCANNER";

    private FragmentManager mFragmentManager;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_WIFI_DPP_ENROLLEE;
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
        switch (intent.getAction()) {
            case ACTION_ENROLLEE_QR_CODE_SCANNER:
                String ssid = intent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SSID);
                showQrCodeScannerFragment(/* addToBackStack */ false, ssid);
                break;
            default:
                Log.e(TAG, "Launch with an invalid action");
                setResult(Activity.RESULT_CANCELED);
                finish();
        }
    }

    private void showQrCodeScannerFragment(boolean addToBackStack, String ssid) {
        // Avoid to replace the same fragment during configuration change
        if (mFragmentManager.findFragmentByTag(WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER) != null) {
            return;
        }

        WifiDppQrCodeScannerFragment fragment = new WifiDppQrCodeScannerFragment(ssid);
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment,
                WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(/* name */ null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public boolean onNavigateUp(){
        setResult(Activity.RESULT_CANCELED);
        finish();
        return true;
    }
}
