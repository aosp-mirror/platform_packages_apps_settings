/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.details2;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.wifitrackerlib.WifiEntry;

/**
 * {@link BasePreferenceController} that launches Wi-Fi Easy Connect configurator flow
 */
public class AddDevicePreferenceController2 extends BasePreferenceController {

    private static final String TAG = "AddDevicePreferenceController2";

    private static final String KEY_ADD_DEVICE = "add_device_to_network";

    private WifiEntry mWifiEntry;
    private WifiManager mWifiManager;

    public AddDevicePreferenceController2(Context context) {
        super(context, KEY_ADD_DEVICE);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void setWifiEntry(WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
    }

    @Override
    public int getAvailabilityStatus() {
        return mWifiEntry.canEasyConnect() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_ADD_DEVICE.equals(preference.getKey())) {
            WifiDppUtils.showLockScreenForWifiSharing(mContext,
                    () -> launchWifiDppConfiguratorQrCodeScanner());
            return true; /* click is handled */
        }

        return false; /* click is not handled */
    }

    private void launchWifiDppConfiguratorQrCodeScanner() {
        final Intent intent = WifiDppUtils.getConfiguratorQrCodeScannerIntentOrNull(mContext,
                mWifiManager, mWifiEntry);

        if (intent == null) {
            Log.e(TAG, "Launch Wi-Fi QR code scanner with a wrong Wi-Fi network!");
        } else {
            mContext.startActivity(intent);
        }
    }
}
