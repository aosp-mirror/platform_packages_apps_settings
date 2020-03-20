/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

/**
 * Class to show a variety of dialogs for the Wireless debugging
 * fragment.
 */
public class AdbWirelessDialog extends AlertDialog implements
        AdbWirelessDialogUiBase,
        DialogInterface.OnClickListener {

    /**
     * Interface for subscribers to implement in order to listen
     * to AdbWirelessDialog events.
     */
    public interface AdbWirelessDialogListener {
        /**
         * Called when the dialog was closed by clicking a negative button.
         */
        default void onCancel() {
        }

        /**
         * Called when the dialog was closed by clicking a positive button.
         *
         * @param dialog the dialog that was closed.
         */
        default void onSubmit(AdbWirelessDialog dialog) {
        }

        /**
         * Called when the dialog was dismissed.
         */
        default void onDismiss() {
        }
    }

    private static final String TAG = "AdbWirelessDialog";

    private static final int BUTTON_CANCEL = DialogInterface.BUTTON_NEGATIVE;
    private static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final AdbWirelessDialogListener mListener;
    private final int mMode;

    private View mView;
    private AdbWirelessDialogController mController;

    /**
     * Creates a AdbWirelessDialog with no additional style. It displays as a dialog above the
     * current view.
     */
    public static AdbWirelessDialog createModal(
            Context context,
            AdbWirelessDialogListener listener,
            int mode) {
        return new AdbWirelessDialog(context, listener, mode);
    }

    AdbWirelessDialog(Context context, AdbWirelessDialogListener listener, int mode) {
        super(context);
        mListener = listener;
        mMode = mode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.adb_wireless_dialog, null);
        setView(mView);
        mController = new AdbWirelessDialogController(this, mView, mMode);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
        super.onStop();

        dismiss();
        if (mListener != null) {
            mListener.onDismiss();
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int id) {
        if (mListener != null) {
            switch (id) {
                case BUTTON_CANCEL:
                    mListener.onCancel();
                    break;
            }
        }
    }

    @Override
    public AdbWirelessDialogController getController() {
        return mController;
    }

    @Override
    public void dispatchSubmit() {
        if (mListener != null) {
            mListener.onSubmit(this);
        }
        dismiss();
    }

    @Override
    public int getMode() {
        return mMode;
    }

    @Override
    public Button getSubmitButton() {
        return getButton(BUTTON_SUBMIT);
    }

    @Override
    public Button getCancelButton() {
        return getButton(BUTTON_NEGATIVE);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        setButton(BUTTON_SUBMIT, text, this);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(BUTTON_NEGATIVE, text, this);
    }
}
