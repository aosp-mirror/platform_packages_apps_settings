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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothError;
import android.bluetooth.BluetoothIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
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

    private LocalBluetoothManager mLocalManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothIntent.NAME_CHANGED_ACTION)) {
                setSummaryToName();
            } else if (action.equals(BluetoothIntent.BLUETOOTH_STATE_CHANGED_ACTION) &&
                    (intent.getIntExtra(BluetoothIntent.BLUETOOTH_STATE,
                    BluetoothError.ERROR) == BluetoothDevice.BLUETOOTH_STATE_ON)) {
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
        filter.addAction(BluetoothIntent.BLUETOOTH_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothIntent.NAME_CHANGED_ACTION);
        getContext().registerReceiver(mReceiver, filter);

        // Make sure the OK button is disabled (if necessary) after rotation
        EditText et = getEditText();
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
        BluetoothDevice manager = mLocalManager.getBluetoothManager();
        if (manager.isEnabled()) {
            setSummary(manager.getName());
        }
    }

    @Override
    protected boolean persistString(String value) {
        BluetoothDevice manager = mLocalManager.getBluetoothManager();
        manager.setName(value);
        return true;
    }

    @Override
    protected void onClick() {
        super.onClick();

        // The dialog should be created by now
        EditText et = getEditText();
        if (et != null) {
            et.setText(mLocalManager.getBluetoothManager().getName());
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
