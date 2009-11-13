/*
 * Copyright (C) 2007 The Android Open Source Project
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

import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_UNKNOWN;

import com.android.settings.R;
import com.android.settings.AirplaneModeEnabler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;

public class WifiEnabler implements Preference.OnPreferenceChangeListener {
    
    private static final boolean LOCAL_LOGD = Config.LOGD || WifiLayer.LOGV;
    private static final String TAG = "SettingsWifiEnabler";
    
    private final Context mContext; 
    private final WifiManager mWifiManager;
    private final CheckBoxPreference mWifiCheckBoxPref;
    private final CharSequence mOriginalSummary;
    
    private final IntentFilter mWifiStateFilter;
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handleWifiStateChanged(
                        intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WIFI_STATE_UNKNOWN),
                        intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE,
                                WIFI_STATE_UNKNOWN));
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                handleNetworkStateChanged(
                        (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
            }
        }
    };
    
    public WifiEnabler(Context context, WifiManager wifiManager,
            CheckBoxPreference wifiCheckBoxPreference) {
        mContext = context;
        mWifiCheckBoxPref = wifiCheckBoxPreference;
        mWifiManager = wifiManager;
        
        mOriginalSummary = wifiCheckBoxPreference.getSummary();
        wifiCheckBoxPreference.setPersistent(false);
        
        mWifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    public void resume() {
        int state = mWifiManager.getWifiState();
        // This is the widget enabled state, not the preference toggled state
        mWifiCheckBoxPref.setEnabled(state == WIFI_STATE_ENABLED || state == WIFI_STATE_DISABLED
                || state == WIFI_STATE_UNKNOWN);

        mContext.registerReceiver(mWifiStateReceiver, mWifiStateFilter);
        mWifiCheckBoxPref.setOnPreferenceChangeListener(this);
    }
    
    public void pause() {
        mContext.unregisterReceiver(mWifiStateReceiver);
        mWifiCheckBoxPref.setOnPreferenceChangeListener(null);
    }
    
    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn on/off Wi-Fi
        setWifiEnabled((Boolean) value);
        
        // Don't update UI to opposite state until we're sure
        return false;
    }
    
    private void setWifiEnabled(final boolean enable) {
        // Disable button
        mWifiCheckBoxPref.setEnabled(false);
        
        if (!mWifiManager.setWifiEnabled(enable)) {
            mWifiCheckBoxPref.setSummary(enable ? R.string.error_starting : R.string.error_stopping);
        }
    }
    
    private void handleWifiStateChanged(int wifiState, int previousWifiState) {

        if (LOCAL_LOGD) {
            Log.d(TAG, "Received wifi state changed from "
                    + getHumanReadableWifiState(previousWifiState) + " to "
                    + getHumanReadableWifiState(wifiState));
        }
        
        if (wifiState == WIFI_STATE_DISABLED || wifiState == WIFI_STATE_ENABLED) {
            mWifiCheckBoxPref.setChecked(wifiState == WIFI_STATE_ENABLED);
            mWifiCheckBoxPref
                    .setSummary(wifiState == WIFI_STATE_DISABLED ? mOriginalSummary : null);
            
            mWifiCheckBoxPref.setEnabled(isWifiAllowed(mContext));
            
        } else if (wifiState == WIFI_STATE_DISABLING || wifiState == WIFI_STATE_ENABLING) {
            mWifiCheckBoxPref.setSummary(wifiState == WIFI_STATE_ENABLING ? R.string.wifi_starting
                    : R.string.wifi_stopping);
            
        } else if (wifiState == WIFI_STATE_UNKNOWN) {
            int message = R.string.wifi_error;
            if (previousWifiState == WIFI_STATE_ENABLING) message = R.string.error_starting;
            else if (previousWifiState == WIFI_STATE_DISABLING) message = R.string.error_stopping;
            
            mWifiCheckBoxPref.setChecked(false);
            mWifiCheckBoxPref.setSummary(message);
            mWifiCheckBoxPref.setEnabled(true);
        }
    }

    private void handleNetworkStateChanged(NetworkInfo networkInfo) {

        if (LOCAL_LOGD) {
            Log.d(TAG, "Received network state changed to " + networkInfo);
        }
        
        if (mWifiManager.isWifiEnabled()) {
            String summary = WifiStatus.getStatus(mContext, 
                    mWifiManager.getConnectionInfo().getSSID(), networkInfo.getDetailedState());
            mWifiCheckBoxPref.setSummary(summary);
        }
    }

    private static boolean isWifiAllowed(Context context) {
        // allowed if we are not in airplane mode
        if (!AirplaneModeEnabler.isAirplaneModeOn(context)) {
            return true;
        }
        // allowed if wifi is not in AIRPLANE_MODE_RADIOS
        String radios = Settings.System.getString(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_RADIOS);
        if (radios == null || !radios.contains(Settings.System.RADIO_WIFI)) {
            return true;
        }
        // allowed if wifi is in AIRPLANE_MODE_TOGGLEABLE_RADIOS
        radios = Settings.System.getString(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return (radios != null && radios.contains(Settings.System.RADIO_WIFI));
    }

    private static String getHumanReadableWifiState(int wifiState) {
        switch (wifiState) {
            case WIFI_STATE_DISABLED:
                return "Disabled";
            case WIFI_STATE_DISABLING:
                return "Disabling";
            case WIFI_STATE_ENABLED:
                return "Enabled";
            case WIFI_STATE_ENABLING:
                return "Enabling";
            case WIFI_STATE_UNKNOWN:
                return "Unknown";
            default:
                return "Some other state!";    
        }
    }
}
