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

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.concurrent.atomic.AtomicBoolean;

public class WifiEnabler implements SwitchWidgetController.OnSwitchChangeListener  {

    private final SwitchWidgetController mSwitchWidget;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private Context mContext;
    private boolean mListeningToOnSwitchChange = false;
    private AtomicBoolean mConnected = new AtomicBoolean(false);


    private boolean mStateMachineEvent;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiStateChanged(mWifiManager.getWifiState());
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                if (!mConnected.get()) {
                    handleStateChanged(WifiInfo.getDetailedStateOf((SupplicantState)
                            intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                mConnected.set(info.isConnected());
                handleStateChanged(info.getDetailedState());
            }
        }
    };

    public WifiEnabler(Context context, SwitchWidgetController switchWidget,
        MetricsFeatureProvider metricsFeatureProvider) {
        this(context, switchWidget, metricsFeatureProvider,
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    @VisibleForTesting
    WifiEnabler(Context context, SwitchWidgetController switchWidget,
            MetricsFeatureProvider metricsFeatureProvider,
            ConnectivityManager connectivityManager) {
        mContext = context;
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mMetricsFeatureProvider = metricsFeatureProvider;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = connectivityManager;

        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // The order matters! We really should not depend on this. :(
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        setupSwitchController();
    }

    public void setupSwitchController() {
        final int state = mWifiManager.getWifiState();
        handleWifiStateChanged(state);
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
        mSwitchWidget.setupView();
    }

    public void teardownSwitchController() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
        mSwitchWidget.teardownView();
    }

    public void resume(Context context) {
        mContext = context;
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
    }

    private void handleWifiStateChanged(int state) {
        // Clear any previous state
        mSwitchWidget.setDisabledByAdmin(null);

        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                setSwitchBarChecked(true);
                mSwitchWidget.setEnabled(true);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                setSwitchBarChecked(false);
                mSwitchWidget.setEnabled(true);
                break;
            default:
                setSwitchBarChecked(false);
                mSwitchWidget.setEnabled(true);
        }
    }

    private void setSwitchBarChecked(boolean checked) {
        mStateMachineEvent = true;
        mSwitchWidget.setChecked(checked);
        mStateMachineEvent = false;
    }

    private void handleStateChanged(@SuppressWarnings("unused") NetworkInfo.DetailedState state) {
        // After the refactoring from a CheckBoxPreference to a Switch, this method is useless since
        // there is nowhere to display a summary.
        // This code is kept in case a future change re-introduces an associated text.
        /*
        // WifiInfo is valid if and only if Wi-Fi is enabled.
        // Here we use the state of the switch as an optimization.
        if (state != null && mSwitch.isChecked()) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            if (info != null) {
                //setSummary(Summary.get(mContext, info.getSSID(), state));
            }
        }
        */
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return true;
        }
        // Show toast message if Wi-Fi is not allowed in airplane mode
        if (isChecked && !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_WIFI)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off. No infinite check/listener loop.
            mSwitchWidget.setChecked(false);
            return false;
        }

        if (isChecked) {
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_WIFI_ON);
        } else {
            // Log if user was connected at the time of switching off.
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_WIFI_OFF,
                    mConnected.get());
        }
        if (!mWifiManager.setWifiEnabled(isChecked)) {
            // Error
            mSwitchWidget.setEnabled(true);
            Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
