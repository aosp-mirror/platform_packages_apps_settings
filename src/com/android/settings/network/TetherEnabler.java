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

import static com.android.settings.AllInOneTetherSettings.DEDUP_POSTFIX;

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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SwitchWidgetController;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TetherEnabler is a helper to manage Tethering switch on/off state. It turns on/off
 * different types of tethering based on stored values in {@link SharedPreferences} and ensures
 * tethering state updated by data saver state.
 *
 * This class is not designed for extending. It's extendable solely for the test purpose.
 */

public class TetherEnabler implements SwitchWidgetController.OnSwitchChangeListener,
        DataSaverBackend.Listener, LifecycleObserver,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "TetherEnabler";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String SHARED_PREF = "tether_options";

    // This KEY is used for a shared preference value, not for any displayed preferences.
    public static final String KEY_ENABLE_WIFI_TETHERING = "enable_wifi_tethering";
    public static final String WIFI_TETHER_DISABLE_KEY = "disable_wifi_tethering";
    public static final String USB_TETHER_KEY = "enable_usb_tethering";
    public static final String BLUETOOTH_TETHER_KEY = "enable_bluetooth_tethering" + DEDUP_POSTFIX;

    private final SwitchWidgetController mSwitchWidgetController;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;

    private final DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;

    private final Context mContext;

    @VisibleForTesting
    ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback;
    private final AtomicReference<BluetoothPan> mBluetoothPan;
    private final SharedPreferences mSharedPreferences;
    private boolean mBluetoothEnableForTether;
    private final BluetoothAdapter mBluetoothAdapter;

    public TetherEnabler(Context context, SwitchWidgetController switchWidgetController,
            AtomicReference<BluetoothPan> bluetoothPan) {
        mContext = context;
        mSwitchWidgetController = switchWidgetController;
        mDataSaverBackend = new DataSaverBackend(context);
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
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

        final IntentFilter filter = new IntentFilter(
                ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mTetherChangeReceiver, filter);

        mOnStartTetheringCallback = new OnStartTetheringCallback(this);
        updateState(null/*tethered*/);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mDataSaverBackend.remListener(this);
        mSwitchWidgetController.stopListening();
        mContext.unregisterReceiver(mTetherChangeReceiver);
    }

    @VisibleForTesting
    void updateState(@Nullable String[] tethered) {
        boolean isTethering = tethered == null ? isTethering() : isTethering(tethered);
        if (DEBUG) {
            Log.d(TAG, "updateState: " + isTethering);
        }
        setSwitchCheckedInternal(isTethering);
        mSwitchWidgetController.setEnabled(!mDataSaverEnabled);
    }

    private void setSwitchCheckedInternal(boolean checked) {
        mSwitchWidgetController.stopListening();
        mSwitchWidgetController.setChecked(checked);
        mSwitchWidgetController.startListening();
    }

    private boolean isTethering() {
        String[] tethered = mConnectivityManager.getTetheredIfaces();
        return isTethering(tethered);
    }

    private boolean isTethering(String[] tethered) {
        if (tethered != null && tethered.length != 0) {
            return true;
        }

        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
            return true;
        }

        final BluetoothPan pan = mBluetoothPan.get();

        return pan != null && pan.isTetheringOn();
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (isChecked && !isTethering()) {
            startTether();
        }

        if (!isChecked && isTethering()) {
            stopTether();
        }
        return true;
    }

    private void stopTether() {

        // Wi-Fi tether is selected by default.
        if (mSharedPreferences.getBoolean(KEY_ENABLE_WIFI_TETHERING, true)) {
            stopTethering(TETHERING_WIFI);
        }

        if (mSharedPreferences.getBoolean(USB_TETHER_KEY, false)) {
            stopTethering(TETHERING_USB);
        }

        if (mSharedPreferences.getBoolean(BLUETOOTH_TETHER_KEY, false)) {
            stopTethering(TETHERING_BLUETOOTH);
        }
    }

    /**
     * Use this method to stop a single choice of tethering.
     *
     * @param choice The choice of tethering to stop.
     */
    public void stopTethering(int choice) {
        mConnectivityManager.stopTethering(choice);
    }

    @VisibleForTesting
    void startTether() {

        // Wi-Fi tether is selected by default.
        if (mSharedPreferences.getBoolean(KEY_ENABLE_WIFI_TETHERING, true)) {
            startTethering(TETHERING_WIFI);
        }

        if (mSharedPreferences.getBoolean(USB_TETHER_KEY, false)) {
            startTethering(TETHERING_USB);
        }

        if (mSharedPreferences.getBoolean(BLUETOOTH_TETHER_KEY, false)) {
            startTethering(TETHERING_BLUETOOTH);
        }
    }

    /**
     * Use this method to start a single choice of tethering.
     * For bluetooth tethering, it will first turn on bluetooth if bluetooth is off.
     * For Wi-Fi tethering, it will be no-op if Wi-Fi tethering already active.
     *
     * @param choice The choice of tethering to start.
     */
    public void startTethering(int choice) {
        mSwitchWidgetController.setEnabled(false);

        if (choice == TETHERING_WIFI && mWifiManager.isWifiApEnabled()) {
            if (DEBUG) {
                Log.d(TAG, "Wifi tether already active!");
            }
            return;
        }

        if (choice == TETHERING_BLUETOOTH) {
            if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                if (DEBUG) {
                    Log.d(TAG, "Turn on bluetooth first.");
                }
                mBluetoothEnableForTether = true;
                mBluetoothAdapter.enable();
                return;
            }
        }

        mConnectivityManager.startTethering(choice, true /* showProvisioningUi */,
                mOnStartTetheringCallback, new Handler(Looper.getMainLooper()));
    }

    private final BroadcastReceiver mTetherChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            ArrayList<String> active = null;
            boolean shouldUpdateState = false;
            if (TextUtils.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED, action)) {
                active = intent.getStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER);
                shouldUpdateState = true;
            } else if (TextUtils.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION, action)) {
                shouldUpdateState = handleWifiApStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED));
            } else if (TextUtils.equals(BluetoothAdapter.ACTION_STATE_CHANGED, action)) {
                shouldUpdateState = handleBluetoothStateChanged(intent
                        .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
            }

            if (shouldUpdateState) {
                if (active != null) {
                    updateState(active.toArray(new String[0]));
                } else {
                    updateState(null/*tethered*/);
                }
            }
        }
    };

    private boolean handleBluetoothStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                if (mBluetoothEnableForTether) {
                    startTethering(TETHERING_BLUETOOTH);
                }
                // Fall through.
            case BluetoothAdapter.STATE_OFF:
                // Fall through.
            case BluetoothAdapter.ERROR:
                mBluetoothEnableForTether = false;
                return true;
            default:
                // Return false for transition states.
                return false;
        }
    }

    private boolean handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_FAILED:
                Log.e(TAG, "Wifi AP is failed!");
                // fall through
            case WifiManager.WIFI_AP_STATE_ENABLED:
                // fall through
            case WifiManager.WIFI_AP_STATE_DISABLED:
                return true;
            default:
                // return false for transition state
                return false;
        }
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        mSwitchWidgetController.setEnabled(!isDataSaving);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
        // we don't care, since we just want to read the value
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
        // we don't care, since we just want to read the value
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!mSwitchWidgetController.isChecked()) {
            return;
        }
        if (TextUtils.equals(KEY_ENABLE_WIFI_TETHERING, key)) {
            if (sharedPreferences.getBoolean(key, true)) {
                startTethering(TETHERING_WIFI);
            } else {
                mConnectivityManager.stopTethering(TETHERING_WIFI);
            }
        } else if (TextUtils.equals(USB_TETHER_KEY, key)) {
            if (sharedPreferences.getBoolean(key, false)) {
                startTethering(TETHERING_USB);
            } else {
                mConnectivityManager.stopTethering(TETHERING_USB);
            }
        } else if (TextUtils.equals(BLUETOOTH_TETHER_KEY, key)) {
            if (sharedPreferences.getBoolean(key, false)) {
                startTethering(TETHERING_BLUETOOTH);
            } else {
                mConnectivityManager.stopTethering(TETHERING_BLUETOOTH);
            }
        }
    }

    private static final class OnStartTetheringCallback extends
            ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TetherEnabler> mTetherEnabler;

        OnStartTetheringCallback(TetherEnabler enabler) {
            mTetherEnabler = new WeakReference<>(enabler);
        }

        @Override
        public void onTetheringStarted() {
            update();
        }

        @Override
        public void onTetheringFailed() {
            update();
        }

        private void update() {
            TetherEnabler enabler = mTetherEnabler.get();
            if (enabler != null) {
                enabler.updateState(null/*tethered*/);
            }
        }
    }
}
