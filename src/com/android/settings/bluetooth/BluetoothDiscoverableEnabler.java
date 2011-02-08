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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;

import com.android.settings.R;

/**
 * BluetoothDiscoverableEnabler is a helper to manage the "Discoverable"
 * checkbox. It sets/unsets discoverability and keeps track of how much time
 * until the the discoverability is automatically turned off.
 */
final class BluetoothDiscoverableEnabler implements Preference.OnPreferenceChangeListener {

    private static final String SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT =
            "debug.bt.discoverable_time";

    private static final int DISCOVERABLE_TIMEOUT_TWO_MINUTES = 120;
    private static final int DISCOVERABLE_TIMEOUT_FIVE_MINUTES = 300;
    private static final int DISCOVERABLE_TIMEOUT_ONE_HOUR = 3600;
    static final int DISCOVERABLE_TIMEOUT_NEVER = 0;

    private static final String VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES = "twomin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES = "fivemin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR = "onehour";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_NEVER = "never";

    static final int DEFAULT_DISCOVERABLE_TIMEOUT = DISCOVERABLE_TIMEOUT_TWO_MINUTES;

    private final Context mContext;
    private final Handler mUiHandler;
    private final CheckBoxPreference mCheckBoxPreference;
    private final ListPreference mTimeoutListPreference;

    private final LocalBluetoothAdapter mLocalAdapter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR);
                if (mode != BluetoothAdapter.ERROR) {
                    handleModeChanged(mode);
                }
            }
        }
    };

    private final Runnable mUpdateCountdownSummaryRunnable = new Runnable() {
        public void run() {
            updateCountdownSummary();
        }
    };

    BluetoothDiscoverableEnabler(Context context, LocalBluetoothAdapter adapter,
            CheckBoxPreference checkBoxPreference, ListPreference timeoutListPreference) {
        mContext = context;
        mUiHandler = new Handler();
        mCheckBoxPreference = checkBoxPreference;
        mTimeoutListPreference = timeoutListPreference;

        checkBoxPreference.setPersistent(false);
        // we actually want to persist this since can't infer from BT device state
        mTimeoutListPreference.setPersistent(true);

        mLocalAdapter = adapter;
        if (adapter == null) {
            // Bluetooth not supported
            checkBoxPreference.setEnabled(false);
        }
    }

    public void resume() {
        if (mLocalAdapter == null) {
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        mCheckBoxPreference.setOnPreferenceChangeListener(this);
        mTimeoutListPreference.setOnPreferenceChangeListener(this);
        handleModeChanged(mLocalAdapter.getScanMode());
    }

    public void pause() {
        if (mLocalAdapter == null) {
            return;
        }

        mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
        mCheckBoxPreference.setOnPreferenceChangeListener(null);
        mTimeoutListPreference.setOnPreferenceChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mCheckBoxPreference) {
            // Turn on/off BT discoverability
            setEnabled((Boolean) value);
        } else if (preference == mTimeoutListPreference) {
            mTimeoutListPreference.setValue((String) value);
            setEnabled(true);
        }

        return true;
    }

    private void setEnabled(boolean enable) {
        if (enable) {
            int timeout = getDiscoverableTimeout();
            mLocalAdapter.setDiscoverableTimeout(timeout);

            long endTimestamp = System.currentTimeMillis() + timeout * 1000L;
            LocalBluetoothPreferences.persistDiscoverableEndTimestamp(mContext, endTimestamp);

            updateCountdownSummary();

            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } else {
            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        }
    }

    private void updateTimerDisplay(int timeout) {
        if (getDiscoverableTimeout() == DISCOVERABLE_TIMEOUT_NEVER) {
            mCheckBoxPreference.setSummaryOn(
                mContext.getString(R.string.bluetooth_is_discoverable_always));
        } else {
            mCheckBoxPreference.setSummaryOn(
                mContext.getString(R.string.bluetooth_is_discoverable, timeout));
        }
    }

    private int getDiscoverableTimeout() {
        int timeout = SystemProperties.getInt(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, -1);
        if (timeout < 0) {
            String timeoutValue = mTimeoutListPreference.getValue();
            if (timeoutValue == null) {
                mTimeoutListPreference.setValue(VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES);
                return DISCOVERABLE_TIMEOUT_TWO_MINUTES;
            }

            if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_NEVER)) {
                timeout = DISCOVERABLE_TIMEOUT_NEVER;
            } else if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR)) {
                timeout = DISCOVERABLE_TIMEOUT_ONE_HOUR;
            } else if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES)) {
                timeout = DISCOVERABLE_TIMEOUT_FIVE_MINUTES;
            } else {
                timeout = DISCOVERABLE_TIMEOUT_TWO_MINUTES;
            }
        }

        return timeout;
    }

    private void handleModeChanged(int mode) {
        if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            mCheckBoxPreference.setChecked(true);
            updateCountdownSummary();
        } else {
            mCheckBoxPreference.setChecked(false);
        }
    }

    private void updateCountdownSummary() {
        int mode = mLocalAdapter.getScanMode();
        if (mode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            return;
        }

        long currentTimestamp = System.currentTimeMillis();
        long endTimestamp = LocalBluetoothPreferences.getDiscoverableEndTimestamp(mContext);

        if (currentTimestamp > endTimestamp) {
            // We're still in discoverable mode, but maybe there isn't a timeout.
            mCheckBoxPreference.setSummaryOn(null);
            return;
        }

        int timeLeft = (int) ((endTimestamp - currentTimestamp) / 1000L);
        updateTimerDisplay(timeLeft);

        synchronized (this) {
            mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
            mUiHandler.postDelayed(mUpdateCountdownSummaryRunnable, 1000);
        }
    }
}
