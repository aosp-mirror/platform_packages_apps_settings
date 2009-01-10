/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.settings.R;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.util.Log;

/**
 * BluetoothDiscoverableEnabler is a helper to manage the "Discoverable"
 * checkbox. It sets/unsets discoverability and keeps track of how much time
 * until the the discoverability is automatically turned off.
 */
public class BluetoothDiscoverableEnabler implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BluetoothDiscoverableEnabler";
    private static final boolean V = LocalBluetoothManager.V;
    
    private static final String SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT =
            "debug.bt.discoverable_time";
    private static final int DISCOVERABLE_TIMEOUT = 120; 

    private static final String SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP =
            "discoverable_end_timestamp";
    
    private final Context mContext;
    private final Handler mUiHandler;
    private final CheckBoxPreference mCheckBoxPreference;
    
    private final LocalBluetoothManager mLocalManager;
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
             * TODO: remove this once the BT framework broadcasts the
             * MODE_CHANGED action when going into MODE_OFF.
             */
            int mode = BluetoothIntent.DISABLED_ACTION.equals(intent.getAction())
                    ? BluetoothDevice.MODE_OFF                    
                    : intent.getIntExtra(BluetoothIntent.MODE, BluetoothDevice.MODE_UNKNOWN);
            handleModeChanged(mode);
        }
    };

    private final Runnable mUpdateCountdownSummaryRunnable = new Runnable() {
        public void run() {
            updateCountdownSummary();        
        } 
    };

    public BluetoothDiscoverableEnabler(Context context, CheckBoxPreference checkBoxPreference) {
        mContext = context;
        mUiHandler = new Handler();
        mCheckBoxPreference = checkBoxPreference;
        
        checkBoxPreference.setPersistent(false);
        
        mLocalManager = LocalBluetoothManager.getInstance(context);
        if (mLocalManager == null) {
            // Bluetooth not supported
            checkBoxPreference.setEnabled(false);
        }
    }

    public void resume() {
        if (mLocalManager == null) {
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothIntent.MODE_CHANGED_ACTION);
        filter.addAction(BluetoothIntent.DISABLED_ACTION);
        mContext.registerReceiver(mReceiver, filter);
        mCheckBoxPreference.setOnPreferenceChangeListener(this);
        
        handleModeChanged(mLocalManager.getBluetoothManager().getMode());
    }
    
    public void pause() {
        if (mLocalManager == null) {
            return;
        }
        
        mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
        mCheckBoxPreference.setOnPreferenceChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
    }
    
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (V) {
            Log.v(TAG, "Preference changed to " + value);
        }
        
        // Turn on/off BT discoverability
        setEnabled((Boolean) value);
        
        return true;
    }
    
    private void setEnabled(final boolean enable) {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();
        
        if (enable) {

            int timeout = getDiscoverableTimeout();
            manager.setDiscoverableTimeout(timeout);
            
            long endTimestamp = System.currentTimeMillis() + timeout * 1000;
            persistDiscoverableEndTimestamp(endTimestamp);
            
            manager.setMode(BluetoothDevice.MODE_DISCOVERABLE);
            handleModeChanged(BluetoothDevice.MODE_DISCOVERABLE);            
            
        } else {
            manager.setMode(BluetoothDevice.MODE_CONNECTABLE);
        }
    }

    private int getDiscoverableTimeout() {
        int timeout = SystemProperties.getInt(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, -1);
        if (timeout <= 0) {
            timeout = DISCOVERABLE_TIMEOUT;
        }
        
        return timeout;
    }

    private void persistDiscoverableEndTimestamp(long endTimestamp) {
        SharedPreferences.Editor editor = mLocalManager.getSharedPreferences().edit();
        editor.putLong(SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, endTimestamp);
        editor.commit();
    }
    
    private void handleModeChanged(int mode) {
        if (V) {
            Log.v(TAG, "Got mode changed: " + mode);
        }
        
        if (mode == BluetoothDevice.MODE_DISCOVERABLE) {
            mCheckBoxPreference.setChecked(true);
            updateCountdownSummary();
            
        } else {
            mCheckBoxPreference.setChecked(false);
        }
    }
    
    private void updateCountdownSummary() {
        int mode = mLocalManager.getBluetoothManager().getMode();
        if (mode != BluetoothDevice.MODE_DISCOVERABLE) {
            return;
        }
            
        long currentTimestamp = System.currentTimeMillis();
        long endTimestamp = mLocalManager.getSharedPreferences().getLong(
                SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, 0);
            
        if (currentTimestamp > endTimestamp) {
            // We're still in discoverable mode, but maybe there isn't a timeout.
            mCheckBoxPreference.setSummaryOn(null);
            return;
        }
        
        String formattedTimeLeft = String.valueOf((endTimestamp - currentTimestamp) / 1000);
        
        mCheckBoxPreference.setSummaryOn(
                mContext.getResources().getString(R.string.bluetooth_is_discoverable,
                        formattedTimeLeft));
        
        synchronized (this) {
            mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
            mUiHandler.postDelayed(mUpdateCountdownSummaryRunnable, 1000);
        }
    }
    
        
}
