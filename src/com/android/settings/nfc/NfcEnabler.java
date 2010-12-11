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

package com.android.settings.nfc;

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.Log;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It is
 * turns on/off Nfc and ensures the summary of the preference reflects the
 * current state.
 */
public class NfcEnabler implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "NfcEnabler";

    private final Context mContext;
    private final CheckBoxPreference mCheckbox;
    private final NfcAdapter mNfcAdapter;
    private final IntentFilter mIntentFilter;
    private final Handler mHandler = new Handler();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGE.equals(action)) {
                handleNfcStateChanged(intent.getBooleanExtra(
                    NfcAdapter.EXTRA_NEW_BOOLEAN_STATE,
                    false));
            }
        }
    };

    private boolean mNfcState;

    public NfcEnabler(Context context, CheckBoxPreference checkBoxPreference) {
        mContext = context;
        mCheckbox = checkBoxPreference;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);

        if (mNfcAdapter == null) {
            // NFC is not supported
            mCheckbox.setEnabled(false);
        }

        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGE);

    }

    public void resume() {
        if (mNfcAdapter == null) {
            return;
        }
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mCheckbox.setOnPreferenceChangeListener(this);
        mNfcState = mNfcAdapter.isEnabled();
        mCheckbox.setChecked(mNfcState);
    }

    public void pause() {
        if (mNfcAdapter == null) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        mCheckbox.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn NFC on/off

        final boolean desiredState = (Boolean) value;
        mCheckbox.setEnabled(false);

        // Start async update of the NFC adapter state, as the API is
        // unfortunately blocking...
        new Thread("toggleNFC") {
            public void run() {
                Log.d(TAG, "Setting NFC enabled state to: " + desiredState);
                boolean success = false;
                if (desiredState) {
                    success = mNfcAdapter.enable();
                } else {
                    success = mNfcAdapter.disable();
                }
                if (success) {
                    Log.d(TAG, "Successfully changed NFC enabled state to " + desiredState);
                    mHandler.post(new Runnable() {
                        public void run() {
                            handleNfcStateChanged(desiredState);
                        }
                    });
                } else {
                    Log.w(TAG, "Error setting NFC enabled state to " + desiredState);
                    mHandler.post(new Runnable() {
                            public void run() {
                                mCheckbox.setEnabled(true);
                                mCheckbox.setSummary(R.string.nfc_toggle_error);
                            }
                        });
                }
            }
        }.start();
        return false;
    }

    private void handleNfcStateChanged(boolean newState) {
        mCheckbox.setChecked(newState);
        mCheckbox.setEnabled(true);
        mCheckbox.setSummary(R.string.nfc_quick_toggle_summary);
    }
}
