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

import android.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

/** The base class for subscription action dialogs */
public class SubscriptionActionDialogActivity extends Activity {

    private static final String TAG = "SubscriptionActionDialogActivity";

    private ProgressDialog mProgressDialog;
    private AlertDialog mErrorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Displays a loading dialog.
     *
     * @param message The string content should be displayed in the progress dialog.
     */
    protected void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, null, message);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    /** Dismisses the loading dialog. */
    protected void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Displays an error dialog to indicate the subscription action failure.
     *
     * @param title The title of the error dialog.
     * @param message The body text of the error dialog.
     * @param positiveOnClickListener The callback function after users confirm with the error.
     */
    protected void showErrorDialog(
            String title, String message, DialogInterface.OnClickListener positiveOnClickListener) {
        if (mErrorDialog == null) {
            mErrorDialog =
                    new AlertDialog.Builder(this)
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(
                                    R.string.ok,
                                    (dialog, which) -> {
                                        positiveOnClickListener.onClick(dialog, which);
                                        dismissErrorDialog();
                                    })
                            .create();
        }
        mErrorDialog.setMessage(message);
        mErrorDialog.show();
    }

    /** Dismisses the error dialog. */
    protected void dismissErrorDialog() {
        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
        }
    }
}
