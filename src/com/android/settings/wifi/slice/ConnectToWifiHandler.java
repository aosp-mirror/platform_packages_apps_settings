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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
 * This receiver helps connect to Wi-Fi network
 */
public class ConnectToWifiHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        final Network network = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK);
        final Bundle accessPointState = intent.getBundleExtra(
                WifiDialogActivity.KEY_ACCESS_POINT_STATE);

        if (network != null) {
            WifiScanWorker.clearClickedWifi();
            final ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
            // start captive portal app to sign in to network
            cm.startCaptivePortalApp(network);
        } else if (accessPointState != null) {
            connect(context, new AccessPoint(context, accessPointState));
        }
    }

    @VisibleForTesting
    void connect(Context context, AccessPoint accessPoint) {
        ContextualWifiScanWorker.saveSession();
        WifiScanWorker.saveClickedWifi(accessPoint);

        final WifiConnectListener connectListener = new WifiConnectListener(context);
        switch (WifiUtils.getConnectingType(accessPoint)) {
            case WifiUtils.CONNECT_TYPE_OSU_PROVISION:
                accessPoint.startOsuProvisioning(connectListener);
                break;

            case WifiUtils.CONNECT_TYPE_OPEN_NETWORK:
                accessPoint.generateOpenNetworkConfig();

            case WifiUtils.CONNECT_TYPE_SAVED_NETWORK:
                final WifiManager wifiManager = context.getSystemService(WifiManager.class);
                wifiManager.connect(accessPoint.getConfig(), connectListener);
                break;
        }
    }
}
