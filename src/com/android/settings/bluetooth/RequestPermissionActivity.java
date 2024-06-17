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

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothDiscoverableTimeoutReceiver;

import kotlin.Unit;

import java.time.Duration;

/**
 * RequestPermissionActivity asks the user whether to enable discovery. This is
 * usually started by an application wanted to start bluetooth and or discovery
 */
public class RequestPermissionActivity extends Activity implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    // Command line to test this
    // adb shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
    // adb shell am start -a android.bluetooth.adapter.action.REQUEST_DISCOVERABLE
    // adb shell am start -a android.bluetooth.adapter.action.REQUEST_DISABLE

    private static final String TAG = "BtRequestPermission";

    private static final int MAX_DISCOVERABLE_TIMEOUT = 3600; // 1 hr

    static final int REQUEST_ENABLE = 1;
    static final int REQUEST_ENABLE_DISCOVERABLE = 2;
    static final int REQUEST_DISABLE = 3;

    private BluetoothAdapter mBluetoothAdapter;

    private int mTimeout = BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT;

    private int mRequest;

    private AlertDialog mDialog;
    private AlertDialog mRequestDialog;

    private BroadcastReceiver mReceiver;

    private @NonNull CharSequence mAppLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        setResult(Activity.RESULT_CANCELED);

        // Note: initializes mBluetoothAdapter and returns true on error
        if (parseIntent()) {
            finish();
            return;
        }

        int btState = mBluetoothAdapter.getState();

        if (mRequest == REQUEST_DISABLE) {
            switch (btState) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    proceedAndFinish();
                    break;
                case BluetoothAdapter.STATE_ON:
                case BluetoothAdapter.STATE_TURNING_ON:
                    mRequestDialog =
                            RequestPermissionHelper.INSTANCE.requestDisable(this, mAppLabel,
                                    () -> {
                                        onDisableConfirmed();
                                        return Unit.INSTANCE;
                                    },
                                    () -> {
                                        cancelAndFinish();
                                        return Unit.INSTANCE;
                                    });
                    if (mRequestDialog != null) {
                        mRequestDialog.show();
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown adapter state: " + btState);
                    cancelAndFinish();
                    break;
            }
        } else {
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

                    // Show the helper dialog to ask the user about enabling bt AND discovery
                    mRequestDialog =
                            RequestPermissionHelper.INSTANCE.requestEnable(this, mAppLabel,
                                    mRequest == REQUEST_ENABLE_DISCOVERABLE ? mTimeout : -1,
                                    () -> {
                                        onEnableConfirmed();
                                        return Unit.INSTANCE;
                                    },
                                    () -> {
                                        cancelAndFinish();
                                        return Unit.INSTANCE;
                                    });
                    if (mRequestDialog != null) {
                        mRequestDialog.show();
                    }
                    break;
                case BluetoothAdapter.STATE_ON:
                    if (mRequest == REQUEST_ENABLE) {
                        // Nothing to do. Already enabled.
                        proceedAndFinish();
                    } else {
                        // Ask the user about enabling discovery mode
                        createDialog();
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown adapter state: " + btState);
                    cancelAndFinish();
                    break;
            }
        }
    }

    private void createDialog() {
        if (getResources().getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog)) {
            onClick(null, DialogInterface.BUTTON_POSITIVE);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Non-null receiver means we are toggling
        if (mReceiver != null) {
            switch (mRequest) {
                case REQUEST_ENABLE:
                case REQUEST_ENABLE_DISCOVERABLE: {
                    builder.setMessage(getString(R.string.bluetooth_turning_on));
                } break;

                default: {
                    builder.setMessage(getString(R.string.bluetooth_turning_off));
                } break;
            }
            builder.setCancelable(false);
        } else {
            // Ask the user whether to turn on discovery mode or not
            // For lasting discoverable mode there is a different message
            if (mTimeout == BluetoothDiscoverableEnabler.DISCOVERABLE_TIMEOUT_NEVER) {
                CharSequence message = mAppLabel != null
                        ? getString(R.string.bluetooth_ask_lasting_discovery, mAppLabel)
                        : getString(R.string.bluetooth_ask_lasting_discovery_no_name);
                builder.setMessage(message);
            } else {
                CharSequence message = mAppLabel != null
                        ? getString(R.string.bluetooth_ask_discovery, mAppLabel, mTimeout)
                        : getString(R.string.bluetooth_ask_discovery_no_name, mTimeout);
                builder.setMessage(message);
            }
            builder.setPositiveButton(getString(R.string.allow), this);
            builder.setNegativeButton(getString(R.string.deny), this);
        }

        builder.setOnDismissListener(this);
        mDialog = builder.create();
        mDialog.show();
    }

    private void onEnableConfirmed() {
        mBluetoothAdapter.enable();
        if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            proceedAndFinish();
        } else {
            // If BT is not up yet, show "Turning on Bluetooth..."
            mReceiver = new StateChangeReceiver();
            registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            createDialog();
        }
    }

    private void onDisableConfirmed() {
        mBluetoothAdapter.disable();
        if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
            proceedAndFinish();
        } else {
            // If BT is not up yet, show "Turning off Bluetooth..."
            mReceiver = new StateChangeReceiver();
            registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            createDialog();
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                proceedAndFinish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                cancelAndFinish();
                break;
        }
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        cancelAndFinish();
    }

    private void proceedAndFinish() {
        int returnCode;

        if (mRequest == REQUEST_ENABLE || mRequest == REQUEST_DISABLE) {
            // BT toggled. Done
            returnCode = RESULT_OK;
        } else {
            mBluetoothAdapter.setDiscoverableTimeout(Duration.ofSeconds(mTimeout));
            if (mBluetoothAdapter.setScanMode(
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
                    == BluetoothStatusCodes.SUCCESS) {
                // If already in discoverable mode, this will extend the timeout.
                long endTime = System.currentTimeMillis() + (long) mTimeout * 1000;
                LocalBluetoothPreferences.persistDiscoverableEndTimestamp(
                        this, endTime);
                if (0 < mTimeout) {
                    BluetoothDiscoverableTimeoutReceiver.setDiscoverableAlarm(this, endTime);
                }
                returnCode = mTimeout;
                // Activity.RESULT_FIRST_USER should be 1
                if (returnCode < RESULT_FIRST_USER) {
                    returnCode = RESULT_FIRST_USER;
                }
            } else {
                returnCode = RESULT_CANCELED;
            }
        }

        setResult(returnCode);
        finish();
    }

    private void cancelAndFinish() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    /**
     * Parse the received Intent and initialize mBluetoothAdapter.
     * @return true if an error occurred; false otherwise
     */
    private boolean parseIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return true;
        }
        if (intent.getAction().equals(BluetoothAdapter.ACTION_REQUEST_ENABLE)) {
            mRequest = REQUEST_ENABLE;
        } else if (intent.getAction().equals(BluetoothAdapter.ACTION_REQUEST_DISABLE)) {
            mRequest = REQUEST_DISABLE;
        } else if (intent.getAction().equals(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)) {
            mRequest = REQUEST_ENABLE_DISCOVERABLE;
            mTimeout = intent.getIntExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                    BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT);

            Log.d(TAG, "Setting Bluetooth Discoverable Timeout = " + mTimeout);

            if (mTimeout < 1 || mTimeout > MAX_DISCOVERABLE_TIMEOUT) {
                mTimeout = BluetoothDiscoverableEnabler.DEFAULT_DISCOVERABLE_TIMEOUT;
            }
        } else {
            Log.e(TAG, "Error: this activity may be started only with intent "
                    + BluetoothAdapter.ACTION_REQUEST_ENABLE + ", "
                    + BluetoothAdapter.ACTION_REQUEST_DISABLE + " or "
                    + BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            setResult(RESULT_CANCELED);
            return true;
        }

        String packageName = getLaunchedFromPackage();
        int mCallingUid = getLaunchedFromUid();

        if (UserHandle.isSameApp(mCallingUid, Process.SYSTEM_UID)
                && getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME) != null) {
            packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        }

        if (!UserHandle.isSameApp(mCallingUid, Process.SYSTEM_UID)
                && getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME) != null) {
            Log.w(TAG, "Non-system Uid: " + mCallingUid + " tried to override packageName \n");
        }

        if (!TextUtils.isEmpty(packageName)) {
            try {
                ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                        packageName, 0);
                mAppLabel = applicationInfo.loadSafeLabel(getPackageManager(),
                        PackageItemInfo.DEFAULT_MAX_LABEL_SIZE_PX,
                        PackageItemInfo.SAFE_LABEL_FLAG_TRIM
                                | PackageItemInfo.SAFE_LABEL_FLAG_FIRST_LINE);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Couldn't find app with package name " + packageName);
                setResult(RESULT_CANCELED);
                return true;
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Error: there's a problem starting Bluetooth");
            setResult(RESULT_CANCELED);
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mRequestDialog != null && mRequestDialog.isShowing()) {
            mRequestDialog.dismiss();
            mRequestDialog = null;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private final class StateChangeReceiver extends BroadcastReceiver {
        private static final long TOGGLE_TIMEOUT_MILLIS = 10000; // 10 sec

        public StateChangeReceiver() {
            getWindow().getDecorView().postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    cancelAndFinish();
                }
            }, TOGGLE_TIMEOUT_MILLIS);
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            final int currentState = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR);
            switch (mRequest) {
                case REQUEST_ENABLE:
                case REQUEST_ENABLE_DISCOVERABLE: {
                    if (currentState == BluetoothAdapter.STATE_ON) {
                        proceedAndFinish();
                    }
                } break;

                case REQUEST_DISABLE: {
                    if (currentState == BluetoothAdapter.STATE_OFF) {
                        proceedAndFinish();
                    }
                } break;
            }
        }
    }
}
