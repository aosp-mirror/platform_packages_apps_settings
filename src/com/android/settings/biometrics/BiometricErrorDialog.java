/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics;

import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_TIMEOUT;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricConstants;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Abstract dialog, shown when an error occurs during biometric enrollment.
 */
public abstract class BiometricErrorDialog extends InstrumentedDialogFragment {

    public static final String KEY_ERROR_MSG = "error_msg";
    public static final String KEY_ERROR_ID = "error_id";

    public abstract int getTitleResId();
    public abstract int getOkButtonTextResId();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        CharSequence errorString = getArguments().getCharSequence(KEY_ERROR_MSG);
        final int errMsgId = getArguments().getInt(KEY_ERROR_ID);

        builder.setTitle(getTitleResId())
                .setMessage(errorString)
                .setCancelable(false)
                .setPositiveButton(getOkButtonTextResId(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                boolean wasTimeout =
                                        errMsgId == BiometricConstants.BIOMETRIC_ERROR_TIMEOUT;
                                Activity activity = getActivity();
                                activity.setResult(wasTimeout ?
                                        RESULT_TIMEOUT : RESULT_FINISHED);
                                activity.finish();
                            }
                        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
