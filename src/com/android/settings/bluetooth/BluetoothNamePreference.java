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

import android.app.AlertDialog;
import android.app.Dialog;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.InputFilter.LengthFilter;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;

/**
 * BluetoothNamePreference is the preference type for editing the device's
 * Bluetooth name. It asks the user for a name, and persists it via the
 * Bluetooth API.
 */
public class BluetoothNamePreference extends EditTextPreference implements TextWatcher {
    private static final String TAG = "BluetoothNamePreference";
    // TODO(): Investigate bluetoothd/dbus crash when length is set to 248, limit as per spec.
    private static final int BLUETOOTH_NAME_MAX_LENGTH = 200;

    private LocalBluetoothManager mLocalManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                setSummaryToName();
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) &&
                    (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                            BluetoothAdapter.STATE_ON)) {
                setSummaryToName();
            }
        }
    };

    public BluetoothNamePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLocalManager = LocalBluetoothManager.getInstance(context);

        setSummaryToName();
    }

    public void resume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        getContext().registerReceiver(mReceiver, filter);

        // Make sure the OK button is disabled (if necessary) after rotation
        EditText et = getEditText();
        et.setFilters(new InputFilter[] {new LengthFilter(BLUETOOTH_NAME_MAX_LENGTH)});
        if (et != null) {
            et.addTextChangedListener(this);
            Dialog d = getDialog();
            if (d instanceof AlertDialog) {
                Button b = ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE);
                b.setEnabled(et.getText().length() > 0);
            }
        }
    }

    public void pause() {
        EditText et = getEditText();
        if (et != null) {
            et.removeTextChangedListener(this);
        }
        getContext().unregisterReceiver(mReceiver);
    }

    private void setSummaryToName() {
        BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();
        if (adapter.isEnabled()) {
            setSummary(adapter.getName());
        }
    }

    @Override
    protected boolean persistString(String value) {
        BluetoothAdapter adapter = mLocalManager.getBluetoothAdapter();
        adapter.setName(value);
        return true;
    }

    @Override
    protected void onClick() {
        super.onClick();

        // The dialog should be created by now
        EditText et = getEditText();
        if (et != null) {
            et.setText(mLocalManager.getBluetoothAdapter().getName());
        }
    }

    // TextWatcher interface
    public void afterTextChanged(Editable s) {
        Dialog d = getDialog();
        if (d instanceof AlertDialog) {
            ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(s.length() > 0);
        }
    }

    // TextWatcher interface
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // not used
    }

    // TextWatcher interface
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // not used
    }
}
