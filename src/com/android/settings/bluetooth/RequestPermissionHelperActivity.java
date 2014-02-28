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

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * RequestPermissionHelperActivity asks the user whether to enable discovery.
 * This is usually started by RequestPermissionActivity.
 */
public class RequestPermissionHelperActivity extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String TAG = "RequestPermissionHelperActivity";

    public static final String ACTION_INTERNAL_REQUEST_BT_ON =
        "com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_ON";

    public static final String ACTION_INTERNAL_REQUEST_BT_ON_AND_DISCOVERABLE =
        "com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_ON_AND_DISCOVERABLE";

    private LocalBluetoothAdapter mLocalAdapter;

    private int mTimeout;

    // True if requesting BT to be turned on
    // False if requesting BT to be turned on + discoverable mode
    private boolean mEnableOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note: initializes mLocalAdapter and returns true on error
        if (parseIntent()) {
            finish();
            return;
        }

        createDialog();

        if (getResources().getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog) == true) {
            // dismiss dialog immediately if settings say so
            onClick(null, BUTTON_POSITIVE);
            dismiss();
        }
    }

    void createDialog() {
        final AlertController.AlertParams p = mAlertParams;

        if (mEnableOnly) {
            p.mMessage = getString(R.string.bluetooth_ask_enablement);
        } else {
            if (mTimeout == BluetoothDiscoverableEnabler.DISCOVERABLE_TIMEOUT_NEVER) {
                p.mMessage = getString(R.string.bluetooth_ask_enablement_and_lasting_discovery);
            } else {
                p.mMessage = getString(R.string.bluetooth_ask_enablement_and_discovery, mTimeout);
            }
        }

        p.mPositiveButtonText = getString(R.string.allow);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.deny);
        p.mNegativeButtonListener = this;

        setupAlert();
    }

    public void onClick(DialogInterface dialog, int which) {
        int returnCode;
        // FIXME: fix this ugly switch logic!
        switch (which) {
            case BUTTON_POSITIVE:
                int btState = 0;

                try {
                    // TODO There's a better way.
                    int retryCount = 30;
                    do {
                        btState = mLocalAdapter.getBluetoothState();
                        Thread.sleep(100);
                    } while (btState == BluetoothAdapter.STATE_TURNING_OFF && --retryCount > 0);
                } catch (InterruptedException ignored) {
                    // don't care
                }

                if (btState == BluetoothAdapter.STATE_TURNING_ON
                        || btState == BluetoothAdapter.STATE_ON
                        || mLocalAdapter.enable()) {
                    returnCode = RequestPermissionActivity.RESULT_BT_STARTING_OR_STARTED;
                } else {
                    returnCode = RESULT_CANCELED;
                }
                break;

            case BUTTON_NEGATIVE:
                returnCode = RESULT_CANCELED;
                break;
            default:
                return;
        }
        setResult(returnCode);
    }

    /**
     * Parse the received Intent and initialize mLocalBluetoothAdapter.
     * @return true if an error occurred; false otherwise
     */
    private boolean parseIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals(ACTION_INTERNAL_REQUEST_BT_ON)) {
            mEnableOnly = true;
        } else if (intent != null
                && intent.getAction().equals(ACTION_INTERNAL_REQUEST_BT_ON_AND_DISCOVERABLE)) {
            mEnableOnly = false;
            // Value used for display purposes. Not range checking.
            mTimeout = intent.getIntExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                    BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT);
        } else {
            setResult(RESULT_CANCELED);
            return true;
        }

        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(this);
        if (manager == null) {
            Log.e(TAG, "Error: there's a problem starting Bluetooth");
            setResult(RESULT_CANCELED);
            return true;
        }
        mLocalAdapter = manager.getBluetoothAdapter();

        return false;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
