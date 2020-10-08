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
import android.os.Bundle;
import android.telephony.SubscriptionManager;

/** The base class for subscription action dialogs */
public class SubscriptionActionDialogActivity extends Activity {

    private static final String TAG = "SubscriptionActionDialogActivity";
    // Arguments
    protected static final String ARG_SUB_ID = "sub_id";

    protected SubscriptionManager mSubscriptionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSubscriptionManager = getSystemService(SubscriptionManager.class);
    }

    /**
     * Displays a loading dialog.
     *
     * @param message The string content should be displayed in the progress dialog.
     */
    protected void showProgressDialog(String message) {
        ProgressDialogFragment.show(getFragmentManager(), message, null);
    }

    /** Dismisses the loading dialog. */
    protected void dismissProgressDialog() {
        ProgressDialogFragment.dismiss(getFragmentManager());
    }

    /**
     * Displays an error dialog to indicate the subscription action failure.
     *
     * @param title The title of the error dialog.
     * @param message The body text of the error dialog.
     */
    protected void showErrorDialog(String title, String message) {
        AlertDialogFragment.show(this, title, message);
    }
}
