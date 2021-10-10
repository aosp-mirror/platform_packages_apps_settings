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
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.wifi.WifiDialogActivity;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.ConnectCallback;

/**
 * This receiver helps connect to Wi-Fi network
 */
public class ConnectToWifiHandler extends BroadcastReceiver {

    static final String KEY_CHOSEN_WIFIENTRY_KEY = "key_chosen_wifientry_key";
    static final String KEY_WIFI_SLICE_URI = "key_wifi_slice_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        final String key = intent.getStringExtra(KEY_CHOSEN_WIFIENTRY_KEY);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        if (intent.getParcelableExtra(KEY_WIFI_SLICE_URI) == null) {
            return;
        }
        final WifiScanWorker worker = getWifiScanWorker(intent);
        if (worker == null) {
            return;
        }
        final WifiEntry wifiEntry = worker.getWifiEntry(key);
        if (wifiEntry == null) {
            return;
        }
        wifiEntry.connect(new WifiEntryConnectCallback(context, wifiEntry));
    }

    @VisibleForTesting
    WifiScanWorker getWifiScanWorker(Intent intent) {
        return SliceBackgroundWorker.getInstance(intent.getParcelableExtra(KEY_WIFI_SLICE_URI));
    }

    @VisibleForTesting
    static class WifiEntryConnectCallback implements WifiEntry.ConnectCallback {
        final Context mContext;
        final WifiEntry mWifiEntry;

        WifiEntryConnectCallback(Context context, WifiEntry connectWifiEntry) {
            mContext = context;
            mWifiEntry = connectWifiEntry;
        }

        @Override
        public void onConnectResult(@ConnectStatus int status) {
            if (status == ConnectCallback.CONNECT_STATUS_FAILURE_NO_CONFIG) {
                final Intent intent = new Intent(mContext, WifiDialogActivity.class)
                        .putExtra(WifiDialogActivity.KEY_CHOSEN_WIFIENTRY_KEY, mWifiEntry.getKey());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else if (status == CONNECT_STATUS_FAILURE_UNKNOWN) {
                Toast.makeText(mContext, R.string.wifi_failed_connect_message,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
