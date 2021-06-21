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

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * BluetoothPermissionActivity shows a dialog for accepting incoming
 * profile connection request from untrusted devices.
 * It is also used to show a dialogue for accepting incoming phonebook
 * read request. The request could be initiated by PBAP PCE or by HF AT+CPBR.
 */
public class BluetoothPermissionActivity extends AlertActivity implements
        DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = "BluetoothPermissionActivity";
    private static final boolean DEBUG = Utils.D;

    private View mView;
    private TextView messageView;
    private Button mOkButton;
    private BluetoothDevice mDevice;

    private int mRequestType = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL)) {
                int requestType = intent.getIntExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                        BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS);
                if (requestType != mRequestType) return;
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.equals(device)) dismissDialog();
            }
        }
    };
    private boolean mReceiverRegistered = false;

    private void dismissDialog() {
        this.dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addPrivateFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        Intent i = getIntent();
        String action = i.getAction();
        if (!action.equals(BluetoothDevice.ACTION_CONNECTION_ACCESS_REQUEST)) {
            Log.e(TAG, "Error: this activity may be started only with intent "
                  + "ACTION_CONNECTION_ACCESS_REQUEST");
            finish();
            return;
        }

        mDevice = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mRequestType = i.getIntExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE,
                                     BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS);

        if(DEBUG) Log.i(TAG, "onCreate() Request type: " + mRequestType);

        if (mRequestType == BluetoothDevice.REQUEST_TYPE_PROFILE_CONNECTION) {
            showDialog(getString(R.string.bluetooth_connect_access_dialog_title), mRequestType);
        } else if (mRequestType == BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS) {
            showDialog(getString(R.string.bluetooth_phonebook_access_dialog_title), mRequestType);
        } else if (mRequestType == BluetoothDevice.REQUEST_TYPE_MESSAGE_ACCESS) {
            showDialog(getString(R.string.bluetooth_message_access_dialog_title), mRequestType);
        } else if (mRequestType == BluetoothDevice.REQUEST_TYPE_SIM_ACCESS) {
            showDialog(getString(R.string.bluetooth_sim_card_access_dialog_title), mRequestType);
        }
        else {
            Log.e(TAG, "Error: bad request type: " + mRequestType);
            finish();
            return;
        }
        registerReceiver(mReceiver,
                         new IntentFilter(BluetoothDevice.ACTION_CONNECTION_ACCESS_CANCEL));
        mReceiverRegistered = true;
    }


    private void showDialog(String title, int requestType)
    {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = title;
        if(DEBUG) Log.i(TAG, "showDialog() Request type: " + mRequestType + " this: " + this);
        switch(requestType)
        {
        case BluetoothDevice.REQUEST_TYPE_PROFILE_CONNECTION:
            p.mView = createConnectionDialogView();
            break;
        case BluetoothDevice.REQUEST_TYPE_PHONEBOOK_ACCESS:
            p.mView = createPhonebookDialogView();
            break;
        case BluetoothDevice.REQUEST_TYPE_MESSAGE_ACCESS:
            p.mView = createMapDialogView();
            break;
        case BluetoothDevice.REQUEST_TYPE_SIM_ACCESS:
            p.mView = createSapDialogView();
            break;
        }
        p.mPositiveButtonText = getString(
                requestType == BluetoothDevice.REQUEST_TYPE_PROFILE_CONNECTION
                        ? R.string.bluetooth_connect_access_dialog_positive : R.string.allow);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(
                requestType == BluetoothDevice.REQUEST_TYPE_PROFILE_CONNECTION
                        ? R.string.bluetooth_connect_access_dialog_negative
                        : R.string.request_manage_bluetooth_permission_dont_allow);
        p.mNegativeButtonListener = this;
        mOkButton = mAlert.getButton(DialogInterface.BUTTON_POSITIVE);
        setupAlert();

    }
    @Override
    public void onBackPressed() {
        /*we need an answer so ignore back button presses during auth */
        if(DEBUG) Log.i(TAG, "Back button pressed! ignoring");
        return;
    }

    // TODO(edjee): createConnectionDialogView, createPhonebookDialogView and createMapDialogView
    // are similar. Refactor them into one method.
    // Also, the string resources bluetooth_remember_choice and bluetooth_pb_remember_choice should
    // be removed.
    private View createConnectionDialogView() {
        String mRemoteName = Utils.createRemoteName(this, mDevice);
        mView = getLayoutInflater().inflate(R.layout.bluetooth_access, null);
        messageView = (TextView)mView.findViewById(R.id.message);
        messageView.setText(getString(R.string.bluetooth_connect_access_dialog_content,
                mRemoteName, mRemoteName));
        return mView;
    }

    private View createPhonebookDialogView() {
        String mRemoteName = Utils.createRemoteName(this, mDevice);
        mView = getLayoutInflater().inflate(R.layout.bluetooth_access, null);
        messageView = (TextView)mView.findViewById(R.id.message);
        messageView.setText(getString(R.string.bluetooth_phonebook_access_dialog_content,
                mRemoteName, mRemoteName));
        return mView;
    }

    private View createMapDialogView() {
        String mRemoteName = Utils.createRemoteName(this, mDevice);
        mView = getLayoutInflater().inflate(R.layout.bluetooth_access, null);
        messageView = (TextView)mView.findViewById(R.id.message);
        messageView.setText(getString(R.string.bluetooth_message_access_dialog_content,
                mRemoteName, mRemoteName));
        return mView;
    }

    private View createSapDialogView() {
        String mRemoteName = Utils.createRemoteName(this, mDevice);
        TelephonyManager tm = getSystemService(TelephonyManager.class);
        mView = getLayoutInflater().inflate(R.layout.bluetooth_access, null);
        messageView = (TextView)mView.findViewById(R.id.message);
        messageView.setText(getString(R.string.bluetooth_sim_card_access_dialog_content,
                mRemoteName, mRemoteName, tm.getLine1Number()));
        return mView;
    }

    private void onPositive() {
        if (DEBUG) Log.d(TAG, "onPositive");
        sendReplyIntentToReceiver(true, true);
        finish();
    }

    private void onNegative() {
        if (DEBUG) Log.d(TAG, "onNegative");
        sendReplyIntentToReceiver(false, true);
    }

    @VisibleForTesting
    void sendReplyIntentToReceiver(final boolean allowed, final boolean always) {
        Intent intent = new Intent(BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY);

        if (DEBUG) {
            Log.i(TAG, "sendReplyIntentToReceiver() Request type: " + mRequestType
                    + " mReturnPackage");
        }

        intent.putExtra(BluetoothDevice.EXTRA_CONNECTION_ACCESS_RESULT,
                        allowed ? BluetoothDevice.CONNECTION_ACCESS_YES
                                : BluetoothDevice.CONNECTION_ACCESS_NO);
        intent.putExtra(BluetoothDevice.EXTRA_ALWAYS_ALLOWED, always);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.putExtra(BluetoothDevice.EXTRA_ACCESS_REQUEST_TYPE, mRequestType);
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
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }
}
