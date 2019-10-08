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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.core.text.BidiFormatter;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * {@link PreferenceControllerMixin} that updates MAC/IP address.
 */
public class WifiInfoPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_CURRENT_IP_ADDRESS = "current_ip_address";
    private static final String KEY_MAC_ADDRESS = "mac_address";

    private final IntentFilter mFilter;
    private final WifiManager mWifiManager;

    private Preference mWifiMacAddressPref;
    private Preference mWifiIpAddressPref;

    public WifiInfoPreferenceController(Context context, Lifecycle lifecycle,
            WifiManager wifiManager) {
        super(context);
        mWifiManager = wifiManager;
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        // Returns null because this controller contains more than 1 preference.
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mWifiMacAddressPref = screen.findPreference(KEY_MAC_ADDRESS);
        mWifiMacAddressPref.setSelectable(false);
        mWifiIpAddressPref = screen.findPreference(KEY_CURRENT_IP_ADDRESS);
        mWifiIpAddressPref.setSelectable(false);
    }

    @Override
    public void onResume() {
        mContext.registerReceiver(mReceiver, mFilter);
        updateWifiInfo();
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mReceiver);
    }

    public void updateWifiInfo() {
        if (mWifiMacAddressPref != null) {
            final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            final boolean macRandomizationSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported);
            final String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();

            if (macRandomizationSupported && WifiInfo.DEFAULT_MAC_ADDRESS.equals(macAddress)) {
                mWifiMacAddressPref.setSummary(R.string.wifi_status_mac_randomized);
            } else if (TextUtils.isEmpty(macAddress)
                    || WifiInfo.DEFAULT_MAC_ADDRESS.equals(macAddress)) {
                mWifiMacAddressPref.setSummary(R.string.status_unavailable);
            } else {
                mWifiMacAddressPref.setSummary(macAddress);
            }
        }
        if (mWifiIpAddressPref != null) {
            final String ipAddress = Utils.getWifiIpAddresses(mContext);
            mWifiIpAddressPref.setSummary(ipAddress == null
                    ? mContext.getString(R.string.status_unavailable)
                    : BidiFormatter.getInstance().unicodeWrap(ipAddress));
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION) ||
                    action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                updateWifiInfo();
            }
        }
    };
}
