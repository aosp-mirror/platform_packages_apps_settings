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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.common.annotations.VisibleForTesting;

/**
 * This controller helps to manage the switch state and visibility of bluetooth tether switch
 * preference.
 */
public final class BluetoothTetherPreferenceController extends TetherBasePreferenceController {
    private int mBluetoothState;

    public BluetoothTetherPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mBluetoothState = BluetoothAdapter.getDefaultAdapter().getState();
        mContext.registerReceiver(mBluetoothChangeReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mBluetoothChangeReceiver);
    }

    @Override
    public boolean shouldEnable() {
        switch (mBluetoothState) {
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_OFF:
                // fall through.
            case BluetoothAdapter.ERROR:
                return true;
            case BluetoothAdapter.STATE_TURNING_OFF:
            case BluetoothAdapter.STATE_TURNING_ON:
                // fall through.
            default:
                return false;
        }
    }

    @Override
    public boolean shouldShow() {
        final String[] bluetoothRegexs = mCm.getTetherableBluetoothRegexs();
        return bluetoothRegexs != null && bluetoothRegexs.length != 0;
    }

    @Override
    public int getTetherType() {
        return ConnectivityManager.TETHERING_BLUETOOTH;
    }

    @VisibleForTesting
    final BroadcastReceiver mBluetoothChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(BluetoothAdapter.ACTION_STATE_CHANGED, intent.getAction())) {
                mBluetoothState =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                updateState(mPreference);
            }
        }
    };
}
