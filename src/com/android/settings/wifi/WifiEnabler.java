/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.WirelessSettings;

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
import android.widget.Toast;

public class WifiEnabler implements Preference.OnPreferenceChangeListener {
    private final Context mContext; 
    private final CheckBoxPreference mCheckBox;
    private final CharSequence mOriginalSummary;

    private final WifiManager mWifiManager;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                handleNetworkStateChanged((NetworkInfo)
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
            }
        }
    };

    public WifiEnabler(Context context, CheckBoxPreference checkBox) {
        this(context, (WifiManager) context.getSystemService(Context.WIFI_SERVICE),
                checkBox);
    }
    
    public WifiEnabler(Context context, WifiManager wifiManager,
            CheckBoxPreference checkBox) {
        mContext = context;
        mCheckBox = checkBox;
        mWifiManager = wifiManager;
        mOriginalSummary = checkBox.getSummary();
        checkBox.setPersistent(false);
        
        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    public void resume() {
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mCheckBox.setOnPreferenceChangeListener(this);
    }
    
    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mCheckBox.setOnPreferenceChangeListener(null);
    }
    
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean enable = (Boolean) value;
    
        // Show toast message if Wi-Fi is not allowed in airplane mode
        if (enable && !WirelessSettings
                .isRadioAllowed(mContext, Settings.System.RADIO_WIFI)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mWifiManager.setWifiEnabled(enable)) {
            mCheckBox.setEnabled(false);
        } else {
            mCheckBox.setSummary(R.string.wifi_error);
        }

        // Don't update UI to opposite state until we're sure
        return false;
    }
    
    private void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                mCheckBox.setSummary(R.string.wifi_starting);
                mCheckBox.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                mCheckBox.setChecked(true);
                mCheckBox.setSummary(null);
                mCheckBox.setEnabled(true);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                mCheckBox.setSummary(R.string.wifi_stopping);
                mCheckBox.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                mCheckBox.setChecked(false);
                mCheckBox.setSummary(mOriginalSummary);
                mCheckBox.setEnabled(true);
                break;
            default:
                mCheckBox.setChecked(false);
                mCheckBox.setSummary(R.string.wifi_error);
                mCheckBox.setEnabled(true);
        }
    }

    private void handleNetworkStateChanged(NetworkInfo networkInfo) {
        if (mWifiManager.isWifiEnabled()) {
            String summary = WifiStatus.getStatus(mContext, 
                    mWifiManager.getConnectionInfo().getSSID(), networkInfo.getDetailedState());
            mCheckBox.setSummary(summary);
        }
    }
}
