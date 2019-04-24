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

package com.android.settings.wifi.slice;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.android.settings.wifi.WifiConnectListener;
import com.android.settings.wifi.WifiDialogActivity;
import com.android.settings.wifi.WifiUtils;
import com.android.settingslib.wifi.AccessPoint;

/**
 * This activity helps connect to the Wi-Fi network which is open or saved
 */
public class ConnectToWifiHandler extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Network network = getIntent().getParcelableExtra(ConnectivityManager.EXTRA_NETWORK);
        final Bundle accessPointState = getIntent().getBundleExtra(
                WifiDialogActivity.KEY_ACCESS_POINT_STATE);

        if (network != null) {
            WifiScanWorker.clearClickedWifi();
            final ConnectivityManager cm = getSystemService(ConnectivityManager.class);
            // start captive portal app to sign in to network
            cm.startCaptivePortalApp(network);
        } else if (accessPointState != null) {
            connect(new AccessPoint(this, accessPointState));
        }

        finish();
    }

    @VisibleForTesting
    void connect(AccessPoint accessPoint) {
        WifiScanWorker.saveClickedWifi(accessPoint);

        final WifiConnectListener connectListener = new WifiConnectListener(this);
        switch (WifiUtils.getConnectingType(accessPoint)) {
            case WifiUtils.CONNECT_TYPE_OSU_PROVISION:
                accessPoint.startOsuProvisioning(connectListener);
                break;

            case WifiUtils.CONNECT_TYPE_OPEN_NETWORK:
                accessPoint.generateOpenNetworkConfig();

            case WifiUtils.CONNECT_TYPE_SAVED_NETWORK:
                final WifiManager wifiManager = getSystemService(WifiManager.class);
                wifiManager.connect(accessPoint.getConfig(), connectListener);
                break;
        }
    }
}
