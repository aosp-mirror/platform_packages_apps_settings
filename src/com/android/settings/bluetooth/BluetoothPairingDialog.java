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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * BluetoothPairingDialog asks the user to enter a PIN / Passkey / simple confirmation
 * for pairing with a remote Bluetooth device. It is an activity that appears as a dialog.
 */
public class BluetoothPairingDialog extends AlertActivity implements DialogInterface.OnClickListener,
        TextWatcher {
    private static final String TAG = "BluetoothPairingDialog";

    private final int BLUETOOTH_PIN_MAX_LENGTH = 16;
    private final int BLUETOOTH_PASSKEY_MAX_LENGTH = 6;
    private LocalBluetoothManager mLocalManager;
    private BluetoothDevice mDevice;
    private int mType;
    private String mConfirmationPasskey;
    private EditText mPairingView;
    private Button mOkButton;

    private static final String INSTANCE_KEY_PAIRING_CANCELED = "received_pairing_canceled";
    private boolean mReceivedPairingCanceled;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothDevice.ACTION_PAIRING_CANCEL.equals(intent.getAction())) {
                return;
            }

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || device.equals(mDevice)) {
                onReceivedPairingCanceled();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (!intent.getAction().equals(BluetoothDevice.ACTION_PAIRING_REQUEST))
        {
            Log.e(TAG,
                  "Error: this activity may be started only with intent " +
                  BluetoothDevice.ACTION_PAIRING_REQUEST);
            finish();
        }

        mLocalManager = LocalBluetoothManager.getInstance(this);
        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
        if (mType == BluetoothDevice.PAIRING_VARIANT_PIN) {
            createUserEntryDialog();
        } else if (mType == BluetoothDevice.PAIRING_VARIANT_PASSKEY) {
            createUserEntryDialog();
        } else if (mType == BluetoothDevice.PAIRING_VARIANT_CONFIRMATION){
            int passkey =
                intent.getIntExtra(BluetoothDevice.EXTRA_PASSKEY, BluetoothDevice.ERROR);
            if (passkey == BluetoothDevice.ERROR) {
                Log.e(TAG, "Invalid ConfirmationPasskey received, not showing any dialog");
                return;
            }
            mConfirmationPasskey = String.format("%06d", passkey);
            createConfirmationDialog();
        } else {
            Log.e(TAG, "Incorrect pairing type received, not showing any dialog");
        }

        /*
         * Leave this registered through pause/resume since we still want to
         * finish the activity in the background if pairing is canceled.
         */
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_CANCEL));
    }

    private void createUserEntryDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_pin_entry);
        p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();

        mOkButton = mAlert.getButton(DialogInterface.BUTTON_POSITIVE);
        mOkButton.setEnabled(false);
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, null);

        String name = mLocalManager.getCachedDeviceManager().getName(mDevice);
        TextView messageView = (TextView) view.findViewById(R.id.message);
        mPairingView = (EditText) view.findViewById(R.id.text);
        mPairingView.addTextChangedListener(this);

        if (mType == BluetoothDevice.PAIRING_VARIANT_PIN) {
            messageView.setText(getString(R.string.bluetooth_enter_pin_msg, name));
            // Maximum of 16 characters in a PIN adb sync
            mPairingView.setFilters(new InputFilter[] {
                    new LengthFilter(BLUETOOTH_PIN_MAX_LENGTH) });
        } else if (mType == BluetoothDevice.PAIRING_VARIANT_PASSKEY){
            messageView.setText(getString(R.string.bluetooth_enter_passkey_msg, name));
            // Maximum of 6 digits for passkey
            mPairingView.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
            mPairingView.setFilters(new InputFilter[] {
                    new LengthFilter(BLUETOOTH_PASSKEY_MAX_LENGTH)});
        } else {
            mPairingView.setVisibility(View.GONE);
            messageView.setText(getString(R.string.bluetooth_confirm_passkey_msg, name,
                    mConfirmationPasskey));
        }
        return view;
    }

    private void createConfirmationDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_pin_entry);
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mReceivedPairingCanceled = savedInstanceState.getBoolean(INSTANCE_KEY_PAIRING_CANCELED);
        if (mReceivedPairingCanceled) {
            onReceivedPairingCanceled();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(INSTANCE_KEY_PAIRING_CANCELED, mReceivedPairingCanceled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }

    public void afterTextChanged(Editable s) {
        if (s.length() > 0) {
            mOkButton.setEnabled(true);
        }
    }

    private void onReceivedPairingCanceled() {
        mReceivedPairingCanceled = true;

        TextView messageView = (TextView) findViewById(R.id.message);
        messageView.setText(getString(R.string.bluetooth_pairing_error_message,
                mDevice.getName()));

        mPairingView.setVisibility(View.GONE);
        mPairingView.clearFocus();
        mPairingView.removeTextChangedListener(this);

        mOkButton = mAlert.getButton(DialogInterface.BUTTON_POSITIVE);
        mOkButton.setEnabled(true);
        mOkButton.setText(android.R.string.ok);
        mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
    }

    private void onPair(String value) {
        if (mType == BluetoothDevice.PAIRING_VARIANT_PIN) {
            byte[] pinBytes = BluetoothDevice.convertPinToBytes(value);
            if (pinBytes == null) {
                return;
            }
            mDevice.setPin(pinBytes);
        } else if (mType == BluetoothDevice.PAIRING_VARIANT_PASSKEY) {
            int passkey = Integer.parseInt(value);
            mDevice.setPasskey(passkey);
        } else {
            mDevice.setPairingConfirmation(true);
        }
    }

    private void onCancel() {
        mDevice.cancelPairingUserInput();
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onPair(mPairingView.getText().toString());
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                onCancel();
                break;
        }
    }

    /* Not used */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /* Not used */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

}
