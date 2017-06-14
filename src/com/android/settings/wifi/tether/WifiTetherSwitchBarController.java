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

package com.android.settings.wifi.tether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Switch;

import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import static android.net.ConnectivityManager.TETHERING_WIFI;

public class WifiTetherSwitchBarController implements SwitchBar.OnSwitchChangeListener,
        LifecycleObserver, OnStart, OnStop {

    private final Context mContext;
    private final SwitchBar mSwitchBar;
    private final ConnectivityManager mConnectivityManager;
    private final DataSaverBackend mDataSaverBackend;
    private final NoOpOnStartTetheringCallback mOnStartTetheringCallback;

    WifiTetherSwitchBarController(Context context, SwitchBar switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mDataSaverBackend = new DataSaverBackend(context);
        mOnStartTetheringCallback = new NoOpOnStartTetheringCallback();
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mReceiver,
                WifiTetherPreferenceController.WIFI_TETHER_INTENT_FILTER);
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            startTether();
        } else {
            stopTether();
        }
    }

    void stopTether() {
        mSwitchBar.setEnabled(false);
        mConnectivityManager.stopTethering(TETHERING_WIFI);
    }

    void startTether() {
        mSwitchBar.setEnabled(false);
        mConnectivityManager.startTethering(TETHERING_WIFI, true /* showProvisioningUi */,
                mOnStartTetheringCallback, new Handler(Looper.getMainLooper()));
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                final int state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED);
                handleWifiApStateChanged(state);
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                enableWifiSwitch();
            }
        }
    };

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                if (!mSwitchBar.isChecked()) {
                    mSwitchBar.setChecked(true);
                }
                enableWifiSwitch();
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                if (mSwitchBar.isChecked()) {
                    mSwitchBar.setChecked(false);
                }
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mSwitchBar.setChecked(false);
                enableWifiSwitch();
                break;
            default:
                mSwitchBar.setChecked(false);
                enableWifiSwitch();
                break;
        }
    }

    private void enableWifiSwitch() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (!isAirplaneMode) {
            mSwitchBar.setEnabled(!mDataSaverBackend.isDataSaverEnabled());
        } else {
            mSwitchBar.setEnabled(false);
        }
    }
}
