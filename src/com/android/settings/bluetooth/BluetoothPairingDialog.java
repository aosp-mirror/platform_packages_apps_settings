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
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import android.view.KeyEvent;

import java.util.Locale;

/**
 * BluetoothPairingDialog asks the user to enter a PIN / Passkey / simple confirmation
 * for pairing with a remote Bluetooth device. It is an activity that appears as a dialog.
 */
public final class BluetoothPairingDialog extends AlertActivity implements
        CompoundButton.OnCheckedChangeListener, DialogInterface.OnClickListener, TextWatcher {
    private static final String TAG = "BluetoothPairingDialog";

    private static final int BLUETOOTH_PIN_MAX_LENGTH = 16;
    private static final int BLUETOOTH_PASSKEY_MAX_LENGTH = 6;
    private BluetoothDevice mDevice;
    private int mType;
    private String mPairingKey;
    private EditText mPairingView;
    private Button mOkButton;

    /**
     * Dismiss the dialog if the bond state changes to bonded or none,
     * or if pairing was canceled for {@link #mDevice}.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                                   BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED ||
                        bondState == BluetoothDevice.BOND_NONE) {
                    dismiss();
                }
            } else if (BluetoothDevice.ACTION_PAIRING_CANCEL.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null || device.equals(mDevice)) {
                    dismiss();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (!intent.getAction().equals(BluetoothDevice.ACTION_PAIRING_REQUEST))
        {
            Log.e(TAG, "Error: this activity may be started only with intent " +
                  BluetoothDevice.ACTION_PAIRING_REQUEST);
            finish();
            return;
        }

        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(this);
        if (manager == null) {
            Log.e(TAG, "Error: BluetoothAdapter not supported by system");
            finish();
            return;
        }
        CachedBluetoothDeviceManager deviceManager = manager.getCachedDeviceManager();

        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);

        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                createUserEntryDialog(deviceManager);
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                int passkey =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                if (passkey == BluetoothDevice.ERROR) {
                    Log.e(TAG, "Invalid Confirmation Passkey received, not showing any dialog");
                    return;
                }
                mPairingKey = String.format(Locale.US, "%06d", passkey);
                createConfirmationDialog(deviceManager);
                break;

            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                createConsentDialog(deviceManager);
                break;

            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                int pairingKey =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                if (pairingKey == BluetoothDevice.ERROR) {
                    Log.e(TAG, "Invalid Confirmation Passkey or PIN received, not showing any dialog");
                    return;
                }
                if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
                    mPairingKey = String.format("%06d", pairingKey);
                } else {
                    mPairingKey = String.format("%04d", pairingKey);
                }
                createDisplayPasskeyOrPinDialog(deviceManager);
                break;

            default:
                Log.e(TAG, "Incorrect pairing type received, not showing any dialog");
        }

        /*
         * Leave this registered through pause/resume since we still want to
         * finish the activity in the background if pairing is canceled.
         */
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_CANCEL));
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    private void createUserEntryDialog(CachedBluetoothDeviceManager deviceManager) {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
        p.mView = createPinEntryView(deviceManager.getName(mDevice));
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();

        mOkButton = mAlert.getButton(BUTTON_POSITIVE);
        mOkButton.setEnabled(false);
    }

    private View createPinEntryView(String deviceName) {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, null);
        TextView messageView = (TextView) view.findViewById(R.id.message);
        TextView messageView2 = (TextView) view.findViewById(R.id.message_below_pin);
        CheckBox alphanumericPin = (CheckBox) view.findViewById(R.id.alphanumeric_pin);
        mPairingView = (EditText) view.findViewById(R.id.text);
        mPairingView.addTextChangedListener(this);
        alphanumericPin.setOnCheckedChangeListener(this);

        int messageId1;
        int messageId2;
        int maxLength;
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                messageId1 = R.string.bluetooth_enter_pin_msg;
                messageId2 = R.string.bluetooth_enter_pin_other_device;
                // Maximum of 16 characters in a PIN
                maxLength = BLUETOOTH_PIN_MAX_LENGTH;
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                messageId1 = R.string.bluetooth_enter_passkey_msg;
                messageId2 = R.string.bluetooth_enter_passkey_other_device;
                // Maximum of 6 digits for passkey
                maxLength = BLUETOOTH_PASSKEY_MAX_LENGTH;
                alphanumericPin.setVisibility(View.GONE);
                break;

            default:
                Log.e(TAG, "Incorrect pairing type for createPinEntryView: " + mType);
                return null;
        }

        // Format the message string, then parse HTML style tags
        String messageText = getString(messageId1, deviceName);
        messageView.setText(Html.fromHtml(messageText));
        messageView2.setText(messageId2);
        mPairingView.setInputType(InputType.TYPE_CLASS_NUMBER);
        mPairingView.setFilters(new InputFilter[] {
                new LengthFilter(maxLength) });

        return view;
    }

    private View createView(CachedBluetoothDeviceManager deviceManager) {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_confirm, null);
        String name = deviceManager.getName(mDevice);
        TextView messageView = (TextView) view.findViewById(R.id.message);

        String messageText; // formatted string containing HTML style tags
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                messageText = getString(R.string.bluetooth_confirm_passkey_msg,
                        name, mPairingKey);
                break;

            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                messageText = getString(R.string.bluetooth_incoming_pairing_msg, name);
                break;

            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                messageText = getString(R.string.bluetooth_display_passkey_pin_msg, name,
                        mPairingKey);
                break;

            default:
                Log.e(TAG, "Incorrect pairing type received, not creating view");
                return null;
        }
        messageView.setText(Html.fromHtml(messageText));
        return view;
    }

    private void createConfirmationDialog(CachedBluetoothDeviceManager deviceManager) {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
        p.mView = createView(deviceManager);
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createConsentDialog(CachedBluetoothDeviceManager deviceManager) {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
        p.mView = createView(deviceManager);
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createDisplayPasskeyOrPinDialog(
            CachedBluetoothDeviceManager deviceManager) {
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.bluetooth_pairing_request);
        p.mView = createView(deviceManager);
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();

        // Since its only a notification, send an OK to the framework,
        // indicating that the dialog has been displayed.
        if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
            mDevice.setPairingConfirmation(true);
        } else if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
            byte[] pinBytes = BluetoothDevice.convertPinToBytes(mPairingKey);
            mDevice.setPin(pinBytes);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void afterTextChanged(Editable s) {
        if (mOkButton != null) {
            mOkButton.setEnabled(s.length() > 0);
        }
    }

    private void onPair(String value) {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                byte[] pinBytes = BluetoothDevice.convertPinToBytes(value);
                if (pinBytes == null) {
                    return;
                }
                mDevice.setPin(pinBytes);
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                int passkey = Integer.parseInt(value);
                mDevice.setPasskey(passkey);
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
                mDevice.setPairingConfirmation(true);
                break;

            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                // Do nothing.
                break;

            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                mDevice.setRemoteOutOfBandData();
                break;

            default:
                Log.e(TAG, "Incorrect pairing type received");
        }
    }

    private void onCancel() {
        mDevice.cancelPairingUserInput();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onCancel();
        }
        return super.onKeyDown(keyCode,event);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                if (mPairingView != null) {
                    onPair(mPairingView.getText().toString());
                } else {
                    onPair(null);
                }
                break;

            case BUTTON_NEGATIVE:
            default:
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

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // change input type for soft keyboard to numeric or alphanumeric
        if (isChecked) {
            mPairingView.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            mPairingView.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }
}
