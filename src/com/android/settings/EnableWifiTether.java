/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.provider.Settings;

public class EnableWifiTether extends Activity {

    private static final String TETHER_CHOICE = "TETHER_TYPE";
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;
    private static final int WIFI_TETHERING = 0;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mProvisionApp = getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
        startProvisioning();
    }

    private void startProvisioning() {
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(closeDialog);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(mProvisionApp[0], mProvisionApp[1]);
        intent.putExtra(TETHER_CHOICE, WIFI_TETHERING);
        startActivityForResult(intent, PROVISION_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == PROVISION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                enableTethering();
            }
            finish();
        }
    }

    private void enableTethering() {
        final ContentResolver cr = getContentResolver();
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();

        if ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
            wifiManager.setWifiEnabled(false);
            Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 1);
        }

        wifiManager.setWifiApEnabled(null, true);
    }

}
