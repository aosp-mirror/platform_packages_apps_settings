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
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.Preference;
import android.text.format.DateUtils;

import com.android.settings.R;

import android.text.format.Time;
import android.util.Log;

/**
 * BluetoothDiscoverableEnabler is a helper to manage the "Discoverable"
 * checkbox. It sets/unsets discoverability and keeps track of how much time
 * until the the discoverability is automatically turned off.
 */
final class BluetoothDiscoverableEnabler implements Preference.OnPreferenceClickListener {

    private static final String TAG = "BluetoothDiscoverableEnabler";

    private static final String SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT =
            "debug.bt.discoverable_time";

    private static final int DISCOVERABLE_TIMEOUT_TWO_MINUTES = 120;
    private static final int DISCOVERABLE_TIMEOUT_FIVE_MINUTES = 300;
    private static final int DISCOVERABLE_TIMEOUT_ONE_HOUR = 3600;
    static final int DISCOVERABLE_TIMEOUT_NEVER = 0;

    // Bluetooth advanced settings screen was replaced with action bar items.
    // Use the same preference key for discoverable timeout as the old ListPreference.
    private static final String KEY_DISCOVERABLE_TIMEOUT = "bt_discoverable_timeout";

    private static final String VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES = "twomin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES = "fivemin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR = "onehour";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_NEVER = "never";

    static final int DEFAULT_DISCOVERABLE_TIMEOUT = DISCOVERABLE_TIMEOUT_TWO_MINUTES;

    private final Context mContext;
    private final Handler mUiHandler;
    private final Preference mDiscoveryPreference;

    private final LocalBluetoothAdapter mLocalAdapter;

    private final SharedPreferences mSharedPreferences;

    private boolean mDiscoverable;
    private int mNumberOfPairedDevices;

    private int mTimeoutSecs = -1;

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
            Preference discoveryPreference) {
        mContext = context;
        mUiHandler = new Handler();
        mLocalAdapter = adapter;
        mDiscoveryPreference = discoveryPreference;
        mSharedPreferences = discoveryPreference.getSharedPreferences();
        discoveryPreference.setPersistent(false);
    }

    public void resume() {
        if (mLocalAdapter == null) {
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        mDiscoveryPreference.setOnPreferenceClickListener(this);
        handleModeChanged(mLocalAdapter.getScanMode());
    }

    public void pause() {
        if (mLocalAdapter == null) {
            return;
        }

        mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
        mContext.unregisterReceiver(mReceiver);
        mDiscoveryPreference.setOnPreferenceClickListener(null);
    }

    public boolean onPreferenceClick(Preference preference) {
        // toggle discoverability
        mDiscoverable = !mDiscoverable;
        setEnabled(mDiscoverable);
        return true;
    }

    private void setEnabled(boolean enable) {
        if (enable) {
            int timeout = getDiscoverableTimeout();
            long endTimestamp = System.currentTimeMillis() + timeout * 1000L;
            LocalBluetoothPreferences.persistDiscoverableEndTimestamp(mContext, endTimestamp);

            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
            updateCountdownSummary();

            Log.d(TAG, "setEnabled(): enabled = " + enable + "timeout = " + timeout);

            if (timeout > 0) {
                BluetoothDiscoverableTimeoutReceiver.setDiscoverableAlarm(mContext, endTimestamp);
            }
            else if(timeout == 0) {
                // cancel the previous alarms set for 2 mims/5 mins/ 1 hour
                BluetoothDiscoverableTimeoutReceiver.cancelDiscoverableAlarm(mContext);
            }
        } else {
            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
            BluetoothDiscoverableTimeoutReceiver.cancelDiscoverableAlarm(mContext);
        }
    }

    private void updateTimerDisplay(int timeout) {
        if (getDiscoverableTimeout() == DISCOVERABLE_TIMEOUT_NEVER) {
            mDiscoveryPreference.setSummary(R.string.bluetooth_is_discoverable_always);
        } else {
            String textTimeout = formatTimeRemaining(timeout);
            mDiscoveryPreference.setSummary(mContext.getString(R.string.bluetooth_is_discoverable,
                    textTimeout));
        }
    }

    private static String formatTimeRemaining(int timeout) {
        StringBuilder sb = new StringBuilder(6);    // "mmm:ss"
        int min = timeout / 60;
        sb.append(min).append(':');
        int sec = timeout - (min * 60);
        if (sec < 10) {
            sb.append('0');
        }
        sb.append(sec);
        return sb.toString();
    }

    void setDiscoverableTimeout(int index) {
        String timeoutValue;
        switch (index) {
            case 0:
            default:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_TWO_MINUTES;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES;
                break;

            case 1:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_FIVE_MINUTES;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES;
                break;

            case 2:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_ONE_HOUR;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR;
                break;

            case 3:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_NEVER;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_NEVER;
                break;
        }
        mSharedPreferences.edit().putString(KEY_DISCOVERABLE_TIMEOUT, timeoutValue).apply();
        setEnabled(true);   // enable discovery and reset timer
    }

    private int getDiscoverableTimeout() {
        if (mTimeoutSecs != -1) {
            return mTimeoutSecs;
        }

        int timeout = SystemProperties.getInt(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, -1);
        if (timeout < 0) {
            String timeoutValue = mSharedPreferences.getString(KEY_DISCOVERABLE_TIMEOUT,
                    VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES);

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
        mTimeoutSecs = timeout;
        return timeout;
    }

    int getDiscoverableTimeoutIndex() {
        int timeout = getDiscoverableTimeout();
        switch (timeout) {
            case DISCOVERABLE_TIMEOUT_TWO_MINUTES:
            default:
                return 0;

            case DISCOVERABLE_TIMEOUT_FIVE_MINUTES:
                return 1;

            case DISCOVERABLE_TIMEOUT_ONE_HOUR:
                return 2;

            case DISCOVERABLE_TIMEOUT_NEVER:
                return 3;
        }
    }

    void setNumberOfPairedDevices(int pairedDevices) {
        mNumberOfPairedDevices = pairedDevices;
        handleModeChanged(mLocalAdapter.getScanMode());
    }

    void handleModeChanged(int mode) {
        Log.d(TAG, "handleModeChanged(): mode = " + mode);
        if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            mDiscoverable = true;
            updateCountdownSummary();
        } else {
            mDiscoverable = false;
            setSummaryNotDiscoverable();
        }
    }

    private void setSummaryNotDiscoverable() {
        if (mNumberOfPairedDevices != 0) {
            mDiscoveryPreference.setSummary(R.string.bluetooth_only_visible_to_paired_devices);
        } else {
            mDiscoveryPreference.setSummary(R.string.bluetooth_not_visible_to_other_devices);
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
            updateTimerDisplay(0);
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
