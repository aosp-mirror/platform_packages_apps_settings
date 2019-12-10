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

package com.android.settings.network;

import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.ConnectivityManager.TETHERING_WIFI;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SwitchWidgetController;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TetherEnabler is a helper to manage Tethering switch on/off state. It turns on/off
 * different types of tethering based on stored values in {@link SharedPreferences} and ensures
 * tethering state updated by data saver state.
 */

public final class TetherEnabler implements SwitchWidgetController.OnSwitchChangeListener,
        DataSaverBackend.Listener, LifecycleObserver {
    @VisibleForTesting
    static final String WIFI_TETHER_KEY = "enable_wifi_tethering";
    @VisibleForTesting
    static final String USB_TETHER_KEY = "enable_usb_tethering";
    @VisibleForTesting
    static final String BLUETOOTH_TETHER_KEY = "enable_bluetooth_tethering";

    private final SwitchWidgetController mSwitchWidgetController;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;

    private final DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;

    private final Context mContext;

    @VisibleForTesting
    final ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback =
            new ConnectivityManager.OnStartTetheringCallback() {
                @Override
                public void onTetheringFailed() {
                    super.onTetheringFailed();
                    mSwitchWidgetController.setChecked(false);
                    setSwitchWidgetEnabled(true);
                }
            };
    private final AtomicReference<BluetoothPan> mBluetoothPan;
    private final SharedPreferences mSharedPreferences;
    private boolean mBluetoothEnableForTether;
    private final BluetoothAdapter mBluetoothAdapter;

    TetherEnabler(Context context, SwitchWidgetController switchWidgetController,
            AtomicReference<BluetoothPan> bluetoothPan) {
        mContext = context;
        mSwitchWidgetController = switchWidgetController;
        mDataSaverBackend = new DataSaverBackend(context);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothPan = bluetoothPan;
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mDataSaverBackend.addListener(this);
        mSwitchWidgetController.setListener(this);
        mSwitchWidgetController.startListening();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mTetherChangeReceiver, filter);
        mSwitchWidgetController.setChecked(isTethering());
        setSwitchWidgetEnabled(true);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mDataSaverBackend.remListener(this);
        mSwitchWidgetController.stopListening();
        mContext.unregisterReceiver(mTetherChangeReceiver);
    }

    private void setSwitchWidgetEnabled(boolean enabled) {
        mSwitchWidgetController.setEnabled(enabled && !mDataSaverEnabled);
    }

    private boolean isTethering() {
        String[] tethered = mConnectivityManager.getTetheredIfaces();
        return isTethering(tethered);
    }

    private boolean isTethering(String[] tethered) {
        if (tethered != null && tethered.length != 0) {
            return true;
        }

        final BluetoothPan pan = mBluetoothPan.get();

        return pan != null && pan.isTetheringOn();
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (isChecked) {
            startTether();
        } else {
            stopTether();
        }
        return true;
    }

    @VisibleForTesting
    void stopTether() {
        setSwitchWidgetEnabled(false);

        // Wi-Fi tether is selected by default
        if (mSharedPreferences.getBoolean(WIFI_TETHER_KEY, true)) {
            mConnectivityManager.stopTethering(TETHERING_WIFI);
        }

        // USB tether is not selected by default
        if (mSharedPreferences.getBoolean(USB_TETHER_KEY, false)) {
            mConnectivityManager.stopTethering(TETHERING_USB);
        }

        // Bluetooth tether is not selected by default
        if (mSharedPreferences.getBoolean(BLUETOOTH_TETHER_KEY, false)) {
            mConnectivityManager.stopTethering(TETHERING_BLUETOOTH);
        }
    }

    @VisibleForTesting
    void startTether() {
        setSwitchWidgetEnabled(false);

        // Wi-Fi tether is selected by default
        if (mSharedPreferences.getBoolean(WIFI_TETHER_KEY, true)) {
            startTethering(TETHERING_WIFI);
        }

        // USB tether is not selected by default
        if (mSharedPreferences.getBoolean(USB_TETHER_KEY, false)) {
            startTethering(TETHERING_USB);
        }

        // Bluetooth tether is not selected by default
        if (mSharedPreferences.getBoolean(BLUETOOTH_TETHER_KEY, false)) {
            startTethering(TETHERING_BLUETOOTH);
        }
    }

    @VisibleForTesting
    void startTethering(int choice) {
        if (choice == TETHERING_BLUETOOTH) {
            // Turn on Bluetooth first.
            if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                mBluetoothEnableForTether = true;
                mBluetoothAdapter.enable();
                return;
            }
        } else if (choice == TETHERING_WIFI && mWifiManager.isWifiApEnabled()) {
            return;
        }


        mConnectivityManager.startTethering(choice, true /* showProvisioningUi */,
                mOnStartTetheringCallback, new Handler(Looper.getMainLooper()));
    }

    private final BroadcastReceiver mTetherChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED, action)) {
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                mSwitchWidgetController.setChecked(
                        isTethering(active.toArray(new String[active.size()])));
                setSwitchWidgetEnabled(true);
            } else if (TextUtils.equals(BluetoothAdapter.ACTION_STATE_CHANGED, action)) {
                if (mBluetoothEnableForTether) {
                    switch (intent
                            .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            startTethering(TETHERING_BLUETOOTH);
                            mBluetoothEnableForTether = false;
                            break;

                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.ERROR:
                            mBluetoothEnableForTether = false;
                            break;

                        default:
                            // ignore transition states
                    }
                }
            }
        }
    };

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        setSwitchWidgetEnabled(!isDataSaving);
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
