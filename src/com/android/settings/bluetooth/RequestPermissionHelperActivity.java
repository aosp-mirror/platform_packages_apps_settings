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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * RequestPermissionHelperActivity asks the user whether to toggle Bluetooth.
 *
 * TODO: This activity isn't needed - this should be folded in RequestPermissionActivity
 */
public class RequestPermissionHelperActivity extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String TAG = "RequestPermissionHelperActivity";

    public static final String ACTION_INTERNAL_REQUEST_BT_ON =
            "com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_ON";

    public static final String ACTION_INTERNAL_REQUEST_BT_OFF =
            "com.android.settings.bluetooth.ACTION_INTERNAL_REQUEST_BT_OFF";

    public static final String EXTRA_APP_LABEL =
            "com.android.settings.bluetooth.extra.APP_LABEL";

    private BluetoothAdapter mBluetoothAdapter;

    private CharSequence mAppLabel;

    private int mTimeout = -1;

    private int mRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        // Note: initializes mBluetoothAdapter and returns true on error
        if (!parseIntent()) {
            finish();
            return;
        }

        if (getResources().getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog)) {
            // Don't even show the dialog if configured this way
            onClick(null, BUTTON_POSITIVE);
            dismiss();
        }

        createDialog();
    }

    void createDialog() {
        final AlertController.AlertParams p = mAlertParams;

        switch (mRequest) {
            case RequestPermissionActivity.REQUEST_ENABLE: {
                if (mTimeout < 0) {
                    p.mMessage = mAppLabel != null
                            ? getString(R.string.bluetooth_ask_enablement, mAppLabel)
                            : getString(R.string.bluetooth_ask_enablement_no_name);
                } else if (mTimeout == BluetoothDiscoverableEnabler.DISCOVERABLE_TIMEOUT_NEVER) {
                    p.mMessage = mAppLabel != null
                            ? getString(
                                   R.string.bluetooth_ask_enablement_and_lasting_discovery,
                                   mAppLabel)
                            : getString(
                                   R.string.bluetooth_ask_enablement_and_lasting_discovery_no_name);
                } else {
                    p.mMessage = mAppLabel != null
                            ? getString(R.string.bluetooth_ask_enablement_and_discovery,
                                    mAppLabel, mTimeout)
                            : getString(R.string.bluetooth_ask_enablement_and_discovery_no_name,
                                    mTimeout);
                }
            } break;

            case RequestPermissionActivity.REQUEST_DISABLE: {
                p.mMessage = mAppLabel != null
                        ? getString(R.string.bluetooth_ask_disablement, mAppLabel)
                        : getString(R.string.bluetooth_ask_disablement_no_name);
            } break;
        }

        p.mPositiveButtonText = getString(R.string.allow);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.deny);

        setupAlert();
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (mRequest) {
            case RequestPermissionActivity.REQUEST_ENABLE:
            case RequestPermissionActivity.REQUEST_ENABLE_DISCOVERABLE: {
                UserManager userManager = getSystemService(UserManager.class);
                if (userManager.hasUserRestriction(UserManager.DISALLOW_BLUETOOTH)) {
                    // If Bluetooth is disallowed, don't try to enable it, show policy transparency
                    // message instead.
                    DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
                    Intent intent = dpm.createAdminSupportIntent(UserManager.DISALLOW_BLUETOOTH);
                    if (intent != null) {
                        startActivity(intent);
                    }
                } else {
                    mBluetoothAdapter.enable();
                    setResult(Activity.RESULT_OK);
                }
            } break;

            case RequestPermissionActivity.REQUEST_DISABLE: {
                mBluetoothAdapter.disable();
                setResult(Activity.RESULT_OK);
            } break;
        }
    }

    /**
     * Parse the received Intent and initialize mBluetoothAdapter.
     * @return true if an error occurred; false otherwise
     */
    private boolean parseIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        if (ACTION_INTERNAL_REQUEST_BT_ON.equals(action)) {
            mRequest = RequestPermissionActivity.REQUEST_ENABLE;
            if (intent.hasExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION)) {
                // Value used for display purposes. Not range checking.
                mTimeout = intent.getIntExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                        BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT);
            }
        } else if (ACTION_INTERNAL_REQUEST_BT_OFF.equals(action)) {
            mRequest = RequestPermissionActivity.REQUEST_DISABLE;
        } else {
            return false;
        }

        mAppLabel = getIntent().getCharSequenceExtra(EXTRA_APP_LABEL);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Error: there's a problem starting Bluetooth");
            return false;
        }

        return true;
    }
}
