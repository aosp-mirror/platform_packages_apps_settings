/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothPairingDialogFragment.BluetoothPairingDialogListener;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import java.util.Locale;

/**
 * A controller used by {@link BluetoothPairingDialog} to manage connection state while we try to
 * pair with a bluetooth device. It includes methods that allow the
 * {@link BluetoothPairingDialogFragment} to interrogate the current state as well.
 */
public class BluetoothPairingController implements OnCheckedChangeListener,
        BluetoothPairingDialogListener {

    private static final String TAG = "BTPairingController";

    // Different types of dialogs we can map to
    public static final int INVALID_DIALOG_TYPE = -1;
    public static final int USER_ENTRY_DIALOG = 0;
    public static final int CONFIRMATION_DIALOG = 1;
    public static final int DISPLAY_PASSKEY_DIALOG = 2;

    private static final int BLUETOOTH_PIN_MAX_LENGTH = 16;
    private static final int BLUETOOTH_PASSKEY_MAX_LENGTH = 6;

    // Bluetooth dependencies for the connection we are trying to establish
    private LocalBluetoothManager mBluetoothManager;
    private BluetoothDevice mDevice;
    private int mType;
    private String mUserInput;
    private String mPasskeyFormatted;
    private int mPasskey;
    private String mDeviceName;
    private LocalBluetoothProfile mPbapClientProfile;
    private boolean mPbapAllowed;

    /**
     * Creates an instance of a BluetoothPairingController.
     *
     * @param intent - must contain {@link BluetoothDevice#EXTRA_PAIRING_VARIANT}, {@link
     * BluetoothDevice#EXTRA_PAIRING_KEY}, and {@link BluetoothDevice#EXTRA_DEVICE}. Missing extra
     * will lead to undefined behavior.
     */
    public BluetoothPairingController(Intent intent, Context context) {
        mBluetoothManager = Utils.getLocalBtManager(context);
        mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        String message = "";
        if (mBluetoothManager == null) {
            throw new IllegalStateException("Could not obtain LocalBluetoothManager");
        } else if (mDevice == null) {
            throw new IllegalStateException("Could not find BluetoothDevice");
        }

        mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
        mPasskey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
        mDeviceName = mBluetoothManager.getCachedDeviceManager().getName(mDevice);
        mPbapClientProfile = mBluetoothManager.getProfileManager().getPbapClientProfile();
        mPasskeyFormatted = formatKey(mPasskey);

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mPbapAllowed = true;
        } else {
            mPbapAllowed = false;
        }
    }

    @Override
    public void onDialogPositiveClick(BluetoothPairingDialogFragment dialog) {
        if (getDialogType() == USER_ENTRY_DIALOG) {
            if (mPbapAllowed) {
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
            } else {
                mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
            }
            onPair(mUserInput);
        } else {
            onPair(null);
        }
    }

    @Override
    public void onDialogNegativeClick(BluetoothPairingDialogFragment dialog) {
        mDevice.setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
        onCancel();
    }

    /**
     * A method for querying which bluetooth pairing dialog fragment variant this device requires.
     *
     * @return - The dialog view variant needed for this device.
     */
    public int getDialogType() {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                return USER_ENTRY_DIALOG;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                return CONFIRMATION_DIALOG;

            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                return DISPLAY_PASSKEY_DIALOG;

            default:
                return INVALID_DIALOG_TYPE;
        }
    }

    /**
     * @return - A string containing the name provided by the device.
     */
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * A method for querying if the bluetooth device has a profile already set up on this device.
     *
     * @return - A boolean indicating if the device has previous knowledge of a profile for this
     * device.
     */
    public boolean isProfileReady() {
        return mPbapClientProfile != null && mPbapClientProfile.isProfileReady();
    }

    /**
     * A method for querying if the bluetooth device has access to contacts on the device.
     *
     * @return - A boolean indicating if the bluetooth device has permission to access the device
     * contacts
     */
    public boolean getContactSharingState() {
        switch (mDevice.getPhonebookAccessPermission()) {
            case BluetoothDevice.ACCESS_ALLOWED:
                return true;
            case BluetoothDevice.ACCESS_REJECTED:
                return false;
            default:
                if (mDevice.getBluetoothClass().getDeviceClass()
                        == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                    return true;
                }
                return false;
        }
    }

    /**
     * A method for querying if the provided editable is a valid passkey/pin format for this device.
     *
     * @param s - The passkey/pin
     * @return - A boolean indicating if the passkey/pin is of the correct format.
     */
    public boolean isPasskeyValid(Editable s) {
        boolean requires16Digits = mType == BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS;
        return s.length() >= 16 && requires16Digits || s.length() > 0 && !requires16Digits;
    }

    /**
     * A method for querying what message should be shown to the user as additional text in the
     * dialog for this device. Returns -1 to indicate a device type that does not use this message.
     *
     * @return - The message ID to show the user.
     */
    public int getDeviceVariantMessageId() {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                return R.string.bluetooth_enter_pin_other_device;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                return R.string.bluetooth_enter_passkey_other_device;

            default:
                return INVALID_DIALOG_TYPE;
        }
    }

    /**
     * A method for querying what message hint should be shown to the user as additional text in the
     * dialog for this device. Returns -1 to indicate a device type that does not use this message.
     *
     * @return - The message ID to show the user.
     */
    public int getDeviceVariantMessageHintId() {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
                return R.string.bluetooth_pin_values_hint_16_digits;

            case BluetoothDevice.PAIRING_VARIANT_PIN:
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                return R.string.bluetooth_pin_values_hint;

            default:
                return INVALID_DIALOG_TYPE;
        }
    }

    /**
     * A method for querying the maximum passkey/pin length for this device.
     *
     * @return - An int indicating the maximum length
     */
    public int getDeviceMaxPasskeyLength() {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                return BLUETOOTH_PIN_MAX_LENGTH;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                return BLUETOOTH_PASSKEY_MAX_LENGTH;

            default:
                return 0;
        }

    }

    /**
     * A method for querying if the device uses an alphanumeric passkey.
     *
     * @return - a boolean indicating if the passkey can be alphanumeric.
     */
    public boolean pairingCodeIsAlphanumeric() {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                return false;

            default:
                return true;
        }
    }

    /**
     * A method used by the dialogfragment to notify the controller that the dialog has been
     * displayed for bluetooth device types that just care about it being displayed.
     */
    protected void notifyDialogDisplayed() {
        // send an OK to the framework, indicating that the dialog has been displayed.
        if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
            mDevice.setPairingConfirmation(true);
        } else if (mType == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
            byte[] pinBytes = BluetoothDevice.convertPinToBytes(mPasskeyFormatted);
            mDevice.setPin(pinBytes);
        }
    }

    /**
     * A method for querying if this bluetooth device type has a key it would like displayed
     * to the user.
     *
     * @return - A boolean indicating if a key exists which should be displayed to the user.
     */
    public boolean isDisplayPairingKeyVariant() {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
            case BluetoothDevice.PAIRING_VARIANT_OOB_CONSENT:
                return true;
            default:
                return false;
        }
    }

    /**
     * A method for querying if this bluetooth device type has other content it would like displayed
     * to the user.
     *
     * @return - A boolean indicating if content exists which should be displayed to the user.
     */
    public boolean hasPairingContent() {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                return true;

            default:
                return false;
        }
    }

    /**
     * A method for obtaining any additional content this bluetooth device has for displaying to the
     * user.
     *
     * @return - A string containing the additional content, null if none exists.
     * @see {@link BluetoothPairingController#hasPairingContent()}
     */
    public String getPairingContent() {
        if (hasPairingContent()) {
            return mPasskeyFormatted;
        } else {
            return null;
        }
    }

    /**
     * A method that exists to allow the fragment to update the controller with input the user has
     * provided in the fragment.
     *
     * @param input - A string containing the user input.
     */
    protected void updateUserInput(String input) {
        mUserInput = input;
    }

    /**
     * Returns the provided passkey in a format that this device expects. Only works for numeric
     * passkeys/pins.
     *
     * @param passkey - An integer containing the passkey to format.
     * @return - A string containing the formatted passkey/pin
     */
    private String formatKey(int passkey) {
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
                return String.format(Locale.US, "%06d", passkey);

            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                return String.format("%04d", passkey);

            default:
                return null;
        }
    }

    /**
     * handles the necessary communication with the bluetooth device to establish a successful
     * pairing
     *
     * @param passkey - The passkey we will attempt to pair to the device with.
     */
    private void onPair(String passkey) {
        Log.d(TAG, "Pairing dialog accepted");
        switch (mType) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
            case BluetoothDevice.PAIRING_VARIANT_PIN_16_DIGITS:
                byte[] pinBytes = BluetoothDevice.convertPinToBytes(passkey);
                if (pinBytes == null) {
                    return;
                }
                mDevice.setPin(pinBytes);
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY:
                int pass = Integer.parseInt(passkey);
                mDevice.setPasskey(pass);
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

    /**
     * A method for properly ending communication with the bluetooth device. Will be called by the
     * {@link BluetoothPairingDialogFragment} when it is dismissed.
     */
    public void onCancel() {
        Log.d(TAG, "Pairing dialog canceled");
        mDevice.cancelPairingUserInput();
    }

    /**
     * A method for checking if this device is equal to another device.
     *
     * @param device - The other device being compared to this device.
     * @return - A boolean indicating if the devices were equal.
     */
    public boolean deviceEquals(BluetoothDevice device) {
        return mDevice == device;
    }
}
