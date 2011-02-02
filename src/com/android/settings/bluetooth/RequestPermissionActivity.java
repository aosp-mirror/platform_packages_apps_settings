/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.settings.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

/**
 * RequestPermissionActivity asks the user whether to enable discovery. This is
 * usually started by an application wanted to start bluetooth and or discovery
 */
public class RequestPermissionActivity extends Activity implements
        DialogInterface.OnClickListener {
    // Command line to test this
    // adb shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
    // adb shell am start -a android.bluetooth.adapter.action.REQUEST_DISCOVERABLE

    private static final String TAG = "RequestPermissionActivity";

    private static final int MAX_DISCOVERABLE_TIMEOUT = 3600; // 1 hr

    // Non-error return code: BT is starting or has started successfully. Used
    // by this Activity and RequestPermissionHelperActivity
    /* package */ static final int RESULT_BT_STARTING_OR_STARTED = -1000;

    private static final int REQUEST_CODE_START_BT = 1;

    private LocalBluetoothManager mLocalManager;

    private int mTimeout = BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT;

    /*
     * True if bluetooth wasn't enabled and RequestPermissionHelperActivity was
     * started to ask the user and start bt.
     *
     * If/when that activity returns successfully, display please wait msg then
     * go away when bt has started and discovery mode has been enabled.
     */
    private boolean mNeededToEnableBluetooth;

    // True if requesting BT to be turned on
    // False if requesting BT to be turned on + discoverable mode
    private boolean mEnableOnly = false;

    private boolean mUserConfirmed = false;

    private AlertDialog mDialog = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null)
                return;
            if (mNeededToEnableBluetooth
                    && BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    if (mUserConfirmed) {
                        proceedAndFinish();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (parseIntent()) {
            finish();
            return;
        }

        int btState = mLocalManager.getBluetoothState();

        switch (btState) {
            case BluetoothAdapter.STATE_OFF:
            case BluetoothAdapter.STATE_TURNING_OFF:
            case BluetoothAdapter.STATE_TURNING_ON:
                /*
                 * Strictly speaking STATE_TURNING_ON belong with STATE_ON;
                 * however, BT may not be ready when the user clicks yes and we
                 * would fail to turn on discovery mode. By kicking this to the
                 * RequestPermissionHelperActivity, this class will handle that
                 * case via the broadcast receiver.
                 */

                /*
                 * Start the helper activity to:
                 * 1) ask the user about enabling bt AND discovery
                 * 2) enable BT upon confirmation
                 */
                registerReceiver(mReceiver,
                        new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                Intent i = new Intent();
                i.setClass(this, RequestPermissionHelperActivity.class);
                if (mEnableOnly) {
                    i.setAction(RequestPermissionHelperActivity.ACTION_INTERNAL_REQUEST_BT_ON);
                } else {
                    i.setAction(RequestPermissionHelperActivity.
                            ACTION_INTERNAL_REQUEST_BT_ON_AND_DISCOVERABLE);
                    i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, mTimeout);
                }
                startActivityForResult(i, REQUEST_CODE_START_BT);
                mNeededToEnableBluetooth = true;
                break;
            case BluetoothAdapter.STATE_ON:
                if (mEnableOnly) {
                    // Nothing to do. Already enabled.
                    proceedAndFinish();
                    return;
                } else {
                    // Ask the user about enabling discovery mode
                    createDialog();
                    break;
                }
        }
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(getString(R.string.bluetooth_permission_request));

        if (mNeededToEnableBluetooth) {
            // RequestPermissionHelperActivity has gotten confirmation from user
            // to turn on BT
            builder.setMessage(getString(R.string.bluetooth_turning_on));
            builder.setCancelable(false);
        } else {
            // Ask the user whether to turn on discovery mode or not
            // For lasting discoverable mode there is a different message
            if (mTimeout == BluetoothDiscoverableEnabler.DISCOVERABLE_TIMEOUT_NEVER) {
                builder.setMessage(
                        getString(R.string.bluetooth_ask_lasting_discovery));
            } else {
                builder.setMessage(
                        getString(R.string.bluetooth_ask_discovery, mTimeout));
            }
            builder.setPositiveButton(getString(R.string.yes), this);
            builder.setNegativeButton(getString(R.string.no), this);
        }

        mDialog = builder.create();
        mDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_START_BT) {
            Log.e(TAG, "Unexpected onActivityResult " + requestCode + " " + resultCode);
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
        if (resultCode != RESULT_BT_STARTING_OR_STARTED) {
            setResult(resultCode);
            finish();
            return;
        }

        // Back from RequestPermissionHelperActivity. User confirmed to enable
        // BT and discoverable mode.
        mUserConfirmed = true;

        if (mLocalManager.getBluetoothState() == BluetoothAdapter.STATE_ON) {
            proceedAndFinish();
        } else {
            // If BT is not up yet, show "Turning on Bluetooth..."
            createDialog();
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                proceedAndFinish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                setResult(Activity.RESULT_CANCELED);
                finish();
                break;
        }
    }

    private void proceedAndFinish() {
        int returnCode;

        if (mEnableOnly) {
            // BT enabled. Done
            returnCode = Activity.RESULT_OK;
        } else if (mLocalManager.getBluetoothAdapter().setScanMode(
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, mTimeout)) {
            // If already in discoverable mode, this will extend the timeout.
            persistDiscoverableEndTimestamp(System.currentTimeMillis() + mTimeout * 1000);
            returnCode = mTimeout;
            // Activity.RESULT_FIRST_USER should be 1
            if (returnCode < Activity.RESULT_FIRST_USER) {
                returnCode = Activity.RESULT_FIRST_USER;
            }
        } else {
            returnCode = Activity.RESULT_CANCELED;
        }

        if (mDialog != null) {
            mDialog.dismiss();
        }

        setResult(returnCode);
        finish();
    }

    private boolean parseIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals(BluetoothAdapter.ACTION_REQUEST_ENABLE)) {
            mEnableOnly = true;
        } else if (intent != null
                && intent.getAction().equals(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)) {
            mTimeout = intent.getIntExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                    BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT);

            Log.d(TAG, "Setting Bluetooth Discoverable Timeout = " + mTimeout);

            if (mTimeout < 0 || mTimeout > MAX_DISCOVERABLE_TIMEOUT) {
                mTimeout = BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT;
            }
        } else {
            Log.e(TAG, "Error: this activity may be started only with intent "
                    + BluetoothAdapter.ACTION_REQUEST_ENABLE + " or "
                    + BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            setResult(Activity.RESULT_CANCELED);
            return true;
        }

        mLocalManager = LocalBluetoothManager.getInstance(this);
        if (mLocalManager == null) {
            Log.e(TAG, "Error: there's a problem starting bluetooth");
            setResult(Activity.RESULT_CANCELED);
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mNeededToEnableBluetooth) unregisterReceiver(mReceiver);
    }

    private void persistDiscoverableEndTimestamp(long endTimestamp) {
        SharedPreferences.Editor editor = mLocalManager.getSharedPreferences().edit();
        editor.putLong(
                BluetoothDiscoverableEnabler.SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP,
                endTimestamp);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}
