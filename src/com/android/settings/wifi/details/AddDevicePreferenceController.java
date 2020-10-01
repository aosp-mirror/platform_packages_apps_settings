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

package com.android.settings.wifi.details;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.wifi.AccessPoint;

/**
 * {@link BasePreferenceController} that launches Wi-Fi Easy Connect configurator flow.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in
 * {@link com.android.settings.wifi.details2.AddDevicePreferenceController2}.
 */
public class AddDevicePreferenceController extends BasePreferenceController {

    private static final String TAG = "AddDevicePreferenceController";

    private static final String KEY_ADD_DEVICE = "add_device_to_network";

    private AccessPoint mAccessPoint;
    private WifiManager mWifiManager;

    public AddDevicePreferenceController(Context context) {
        super(context, KEY_ADD_DEVICE);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public AddDevicePreferenceController init(AccessPoint accessPoint) {
        mAccessPoint = accessPoint;

        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        if (WifiDppUtils.isSupportConfiguratorQrCodeScanner(mContext, mAccessPoint)) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_ADD_DEVICE.equals(preference.getKey())) {
            WifiDppUtils.showLockScreen(mContext, () -> launchWifiDppConfiguratorQrCodeScanner());
            return true; /* click is handled */
        }

        return false; /* click is not handled */
    }

    private void launchWifiDppConfiguratorQrCodeScanner() {
        final Intent intent = WifiDppUtils.getConfiguratorQrCodeScannerIntentOrNull(mContext,
                mWifiManager, mAccessPoint);

        if (intent == null) {
            Log.e(TAG, "Launch Wi-Fi QR code scanner with a wrong Wi-Fi network!");
        } else {
            mContext.startActivity(intent);
        }
    }
}
