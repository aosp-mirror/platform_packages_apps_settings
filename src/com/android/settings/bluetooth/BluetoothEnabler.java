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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.search.Index;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * BluetoothEnabler is a helper to manage the Bluetooth on/off checkbox
 * preference. It turns on/off Bluetooth and ensures the summary of the
 * preference reflects the current state.
 */
public final class BluetoothEnabler implements SwitchBar.OnSwitchChangeListener {
    private Context mContext;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private boolean mValidListener;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final IntentFilter mIntentFilter;

    private static final String EVENT_DATA_IS_BT_ON = "is_bluetooth_on";
    private static final int EVENT_UPDATE_INDEX = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_INDEX:
                    final boolean isBluetoothOn = msg.getData().getBoolean(EVENT_DATA_IS_BT_ON);
                    Index.getInstance(mContext).updateFromClassNameResource(
                            BluetoothSettings.class.getName(), true, isBluetoothOn);
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            handleStateChanged(state);
        }
    };

    public BluetoothEnabler(Context context, SwitchBar switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mSwitch = switchBar.getSwitch();
        mValidListener = false;

        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        if (manager == null) {
            // Bluetooth is not supported
            mLocalAdapter = null;
            mSwitch.setEnabled(false);
        } else {
            mLocalAdapter = manager.getBluetoothAdapter();
        }
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    public void setupSwitchBar() {
        mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        mSwitchBar.hide();
    }

    public void resume(Context context) {
        if (mLocalAdapter == null) {
            mSwitch.setEnabled(false);
            return;
        }

        if (mContext != context) {
            mContext = context;
        }

        // Bluetooth state is not sticky, so set it manually
        handleStateChanged(mLocalAdapter.getBluetoothState());

        mSwitchBar.addOnSwitchChangeListener(this);
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mValidListener = true;
    }

    public void pause() {
        if (mLocalAdapter == null) {
            return;
        }

        mSwitchBar.removeOnSwitchChangeListener(this);
        mContext.unregisterReceiver(mReceiver);
        mValidListener = false;
    }

    void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                setChecked(true);
                mSwitch.setEnabled(true);
                updateSearchIndex(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                setChecked(false);
                mSwitch.setEnabled(true);
                updateSearchIndex(false);
                break;
            default:
                setChecked(false);
                mSwitch.setEnabled(true);
                updateSearchIndex(false);
        }
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != mSwitch.isChecked()) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitchBar.removeOnSwitchChangeListener(this);
            }
            mSwitch.setChecked(isChecked);
            if (mValidListener) {
                mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }

    private void updateSearchIndex(boolean isBluetoothOn) {
        mHandler.removeMessages(EVENT_UPDATE_INDEX);

        Message msg = new Message();
        msg.what = EVENT_UPDATE_INDEX;
        msg.getData().putBoolean(EVENT_DATA_IS_BT_ON, isBluetoothOn);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        // Show toast message if Bluetooth is not allowed in airplane mode
        if (isChecked &&
                !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_BLUETOOTH)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off
            switchView.setChecked(false);
        }

        MetricsLogger.action(mContext, MetricsLogger.ACTION_BLUETOOTH_TOGGLE, isChecked);

        if (mLocalAdapter != null) {
            mLocalAdapter.setBluetoothEnabled(isChecked);
        }
        mSwitch.setEnabled(false);
    }
}
