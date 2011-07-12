/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.settings.R;

/**
 * Dialog fragment for renaming the local Bluetooth device.
 */
final class BluetoothNameDialogFragment extends DialogFragment implements TextWatcher {
    private static final int BLUETOOTH_NAME_MAX_LENGTH_BYTES = 248;

    private AlertDialog mAlertDialog;
    private Button mOkButton;

    // accessed from inner class (not private to avoid thunks)
    static final String TAG = "BluetoothNameDialogFragment";
    final LocalBluetoothAdapter mLocalAdapter;
    EditText mDeviceNameView;

    // This flag is set when the name is updated by code, to distinguish from user changes
    private boolean mDeviceNameUpdated;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                updateDeviceName();
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) &&
                    (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                            BluetoothAdapter.STATE_ON)) {
                updateDeviceName();
            }
        }
    };

    public BluetoothNameDialogFragment(LocalBluetoothAdapter adapter) {
        mLocalAdapter = adapter;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.bluetooth_rename_device)
                .setView(createDialogView())
                .setPositiveButton(R.string.bluetooth_rename_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (mLocalAdapter != null) {
                                    String deviceName = mDeviceNameView.getText().toString();
                                    Log.d(TAG, "Setting device name to " + deviceName);
                                    mLocalAdapter.setName(deviceName);
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        return mAlertDialog;
    }

    private View createDialogView() {
        final LayoutInflater layoutInflater = (LayoutInflater)getActivity()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_edittext, null);
        mDeviceNameView = (EditText) view.findViewById(R.id.edittext);
        mDeviceNameView.setFilters(new InputFilter[] {
                new Utf8ByteLengthFilter(BLUETOOTH_NAME_MAX_LENGTH_BYTES)
        });
        mDeviceNameView.addTextChangedListener(this);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlertDialog = null;
        mDeviceNameView = null;
        mOkButton = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOkButton == null) {
            mOkButton = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            mOkButton.setEnabled(false);    // Ok button is enabled when the user edits the name
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        getActivity().registerReceiver(mReceiver, filter);
        updateDeviceName();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    void updateDeviceName() {
        if (mLocalAdapter != null && mLocalAdapter.isEnabled()) {
            mDeviceNameUpdated = true;
            mDeviceNameView.setText(mLocalAdapter.getName());
        }
    }

    public void afterTextChanged(Editable s) {
        if (mDeviceNameUpdated) {
            // Device name changed by code; disable Ok button until edited by user
            mDeviceNameUpdated = false;
            mOkButton.setEnabled(false);
        } else {
            mOkButton.setEnabled(s.length() != 0);
        }
    }

    /* Not used */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /* Not used */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
