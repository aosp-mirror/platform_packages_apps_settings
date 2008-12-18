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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * BluetoothPinDialog asks the user to enter a PIN for pairing with a remote
 * Bluetooth device. It is an activity that appears as a dialog.
 */
public class BluetoothPinDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "BluetoothPinDialog";

    private LocalBluetoothManager mLocalManager;
    private String mAddress;
    private EditText mPinView;
    
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
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, null);
        
        String name = mLocalManager.getLocalDeviceManager().getName(mAddress);
        TextView messageView = (TextView) view.findViewById(R.id.message);
        messageView.setText(getString(R.string.bluetooth_enter_pin_msg, name));
        
        mPinView = (EditText) view.findViewById(R.id.text);
        
        return view;
    }
    
    private void onPair(String pin) {
        byte[] pinBytes = BluetoothDevice.convertPinToBytes(pin);
        
        if (pinBytes == null) {
            return;
        }
        
        mLocalManager.getBluetoothManager().setPin(mAddress, pinBytes);
    }

    private void onCancel() {
        mLocalManager.getBluetoothManager().cancelPin(mAddress);
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

}
