/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.Log;

/**
 * WifiP2pEnabler is a helper to manage the Wifi p2p on/off
 */
public class WifiP2pEnabler implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "WifiP2pEnabler";

    private final Context mContext;
    private final CheckBoxPreference mCheckBox;
    private final IntentFilter mIntentFilter;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                handleP2pStateChanged(intent.getIntExtra(
                        WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED));
            }
        }
    };

    public WifiP2pEnabler(Context context, CheckBoxPreference checkBox) {
        mContext = context;
        mCheckBox = checkBox;

        mWifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (mWifiP2pManager != null) {
            mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);
            if (mChannel == null) {
                //Failure to set up connection
                Log.e(TAG, "Failed to set up connection with wifi p2p service");
                mWifiP2pManager = null;
                mCheckBox.setEnabled(false);
            }
        } else {
            Log.e(TAG, "mWifiP2pManager is null!");
        }
        mIntentFilter = new IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

    }

    public void resume() {
        if (mWifiP2pManager == null) return;
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mCheckBox.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        if (mWifiP2pManager == null) return;
        mContext.unregisterReceiver(mReceiver);
        mCheckBox.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {

        if (mWifiP2pManager == null) return false;

        mCheckBox.setEnabled(false);
        final boolean enable = (Boolean) value;
        if (enable) {
            mWifiP2pManager.enableP2p(mChannel);
        } else {
            mWifiP2pManager.disableP2p(mChannel);
        }
        return false;
    }

    private void handleP2pStateChanged(int state) {
        mCheckBox.setEnabled(true);
        switch (state) {
            case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                mCheckBox.setChecked(true);
                break;
            case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                mCheckBox.setChecked(false);
                break;
            default:
                Log.e(TAG,"Unhandled wifi state " + state);
                break;
        }
    }

}
