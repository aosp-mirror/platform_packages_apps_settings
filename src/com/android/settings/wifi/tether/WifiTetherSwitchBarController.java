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

import static android.net.ConnectivityManager.TETHERING_WIFI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;

import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller for logic pertaining to switch Wi-Fi tethering.
 */
public class WifiTetherSwitchBarController implements
        LifecycleObserver, OnStart, OnStop, DataSaverBackend.Listener, View.OnClickListener {
    private static final IntentFilter WIFI_INTENT_FILTER;

    private final Context mContext;
    private final SwitchBar mSwitchBar;
    private final Switch mSwitch;
    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;

    @VisibleForTesting
    final DataSaverBackend mDataSaverBackend;
    @VisibleForTesting
    final ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback =
            new ConnectivityManager.OnStartTetheringCallback() {
                @Override
                public void onTetheringFailed() {
                    super.onTetheringFailed();
                    mSwitchBar.setChecked(false);
                    updateWifiSwitch();
                }
            };

    static {
        WIFI_INTENT_FILTER = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
    }

    WifiTetherSwitchBarController(Context context, SwitchBar switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mSwitch = mSwitchBar.getSwitch();
        mDataSaverBackend = new DataSaverBackend(context);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mSwitchBar.setChecked(mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED);
        updateWifiSwitch();
    }

    @Override
    public void onStart() {
        mDataSaverBackend.addListener(this);
        mSwitch.setOnClickListener(this);
        mContext.registerReceiver(mReceiver, WIFI_INTENT_FILTER);
    }

    @Override
    public void onStop() {
        mDataSaverBackend.remListener(this);
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onClick(View v) {
        if (((Switch) v).isChecked()) {
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
            }
        }
    };

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                if (!mSwitch.isChecked()) {
                    mSwitch.setChecked(true);
                }
                updateWifiSwitch();
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                if (mSwitch.isChecked()) {
                    mSwitch.setChecked(false);
                }
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mSwitch.setChecked(false);
                updateWifiSwitch();
                break;
            default:
                mSwitch.setChecked(false);
                updateWifiSwitch();
                break;
        }
    }

    private void updateWifiSwitch() {
        mSwitchBar.setEnabled(!mDataSaverBackend.isDataSaverEnabled());
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        updateWifiSwitch();
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
        // we don't care, since we just want to read the value
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
        // we don't care, since we just want to read the value
    }
}
