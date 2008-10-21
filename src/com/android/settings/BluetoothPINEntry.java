/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BluetoothPINEntry extends BluetoothDataEntry {
    private BluetoothDevice mBluetooth;
    private String mAddress;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (!intent.getAction().equals(BluetoothIntent.PAIRING_REQUEST_ACTION))
        {
            Log.e(this.getClass().getName(),
                  "Error: this activity may be started only with intent " +
                  BluetoothIntent.PAIRING_REQUEST_ACTION);
            finish();
        }
        
        // Cancel the notification, if any
        NotificationManager manager = (NotificationManager) 
        getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(0xb100ceee);
        
        mAddress = intent.getStringExtra(BluetoothIntent.ADDRESS);

        mBluetooth = (BluetoothDevice)getSystemService(BLUETOOTH_SERVICE);
        
        String remoteName = mBluetooth.getRemoteName(mAddress);
        if (remoteName == null) {
            remoteName = mAddress;
        }
            
        mDataLabel.setText(getString(R.string.bluetooth_enter_pin_msg) + remoteName);
    }

    @Override
    public void activityResult(int result, String data, Bundle extras) {
        switch (result) {
        case RESULT_OK:
            byte[] pin = BluetoothDevice.convertPinToBytes(mDataEntry.getText().toString());
            if (pin == null) {
                return;
            }
            mBluetooth.setPin(mAddress, pin);
            break;
        case RESULT_CANCELED:
            mBluetooth.cancelPin(mAddress);
            break;
        }
        finish();
    }
}
