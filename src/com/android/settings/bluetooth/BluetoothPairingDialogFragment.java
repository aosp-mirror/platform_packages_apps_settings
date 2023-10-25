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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * A dialogFragment used by {@link BluetoothPairingDialog} to create an appropriately styled dialog
 * for the bluetooth device.
 */
public class BluetoothPairingDialogFragment extends InstrumentedDialogFragment implements
        TextWatcher, OnClickListener {

    private static final String TAG = "BTPairingDialogFragment";

    private AlertDialog.Builder mBuilder;
    private AlertDialog mDialog;
    private BluetoothPairingController mPairingController;
    private BluetoothPairingDialog mPairingDialogActivity;
    private EditText mPairingView;
    private boolean mPositiveClicked = false;
    /**
     * The interface we expect a listener to implement. Typically this should be done by
     * the controller.
     */
    public interface BluetoothPairingDialogListener {

        void onDialogNegativeClick(BluetoothPairingDialogFragment dialog);

        void onDialogPositiveClick(BluetoothPairingDialogFragment dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!isPairingControllerSet()) {
            throw new IllegalStateException(
                "Must call setPairingController() before showing dialog");
        }
        if (!isPairingDialogActivitySet()) {
            throw new IllegalStateException(
                "Must call setPairingDialogActivity() before showing dialog");
        }
        mBuilder = new AlertDialog.Builder(getActivity());
        mDialog = setupDialog();
        mDialog.setCanceledOnTouchOutside(false);
        return mDialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPairingController.getDialogType()
                != BluetoothPairingController.DISPLAY_PASSKEY_DIALOG) {
            /* Cancel pairing unless explicitly accepted by user */
            if (!mPositiveClicked) {
                mPairingController.onCancel();
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // enable the positive button when we detect potentially valid input
        Button positiveButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(mPairingController.isPasskeyValid(s));
        }
        // notify the controller about user input
        mPairingController.updateUserInput(s.toString());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mPositiveClicked = true;
            mPairingController.onDialogPositiveClick(this);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            mPairingController.onDialogNegativeClick(this);
        }
        mPairingDialogActivity.dismiss();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_DIALOG_FRAGMENT;
    }

    /**
     * Used in testing to get a reference to the dialog.
     * @return - The fragments current dialog
     */
    protected AlertDialog getmDialog() {
        return mDialog;
    }

    /**
     * Sets the controller that the fragment should use. this method MUST be called
     * before you try to show the dialog or an error will be thrown. An implementation
     * of a pairing controller can be found at {@link BluetoothPairingController}. A
     * controller may not be substituted once it is assigned. Forcibly switching a
     * controller for a new one will lead to undefined behavior.
     */
    void setPairingController(BluetoothPairingController pairingController) {
        if (isPairingControllerSet()) {
            throw new IllegalStateException("The controller can only be set once. "
                    + "Forcibly replacing it will lead to undefined behavior");
        }
        mPairingController = pairingController;
    }

    /**
     * Checks whether mPairingController is set
     * @return True when mPairingController is set, False otherwise
     */
    boolean isPairingControllerSet() {
        return mPairingController != null;
    }

    /**
     * Sets the BluetoothPairingDialog activity that started this fragment
     * @param pairingDialogActivity The pairing dialog activty that started this fragment
     */
    void setPairingDialogActivity(BluetoothPairingDialog pairingDialogActivity) {
        if (isPairingDialogActivitySet()) {
            throw new IllegalStateException("The pairing dialog activity can only be set once");
        }
        mPairingDialogActivity = pairingDialogActivity;
    }

    /**
     * Checks whether mPairingDialogActivity is set
     * @return True when mPairingDialogActivity is set, False otherwise
     */
    boolean isPairingDialogActivitySet() {
        return mPairingDialogActivity != null;
    }

    /**
     * Creates the appropriate type of dialog and returns it.
     */
    private AlertDialog setupDialog() {
        AlertDialog dialog;
        switch (mPairingController.getDialogType()) {
            case BluetoothPairingController.USER_ENTRY_DIALOG:
                dialog = createUserEntryDialog();
                break;
            case BluetoothPairingController.CONFIRMATION_DIALOG:
                dialog = createConsentDialog();
                break;
            case BluetoothPairingController.DISPLAY_PASSKEY_DIALOG:
                dialog = createDisplayPasskeyOrPinDialog();
                break;
            default:
                dialog = null;
                Log.e(TAG, "Incorrect pairing type received, not showing any dialog");
        }
        return dialog;
    }

    /**
     * Helper method to return the text of the pin entry field - this exists primarily to help us
     * simulate having existing text when the dialog is recreated, for example after a screen
     * rotation.
     */
    @VisibleForTesting
    CharSequence getPairingViewText() {
        if (mPairingView != null) {
            return mPairingView.getText();
        }
        return null;
    }

    /**
     * Returns a dialog with UI elements that allow a user to provide input.
     */
    private AlertDialog createUserEntryDialog() {
        mBuilder.setTitle(getString(R.string.bluetooth_pairing_request,
                mPairingController.getDeviceName()));
        mBuilder.setView(createPinEntryView());
        mBuilder.setPositiveButton(getString(android.R.string.ok), this);
        mBuilder.setNegativeButton(getString(android.R.string.cancel), this);
        AlertDialog dialog = mBuilder.create();
        dialog.setOnShowListener(d -> {
            if (TextUtils.isEmpty(getPairingViewText())) {
                mDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
            }
            if (mPairingView != null && mPairingView.requestFocus()) {
                InputMethodManager imm = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(mPairingView, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        return dialog;
    }

    /**
     * Creates the custom view with UI elements for user input.
     */
    private View createPinEntryView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, null);
        TextView messageViewCaptionHint = (TextView) view.findViewById(R.id.pin_values_hint);
        TextView messageView2 = (TextView) view.findViewById(R.id.message_below_pin);
        CheckBox alphanumericPin = (CheckBox) view.findViewById(R.id.alphanumeric_pin);
        CheckBox contactSharing = (CheckBox) view.findViewById(
                R.id.phonebook_sharing_message_entry_pin);
        contactSharing.setText(getString(R.string.bluetooth_pairing_shares_phonebook));
        EditText pairingView = (EditText) view.findViewById(R.id.text);

        contactSharing.setVisibility(
                mPairingController.isContactSharingVisible() ? View.VISIBLE : View.GONE);
        mPairingController.setContactSharingState();
        contactSharing.setOnCheckedChangeListener(mPairingController);
        contactSharing.setChecked(mPairingController.getContactSharingState());

        mPairingView = pairingView;

        pairingView.setInputType(InputType.TYPE_CLASS_NUMBER);
        pairingView.addTextChangedListener(this);
        alphanumericPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // change input type for soft keyboard to numeric or alphanumeric
            if (isChecked) {
                mPairingView.setInputType(InputType.TYPE_CLASS_TEXT);
            } else {
                mPairingView.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        });

        int messageId = mPairingController.getDeviceVariantMessageId();
        int messageIdHint = mPairingController.getDeviceVariantMessageHintId();
        int maxLength = mPairingController.getDeviceMaxPasskeyLength();
        alphanumericPin.setVisibility(mPairingController.pairingCodeIsAlphanumeric()
                ? View.VISIBLE : View.GONE);
        if (messageId != BluetoothPairingController.INVALID_DIALOG_TYPE) {
            messageView2.setText(messageId);
        } else {
            messageView2.setVisibility(View.GONE);
        }
        if (messageIdHint != BluetoothPairingController.INVALID_DIALOG_TYPE) {
            messageViewCaptionHint.setText(messageIdHint);
        } else {
            messageViewCaptionHint.setVisibility(View.GONE);
        }
        pairingView.setFilters(new InputFilter[]{
                new LengthFilter(maxLength)});

        return view;
    }

    /**
     * Creates a dialog with UI elements that allow the user to confirm a pairing request.
     */
    private AlertDialog createConfirmationDialog() {
        mBuilder.setTitle(getString(R.string.bluetooth_pairing_request,
                mPairingController.getDeviceName()));
        mBuilder.setView(createView());
        mBuilder.setPositiveButton(
                getString(com.android.settingslib.R.string.bluetooth_pairing_accept), this);
        mBuilder.setNegativeButton(
                getString(com.android.settingslib.R.string.bluetooth_pairing_decline), this);
        AlertDialog dialog = mBuilder.create();
        return dialog;
    }

    /**
     * Creates a dialog with UI elements that allow the user to consent to a pairing request.
     */
    private AlertDialog createConsentDialog() {
        return createConfirmationDialog();
    }

    /**
     * Creates a dialog that informs users of a pairing request and shows them the passkey/pin
     * of the device.
     */
    private AlertDialog createDisplayPasskeyOrPinDialog() {
        mBuilder.setTitle(getString(R.string.bluetooth_pairing_request,
                mPairingController.getDeviceName()));
        mBuilder.setView(createView());
        mBuilder.setNegativeButton(getString(android.R.string.cancel), this);
        AlertDialog dialog = mBuilder.create();

        // Tell the controller the dialog has been created.
        mPairingController.notifyDialogDisplayed();

        return dialog;
    }

    /**
     * Creates a custom view for dialogs which need to show users additional information but do
     * not require user input.
     */
    private View createView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.bluetooth_pin_confirm, null);
        TextView pairingViewCaption = (TextView) view.findViewById(R.id.pairing_caption);
        TextView pairingViewContent = (TextView) view.findViewById(R.id.pairing_subhead);
        TextView messagePairing = (TextView) view.findViewById(R.id.pairing_code_message);
        CompoundButton contactSharing =
                view.findViewById(R.id.phonebook_sharing_message_confirm_pin);
        view.findViewById(R.id.phonebook_sharing).setVisibility(
                mPairingController.isContactSharingVisible() ? View.VISIBLE : View.GONE);
        mPairingController.setContactSharingState();
        contactSharing.setChecked(mPairingController.getContactSharingState());
        contactSharing.setOnCheckedChangeListener(mPairingController);

        messagePairing.setVisibility(mPairingController.isDisplayPairingKeyVariant()
                ? View.VISIBLE : View.GONE);
        if (mPairingController.hasPairingContent()) {
            pairingViewCaption.setVisibility(View.VISIBLE);
            pairingViewContent.setVisibility(View.VISIBLE);
            pairingViewContent.setText(mPairingController.getPairingContent());
        }
        final TextView messagePairingSet = (TextView) view.findViewById(R.id.pairing_group_message);
        if (mPairingController.isLateBonding()) {
            messagePairingSet.setText(getString(R.string.bluetooth_pairing_group_late_bonding));
        }

        boolean setPairingMessage =
            mPairingController.isCoordinatedSetMemberDevice() || mPairingController.isLateBonding();

        messagePairingSet.setVisibility(setPairingMessage ? View.VISIBLE : View.GONE);
        return view;
    }
}
