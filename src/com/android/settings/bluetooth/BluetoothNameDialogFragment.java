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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Dialog fragment for renaming a Bluetooth device.
 */
abstract class BluetoothNameDialogFragment extends InstrumentedDialogFragment
        implements TextWatcher, TextView.OnEditorActionListener {

    // Key to save the edited name and edit status for restoring after rotation
    private static final String KEY_NAME = "device_name";
    private static final String KEY_NAME_EDITED = "device_name_edited";

    @VisibleForTesting
    AlertDialog mAlertDialog;
    private Button mOkButton;

    EditText mDeviceNameView;

    // This flag is set when the name is updated by code, to distinguish from user changes
    private boolean mDeviceNameUpdated;

    // This flag is set when the user edits the name (preserved on rotation)
    private boolean mDeviceNameEdited;

    /**
     * @return the title to use for the dialog.
     */
    abstract protected int getDialogTitle();

    /**
     * @return the current name used for this device.
     */
    abstract protected String getDeviceName();

    /**
     * Set the device to the given name.
     * @param deviceName the name to use
     */
    abstract protected void setDeviceName(String deviceName);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String deviceName = getDeviceName();
        if (savedInstanceState != null) {
            deviceName = savedInstanceState.getString(KEY_NAME, deviceName);
            mDeviceNameEdited = savedInstanceState.getBoolean(KEY_NAME_EDITED, false);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(getDialogTitle())
                .setView(createDialogView(deviceName))
                .setPositiveButton(R.string.bluetooth_rename_button, (dialog, which) -> {
                    setDeviceName(mDeviceNameView.getText().toString());
                })
                .setNegativeButton(android.R.string.cancel, null);
        mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(d -> {
            if (mDeviceNameView != null && mDeviceNameView.requestFocus()) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(mDeviceNameView, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        return mAlertDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_NAME, mDeviceNameView.getText().toString());
        outState.putBoolean(KEY_NAME_EDITED, mDeviceNameEdited);
    }

    private View createDialogView(String deviceName) {
        final LayoutInflater layoutInflater = (LayoutInflater)getActivity()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_edittext, null);
        mDeviceNameView = (EditText) view.findViewById(R.id.edittext);
        mDeviceNameView.setFilters(new InputFilter[] {
                new BluetoothLengthDeviceNameFilter()
        });
        mDeviceNameView.setText(deviceName);    // set initial value before adding listener
        if (!TextUtils.isEmpty(deviceName)) {
            mDeviceNameView.setSelection(deviceName.length());
        }
        mDeviceNameView.addTextChangedListener(this);
        com.android.settings.Utils.setEditTextCursorPosition(mDeviceNameView);
        mDeviceNameView.setOnEditorActionListener(this);
        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            setDeviceName(v.getText().toString());
            if (mAlertDialog != null && mAlertDialog.isShowing()) {
                mAlertDialog.dismiss();
            }
            return true;    // action handled
        } else {
            return false;   // not handled
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlertDialog = null;
        mDeviceNameView = null;
        mOkButton = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOkButton == null) {
            mOkButton = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            mOkButton.setEnabled(mDeviceNameEdited);    // Ok button enabled after user edits
        }
    }

    void updateDeviceName() {
        String name = getDeviceName();
        if (name != null) {
            mDeviceNameUpdated = true;
            mDeviceNameEdited = false;
            mDeviceNameView.setText(name);
        }
    }

    public void afterTextChanged(Editable s) {
        if (mDeviceNameUpdated) {
            // Device name changed by code; disable Ok button until edited by user
            mDeviceNameUpdated = false;
            mOkButton.setEnabled(false);
        } else {
            mDeviceNameEdited = true;
            if (mOkButton != null) {
                mOkButton.setEnabled(s.toString().trim().length() != 0);
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig, CharSequence s) {
        super.onConfigurationChanged(newConfig);
        if (mOkButton != null) {
            mOkButton.setEnabled(s.length() != 0 && !(s.toString().trim().isEmpty()));
        }
    }

    /* Not used */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /* Not used */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
