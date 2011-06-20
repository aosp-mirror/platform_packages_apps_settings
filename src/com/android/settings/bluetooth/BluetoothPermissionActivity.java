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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import com.android.settings.R;

/**
 * BluetoothPermissionActivity shows a dialog for accepting incoming
 * profile connection request from untrusted devices.
 */
public class BluetoothPermissionActivity extends AlertActivity implements
        DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = "BluetoothPermissionActivity";
    private static final boolean DEBUG = false;

    private static final String KEY_USER_TIMEOUT = "user_timeout";

    private View mView;
    private TextView messageView;
    private Button mOkButton;
    private BluetoothDevice mDevice;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.equals(device)) dismissDialog();
            }
        }
    };

    private void dismissDialog() {
        this.dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DEBUG) Log.d(TAG, "onCreate");
        Intent i = getIntent();
        String action = i.getAction();
        if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_REQUEST)) {
            mDevice = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            showConnectionDialog();
            registerReceiver(mReceiver,
                             new IntentFilter(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL));
        } else {
            Log.e(TAG, "Error: this activity may be started only with intent "
                    + "ACTION_CONNECTION_ACCESS_REQUEST");
            finish();
        }
    }

    private void showConnectionDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_connection_permission_request);
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.yes);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.no);
        p.mNegativeButtonListener = this;
        mOkButton = mAlert.getButton(DialogInterface.BUTTON_POSITIVE);
        setupAlert();
    }

    private String createDisplayText() {
        String mRemoteName = mDevice != null ? mDevice.getName() : null;
        if (mRemoteName == null) mRemoteName = getString(R.string.unknown);
        String mMessage1 = getString(R.string.bluetooth_connection_dialog_text,
                mRemoteName);
        return mMessage1;
    }

    private View createView() {
        mView = getLayoutInflater().inflate(R.layout.bluetooth_connection_access, null);
        messageView = (TextView)mView.findViewById(R.id.message);
        messageView.setText(createDisplayText());
        return mView;
    }

    private void onPositive() {
        sendIntentToReceiver(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY,
                             BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                             BluetoothDevice.CONNECTION_ACCESS_YES);
        finish();
    }

    private void onNegative() {
        sendIntentToReceiver(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY,
                             BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                             BluetoothDevice.CONNECTION_ACCESS_NO);
        finish();
    }

    private void sendIntentToReceiver(final String intentName, final String extraName,
            final int extraValue) {
        Intent intent = new Intent(intentName);
        if (extraName != null) {
            intent.putExtra(extraName, extraValue);
        }
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        sendBroadcast(intent, android.Manifest.permission.BLUETOOTH_ADMIN);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onPositive();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                onNegative();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }
}
