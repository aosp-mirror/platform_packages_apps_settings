/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.android.internal.util.HexDump;
import com.android.settings.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CarrierDemoPasswordDialogFragment extends DialogFragment {

    private static final String TAG = "CarrierDemoPasswordDialogFragment";

    private MessageDigest mMessageDigest;

    public CarrierDemoPasswordDialogFragment() {
        try {
            mMessageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Unable to verify demo mode password", e);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setPositiveButton(R.string.retail_demo_reset_next,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Fragment parentFragment = getParentFragment();
                        if (parentFragment instanceof Callback
                                && which == DialogInterface.BUTTON_POSITIVE) {
                            ((Callback) parentFragment).onPasswordVerified();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setMessage(R.string.retail_demo_reset_message)
                .setTitle(R.string.retail_demo_reset_title)
                .create();

        final Context context = dialog.getContext();
        final EditText passwordField = new EditText(context);
        passwordField.setSingleLine();
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                verifyPassword(dialog, passwordField.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                verifyPassword(dialog, passwordField.getText().toString());
                passwordField.requestFocus();
                final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(passwordField, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        dialog.setCanceledOnTouchOutside(false);

        final TypedArray a = context.obtainStyledAttributes(
                new int[] { android.R.attr.dialogPreferredPadding });
        final int sidePadding = a.getDimensionPixelSize(0, 0);
        dialog.setView(passwordField, sidePadding, 0, sidePadding, 0);
        a.recycle();

        return dialog;
    }

    private void verifyPassword(AlertDialog dialog, String input) {
        final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (mMessageDigest == null || TextUtils.isEmpty(input)) {
            positiveButton.setEnabled(false);
            return;
        }
        final String passwordHash = getContext().getString(
                com.android.internal.R.string.config_carrierDemoModePassword);
        if (passwordHash == null || TextUtils.isEmpty(passwordHash)) {
            // This device does not support carrier demo mode.
            return;
        }
        final byte[] inputDigest = mMessageDigest.digest(input.getBytes());
        final String inputHash = HexDump.toHexString(inputDigest, 0, inputDigest.length, false);
        positiveButton.setEnabled(TextUtils.equals(passwordHash, inputHash));
    }

    public interface Callback {
        void onPasswordVerified();
    }
}
