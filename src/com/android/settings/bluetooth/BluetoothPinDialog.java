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
import android.bluetooth.BluetoothIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
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
 * BluetoothPinDialog asks the user to enter a PIN for pairing with a remote
 * Bluetooth device. It is an activity that appears as a dialog.
 */
public class BluetoothPinDialog extends AlertActivity implements DialogInterface.OnClickListener,
        TextWatcher {
    private static final String TAG = "BluetoothPinDialog";

    private final int BLUETOOTH_PIN_MAX_LENGTH = 16;
    private LocalBluetoothManager mLocalManager;
    private String mAddress;
    private EditText mPinView;
    private Button mOkButton;

    private static final String INSTANCE_KEY_PAIRING_CANCELED = "received_pairing_canceled";
    private boolean mReceivedPairingCanceled;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothIntent.PAIRING_CANCEL_ACTION.equals(intent.getAction())) {
                return;
            }
            
            String address = intent.getStringExtra(BluetoothIntent.ADDRESS);
            if (address == null || address.equals(mAddress)) {
                onReceivedPairingCanceled();
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (!intent.getAction().equals(BluetoothIntent.PAIRING_REQUEST_ACTION))
        {
            Log.e(TAG,
                  "Error: this activity may be started only with intent " +
                  BluetoothIntent.PAIRING_REQUEST_ACTION);
            finish();
        }
        
        mLocalManager = LocalBluetoothManager.getInstance(this);
        mAddress = intent.getStringExtra(BluetoothIntent.ADDRESS);
        
        // Set up the "dialog"
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

        /*
         * Leave this registered through pause/resume since we still want to
         * finish the activity in the background if pairing is canceled.
         */
        registerReceiver(mReceiver, new IntentFilter(BluetoothIntent.PAIRING_CANCEL_ACTION));
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

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, null);
        
        String name = mLocalManager.getLocalDeviceManager().getName(mAddress);
        TextView messageView = (TextView) view.findViewById(R.id.message);
        messageView.setText(getString(R.string.bluetooth_enter_pin_msg, name));
        
        mPinView = (EditText) view.findViewById(R.id.text);
        mPinView.addTextChangedListener(this);
        // Maximum of 16 characters in a PIN
        mPinView.setFilters(new InputFilter[] { new LengthFilter(BLUETOOTH_PIN_MAX_LENGTH) });
        
        return view;
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
                mLocalManager.getLocalDeviceManager().getName(mAddress)));
        
        mPinView.setVisibility(View.GONE);
        mPinView.clearFocus();
        mPinView.removeTextChangedListener(this);

        mOkButton.setEnabled(true);
        mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
    }
    
    private void onPair(String pin) {
        byte[] pinBytes = BluetoothDevice.convertPinToBytes(pin);
        
        if (pinBytes == null) {
            return;
        }
        
        mLocalManager.getBluetoothManager().setPin(mAddress, pinBytes);
    }

    private void onCancel() {
        mLocalManager.getBluetoothManager().cancelBondProcess(mAddress);
    }
    
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onPair(mPinView.getText().toString());
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
