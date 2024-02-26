/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

/** Fragment to show a warning dialog. The caller should implement onConfirmListener. */
public class WarningDialogFragment extends BaseDialogFragment
        implements DialogInterface.OnClickListener {
    private static final String TAG = "WarningDialogFragment";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MSG = "msg";
    private static final String ARG_POS_BUTTON_STRING = "pos_button_string";
    private static final String ARG_NEG_BUTTON_STRING = "neg_button_string";

    /**
     * Interface defining the method that will be invoked when the user has done with the dialog.
     */
    public interface OnConfirmListener {
        /**
         * @param tag The tag in the caller.
         * @param confirmed True if the user has clicked the positive button. False if the user has
         *     clicked the negative button or cancel the dialog.
         */
        void onConfirm(int tag, boolean confirmed);
    }

    /** Displays a confirmation dialog which has confirm and cancel buttons. */
    static <T> void show(
            FragmentActivity activity,
            Class<T> callbackInterfaceClass,
            int tagInCaller,
            String title,
            String msg,
            String posButtonString,
            String negButtonString) {
        WarningDialogFragment fragment = new WarningDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putCharSequence(ARG_MSG, msg);
        arguments.putString(ARG_POS_BUTTON_STRING, posButtonString);
        arguments.putString(ARG_NEG_BUTTON_STRING, negButtonString);
        setListener(activity, null, callbackInterfaceClass, tagInCaller, arguments);
        fragment.setArguments(arguments);
        fragment.show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    @NonNull
    public final Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String title = getArguments().getString(ARG_TITLE);
        String message = getArguments().getString(ARG_MSG);
        String leftButton = getArguments().getString(ARG_POS_BUTTON_STRING);
        String rightButton = getArguments().getString(ARG_NEG_BUTTON_STRING);

        Log.i(TAG, "Showing dialog with title =" + title);
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext())
                        .setPositiveButton(rightButton, this)
                        .setNegativeButton(leftButton, this);

        View content =
                LayoutInflater.from(getContext())
                        .inflate(R.layout.sim_warning_dialog_wifi_connectivity, null);

        if (content != null) {
            TextView dialogTitle = content.findViewById(R.id.title);
            if (!TextUtils.isEmpty(title) && dialogTitle != null) {
                dialogTitle.setText(title);
                dialogTitle.setVisibility(View.VISIBLE);
            }
            TextView dialogMessage = content.findViewById(R.id.msg);
            if (!TextUtils.isEmpty(message) && dialogMessage != null) {
                dialogMessage.setText(message);
                dialogMessage.setVisibility(View.VISIBLE);
            }

            builder.setView(content);
        } else {
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            if (!TextUtils.isEmpty(message)) {
                builder.setMessage(message);
            }
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int which) {
        Log.i(TAG, "dialog onClick =" + which);

        // Positions of the buttons have been switch:
        // negative button = left button = the button to continue
        informCaller(which == DialogInterface.BUTTON_NEGATIVE);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        informCaller(false);
    }

    private void informCaller(boolean confirmed) {
        OnConfirmListener listener;
        try {
            listener = getListener(OnConfirmListener.class);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Do nothing and return.", e);
            return;
        }
        if (listener == null) {
            return;
        }
        listener.onConfirm(getTagInCaller(), confirmed);
    }
}
