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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.text.TextUtils;
import android.util.Config;

/**
 * BluetoothEnabler is a helper to manage the Bluetooth on/off checkbox
 * preference. It is turns on/off Bluetooth and ensures the summary of the
 * preference reflects the current state.
 */
public class BluetoothEnabler implements Preference.OnPreferenceChangeListener {

    private static final boolean LOCAL_LOGD = Config.LOGD || false;
    private static final String TAG = "BluetoothEnabler";

    private final Context mContext;
    private final CheckBoxPreference mCheckBoxPreference;
    private final CharSequence mOriginalSummary;

    private final LocalBluetoothManager mLocalManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            handleStateChanged(state);
        }
    };

    public BluetoothEnabler(Context context, CheckBoxPreference checkBoxPreference) {
        mContext = context;
        mCheckBoxPreference = checkBoxPreference;

        mOriginalSummary = checkBoxPreference.getSummary();
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

        int state = mLocalManager.getBluetoothState();
        // This is the widget enabled state, not the preference toggled state
        mCheckBoxPreference.setEnabled(state == BluetoothAdapter.STATE_ON ||
                state == BluetoothAdapter.STATE_OFF);
        // BT state is not a sticky broadcast, so set it manually
        handleStateChanged(state);

        mContext.registerReceiver(mReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        mCheckBoxPreference.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        if (mLocalManager == null) {
            return;
        }

        mContext.unregisterReceiver(mReceiver);
        mCheckBoxPreference.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn on/off BT
        setEnabled((Boolean) value);

        // Don't update UI to opposite state until we're sure
        return false;
    }

    private void setEnabled(final boolean enable) {
        // Disable preference
        mCheckBoxPreference.setEnabled(false);

        mLocalManager.setBluetoothEnabled(enable);
    }

    private void handleStateChanged(int state) {

        if (state == BluetoothAdapter.STATE_OFF ||
                state == BluetoothAdapter.STATE_ON) {
            mCheckBoxPreference.setChecked(state == BluetoothAdapter.STATE_ON);
            mCheckBoxPreference.setSummary(state == BluetoothAdapter.STATE_OFF ?
                                           mOriginalSummary :
                                           null);

            /*
             * Don't ever disable the preference. Only enable here. Disablement
             * is taken care of by the dependency code. If this is disabled
             * here, it may not be re-enabled from the framework when dependency
             * is met. http://b/issue?id=2053751
             */
            if (isEnabledByDependency()) {
                mCheckBoxPreference.setEnabled(true);
            }

        } else if (state == BluetoothAdapter.STATE_TURNING_ON ||
                state == BluetoothAdapter.STATE_TURNING_OFF) {
            mCheckBoxPreference.setSummary(state == BluetoothAdapter.STATE_TURNING_ON
                    ? R.string.wifi_starting
                    : R.string.wifi_stopping);

        } else {
            mCheckBoxPreference.setChecked(false);
            mCheckBoxPreference.setSummary(R.string.wifi_error);
            mCheckBoxPreference.setEnabled(true);
        }
    }

    private boolean isEnabledByDependency() {
        Preference dep = getDependencyPreference();
        if (dep == null) {
            return true;
        }

        return !dep.shouldDisableDependents();
    }

    private Preference getDependencyPreference() {
        String depKey = mCheckBoxPreference.getDependency();
        if (TextUtils.isEmpty(depKey)) {
            return null;
        }

        return mCheckBoxPreference.getPreferenceManager().findPreference(depKey);
    }

}
