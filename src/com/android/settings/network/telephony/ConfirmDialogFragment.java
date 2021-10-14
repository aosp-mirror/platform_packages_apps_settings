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

package com.android.settings.network.telephony;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/** Fragment to show a confirm dialog. The caller should implement onConfirmListener. */
public class ConfirmDialogFragment extends BaseDialogFragment
        implements DialogInterface.OnClickListener {
    private static final String TAG = "ConfirmDialogFragment";
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
    public static <T> void show(
            Activity activity,
            Class<T> callbackInterfaceClass,
            int tagInCaller,
            String title,
            String msg,
            String posButtonString,
            String negButtonString) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putCharSequence(ARG_MSG, msg);
        arguments.putString(ARG_POS_BUTTON_STRING, posButtonString);
        arguments.putString(ARG_NEG_BUTTON_STRING, negButtonString);
        setListener(activity, null, callbackInterfaceClass, tagInCaller, arguments);
        fragment.setArguments(arguments);
        fragment.show(activity.getFragmentManager(), TAG);
    }

    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString(ARG_TITLE);
        String message = getArguments().getString(ARG_MSG);
        String posBtnString = getArguments().getString(ARG_POS_BUTTON_STRING);
        String negBtnString = getArguments().getString(ARG_NEG_BUTTON_STRING);

        Log.i("Showing dialog with title = %s", title);
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext())
                        .setTitle(title)
                        .setPositiveButton(posBtnString, this)
                        .setNegativeButton(negBtnString, this);

        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        AlertDialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        informCaller(which == DialogInterface.BUTTON_POSITIVE);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        informCaller(false);
    }

    private void informCaller(boolean confirmed) {
        OnConfirmListener listener = getListener(OnConfirmListener.class);
        if (listener == null) {
            return;
        }
        listener.onConfirm(getTagInCaller(), confirmed);
    }
}
