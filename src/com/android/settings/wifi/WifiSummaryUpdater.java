/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.wifi.WifiStatusTracker;

/**
 * Helper class that listeners to wifi callback and notify client when there is update in
 * wifi summary info.
 */
public final class WifiSummaryUpdater extends SummaryUpdater {

    private final WifiStatusTracker mWifiTracker;
    private final BroadcastReceiver mReceiver;

    private static final IntentFilter INTENT_FILTER;
    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        INTENT_FILTER.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        INTENT_FILTER.addAction(WifiManager.RSSI_CHANGED_ACTION);
    }

    public WifiSummaryUpdater(Context context, OnSummaryChangeListener listener) {
        this(context, listener, null);
    }

    @VisibleForTesting
    public WifiSummaryUpdater(Context context, OnSummaryChangeListener listener,
        WifiStatusTracker wifiTracker) {
        super(context, listener);
        mWifiTracker = wifiTracker != null ? wifiTracker :
                new WifiStatusTracker(context, context.getSystemService(WifiManager.class),
                context.getSystemService(NetworkScoreManager.class),
                context.getSystemService(ConnectivityManager.class),
                        this::notifyChangeIfNeeded);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mWifiTracker.handleBroadcast(intent);
                notifyChangeIfNeeded();
            }
        };
    }

    @Override
    public void register(boolean register) {
        if (register) {
            notifyChangeIfNeeded();
            mContext.registerReceiver(mReceiver, INTENT_FILTER);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
        mWifiTracker.setListening(register);
    }

    @Override
    public String getSummary() {
        if (!mWifiTracker.enabled) {
            return mContext.getString(R.string.switch_off_text);
        }
        if (!mWifiTracker.connected) {
            return mContext.getString(R.string.disconnected);
        }
        String ssid = WifiInfo.sanitizeSsid(mWifiTracker.ssid);
        if (TextUtils.isEmpty(mWifiTracker.statusLabel)) {
            return ssid;
        }
        return mContext.getResources().getString(
                com.android.settingslib.R.string.preference_summary_default_combination,
                ssid, mWifiTracker.statusLabel);
    }
}
