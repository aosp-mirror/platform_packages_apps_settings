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
package com.android.settings.wifi.p2p;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * {@link PreferenceControllerMixin} to toggle Wifi Direct preference on Wi-Fi state.
 */
public class WifiP2pPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {

    private static final String KEY_WIFI_DIRECT = "wifi_direct";

    private final WifiManager mWifiManager;
    @VisibleForTesting
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            togglePreferences();
        }
    };
    private final IntentFilter mFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    private final LocationManager mLocationManager;
    @VisibleForTesting
    final BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mWifiDirectPref != null) {
                updateState(mWifiDirectPref);
            }
        }
    };
    private final IntentFilter mLocationFilter =
            new IntentFilter(LocationManager.MODE_CHANGED_ACTION);

    private Preference mWifiDirectPref;

    public WifiP2pPreferenceController(
            Context context, Lifecycle lifecycle, WifiManager wifiManager) {
        super(context);
        mWifiManager = wifiManager;
        lifecycle.addObserver(this);
        mLocationManager = (LocationManager) context.getSystemService(Service.LOCATION_SERVICE);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mWifiDirectPref = screen.findPreference(KEY_WIFI_DIRECT);
        togglePreferences();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(mLocationManager.isLocationEnabled() && mWifiManager.isWifiEnabled());
    }

    @Override
    public void onResume() {
        mContext.registerReceiver(mReceiver, mFilter);
        mContext.registerReceiver(mLocationReceiver, mLocationFilter);
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mReceiver);
        mContext.unregisterReceiver(mLocationReceiver);
    }

    @Override
    public boolean isAvailable() {
        // Always show preference.
        return true;
    }
    @Override
    public String getPreferenceKey() {
        return KEY_WIFI_DIRECT;
    }

    private void togglePreferences() {
        if (mWifiDirectPref != null) {
            mWifiDirectPref.setEnabled(
                    mWifiManager.isWifiEnabled()
                    && mLocationManager.isLocationEnabled());
        }
    }
}
