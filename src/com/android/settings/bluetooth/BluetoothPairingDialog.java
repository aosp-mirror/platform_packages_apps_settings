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

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

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

    private LocalBluetoothManager mBluetoothManager;
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    private BluetoothDevice mDevice;
    private int mType;
    private String mPairingKey;
    private EditText mPairingView;
    private Button mOkButton;
    private LocalBluetoothProfile mPbapClientProfile;


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

        mBluetoothManager = Utils.getLocalBtManager(this);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Error: BluetoothAdapter not supported by system");
            finish();
            return;
        }
        mCachedDeviceManager = mBluetoothManager.getCachedDeviceManager();
        mPbapClientProfile = mBluetoothManager.getProfileManager().getPbapClientProfile();

        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);

        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                createUserEntryDialog();
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                int passkey =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                if (passkey == BluetoothDevice.ERROR) {
                    Log.e(TAG, "Invalid Confirmation Passkey received, not showing any dialog");
                    return;
                }
                mPairingKey = String.format(Locale.US, "%06d", passkey);
                createConfirmationDialog();
                break;

            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                createConsentDialog();
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
                createDisplayPasskeyOrPinDialog();
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

    private void createUserEntryDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request,
                mCachedDeviceManager.getName(mDevice));
        p.mView = createPinEntryView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();

        mOkButton = mAlert.getButton(BUTTON_POSITIVE);
        mOkButton.setEnabled(false);
    }

    private View createPinEntryView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, null);
        TextView messageViewCaptionHint = (TextView) view.findViewById(R.id.pin_values_hint);
        TextView messageView2 = (TextView) view.findViewById(R.id.message_below_pin);
        CheckBox alphanumericPin = (CheckBox) view.findViewById(R.id.alphanumeric_pin);
        CheckBox contactSharing = (CheckBox) view.findViewById(
                R.id.phonebook_sharing_message_entry_pin);
        contactSharing.setText(getString(R.string.bluetooth_pairing_shares_phonebook,
                mCachedDeviceManager.getName(mDevice)));
        if (mPbapClientProfile != null && mPbapClientProfile.isProfileReady()) {
            contactSharing.setVisibility(View.GONE);
        }
        if (mDevice.getPhonebookAccessPermission() == BluetoothDevice.ACCESS_ALLOWED) {
            contactSharing.setChecked(true);
        } else if (mDevice.getPhonebookAccessPermission() == BluetoothDevice.ACCESS_REJECTED){
            contactSharing.setChecked(false);
        } else {
            if (mDevice.getBluetoothClass().getDeviceClass()
                    == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                contactSharing.setChecked(true);
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            } else {
                contactSharing.setChecked(false);
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
            }
        }

        contactSharing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
                } else {
                    mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
                }
            }
        });

        mPairingView = (EditText) view.findViewById(R.id.text);
        mPairingView.addTextChangedListener(this);
        alphanumericPin.setOnCheckedChangeListener(this);

        int messageId;
        int messageIdHint = R.string.bluetooth_pin_values_hint;
        int maxLength;
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
                messageIdHint = R.string.bluetooth_pin_values_hint_16_digits;
                // FALLTHROUGH
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                messageId = R.string.bluetooth_enter_pin_other_device;
                // Maximum of 16 characters in a PIN
                maxLength = BLUETOOTH_PIN_MAX_LENGTH;
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                messageId = R.string.bluetooth_enter_passkey_other_device;
                // Maximum of 6 digits for passkey
                maxLength = BLUETOOTH_PASSKEY_MAX_LENGTH;
                alphanumericPin.setVisibility(View.GONE);
                break;

            default:
                Log.e(TAG, "Incorrect pairing type for createPinEntryView: " + mType);
                return null;
        }

        messageViewCaptionHint.setText(messageIdHint);
        messageView2.setText(messageId);
        mPairingView.setInputType(InputType.TYPE_CLASS_NUMBER);
        mPairingView.setFilters(new InputFilter[] {
                new LengthFilter(maxLength) });

        return view;
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_confirm, null);
        TextView pairingViewCaption = (TextView) view.findViewById(R.id.pairing_caption);
        TextView pairingViewContent = (TextView) view.findViewById(R.id.pairing_subhead);
        TextView messagePairing = (TextView) view.findViewById(R.id.pairing_code_message);
        CheckBox contactSharing = (CheckBox) view.findViewById(
                R.id.phonebook_sharing_message_confirm_pin);
        contactSharing.setText(getString(R.string.bluetooth_pairing_shares_phonebook,
                mCachedDeviceManager.getName(mDevice)));
        if (mPbapClientProfile != null && mPbapClientProfile.isProfileReady()) {
            contactSharing.setVisibility(View.GONE);
        }
        if (mDevice.getPhonebookAccessPermission() == BluetoothDevice.ACCESS_ALLOWED) {
            contactSharing.setChecked(true);
        } else if (mDevice.getPhonebookAccessPermission() == BluetoothDevice.ACCESS_REJECTED){
            contactSharing.setChecked(false);
        } else {
            if (mDevice.getBluetoothClass().getDeviceClass()
                    == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                contactSharing.setChecked(true);
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            } else {
                contactSharing.setChecked(false);
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
            }
        }

        contactSharing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
                } else {
                    mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
                }
            }
        });

        String messageCaption = null;
        String pairingContent = null;
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                messagePairing.setVisibility(View.VISIBLE);
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                pairingContent = mPairingKey;
                break;

            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                messagePairing.setVisibility(View.VISIBLE);
                break;

            default:
                Log.e(TAG, "Incorrect pairing type received, not creating view");
                return null;
        }

        if (pairingContent != null) {
            pairingViewCaption.setVisibility(View.VISIBLE);
            pairingViewContent.setVisibility(View.VISIBLE);
            pairingViewContent.setText(pairingContent);
        }

        return view;
    }

    private void createConfirmationDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request,
                mCachedDeviceManager.getName(mDevice));
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createConsentDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request,
                mCachedDeviceManager.getName(mDevice));
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createDisplayPasskeyOrPinDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request,
                mCachedDeviceManager.getName(mDevice));
        p.mView = createView();
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
            if (mType == BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS) {
                mOkButton.setEnabled(s.length() >= 16);
            } else {
                mOkButton.setEnabled(s.length() > 0);
            }
        }
    }

    private void onPair(String value) {
        Log.i(TAG, "Pairing dialog accepted");
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
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
        Log.i(TAG, "Pairing dialog canceled");
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
