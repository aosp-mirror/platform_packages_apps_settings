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

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

/** The base class for subscription action dialogs */
public class SubscriptionActionDialogActivity extends FragmentActivity {

    private static final String TAG = "SubscriptionActionDialogActivity";
    // Arguments
    protected static final String ARG_SUB_ID = "sub_id";
    protected SubscriptionManager mSubscriptionManager;

    public static final String SIM_ACTION_DIALOG_PREFS = "sim_action_dialog_prefs";
    // Shared preference keys
    public static final String KEY_PROGRESS_STATE = "progress_state";
    public static final int PROGRESS_IS_NOT_SHOWING = 0;
    public static final int PROGRESS_IS_SHOWING = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSubscriptionManager = getSystemService(SubscriptionManager.class)
                .createForAllUserProfiles();
        setProgressState(PROGRESS_IS_NOT_SHOWING);
    }


    @Override
    public void finish() {
        setProgressState(PROGRESS_IS_NOT_SHOWING);
        super.finish();
    }

    /**
     * Displays a loading dialog.
     *
     * @param message The string content should be displayed in the progress dialog.
     */
    protected void showProgressDialog(String message) {
        showProgressDialog(message,false);
    }

    /**
     * Displays a loading dialog.
     *
     * @param message The string content should be displayed in the progress dialog.
     * @param updateIfNeeded is whether to update the progress state in the SharedPreferences.
     */
    protected void showProgressDialog(String message, boolean updateIfNeeded) {
        ProgressDialogFragment.show(getFragmentManager(), message, null);
        if (updateIfNeeded) {
            setProgressState(PROGRESS_IS_SHOWING);
        }
    }

    /** Dismisses the loading dialog. */
    protected void dismissProgressDialog() {
        ProgressDialogFragment.dismiss(getFragmentManager());
        setProgressState(PROGRESS_IS_NOT_SHOWING);
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

    protected void setProgressState(int state) {
        final SharedPreferences prefs = getSharedPreferences(SIM_ACTION_DIALOG_PREFS, MODE_PRIVATE);
        prefs.edit().putInt(KEY_PROGRESS_STATE, state).apply();
        Log.i(TAG, "setProgressState:" + state);
    }
}
