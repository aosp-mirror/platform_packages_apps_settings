/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.development;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settings.wifi.dpp.WifiDppBaseActivity;

/**
 * To scan an ADB QR code to pair a device.
 *
 * To use intent action {@code ACTION_ADB_QR_CODE_SCANNER}.
 */
public class AdbQrCodeActivity extends WifiDppBaseActivity {
    private static final String TAG = "AdbQrCodeActivity";

    static final String TAG_FRAGMENT_ADB_QR_CODE_SCANNER = "adb_qr_code_scanner_fragment";

    public static final String ACTION_ADB_QR_CODE_SCANNER =
            "android.settings.ADB_QR_CODE_SCANNER";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ADB_WIRELESS;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        AdbQrcodeScannerFragment fragment =
                (AdbQrcodeScannerFragment) mFragmentManager.findFragmentByTag(
                        TAG_FRAGMENT_ADB_QR_CODE_SCANNER);

        if (fragment == null) {
            fragment = new AdbQrcodeScannerFragment();
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
                TAG_FRAGMENT_ADB_QR_CODE_SCANNER);
        fragmentTransaction.commit();
    }

    @Override
    protected void handleIntent(Intent intent) {
    }
}
