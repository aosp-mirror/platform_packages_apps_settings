/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.euicc.EuiccManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class DeleteSimProfileProgressDialog extends InstrumentedDialogFragment {
    public static final String TAG = "delete_sim_progress";

    // Note that this must be listed in AndroidManfiest.xml in a <protected-broadcast> tag
    @VisibleForTesting
    static final String PENDING_INTENT =
            "com.android.settings.DELETE_SIM_PROFILE_RESULT";
    private static final int PENDING_INTENT_REQUEST_CODE = 1;
    private static final String KEY_SUBSCRIPTION_ID = "subscription_id";
    @VisibleForTesting
    static final String KEY_DELETE_STARTED = "delete_started";

    private boolean mDeleteStarted;
    private BroadcastReceiver mReceiver;

    public static DeleteSimProfileProgressDialog newInstance(int subscriptionId) {
        final DeleteSimProfileProgressDialog dialog = new DeleteSimProfileProgressDialog();
        final Bundle args = new Bundle();
        args.putInt(KEY_SUBSCRIPTION_ID, subscriptionId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_DELETE_STARTED, mDeleteStarted);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mDeleteStarted = savedInstanceState.getBoolean(KEY_DELETE_STARTED, false);
        }
        final Context context = getContext();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(
                context.getString(R.string.mobile_network_erase_sim_dialog_progress));

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                dismiss();
                final Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    activity.finish();
                }
            }
        };
        context.registerReceiver(mReceiver, new IntentFilter(PENDING_INTENT));

        if (!mDeleteStarted) {
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    PENDING_INTENT_REQUEST_CODE, new Intent(PENDING_INTENT),
                    PendingIntent.FLAG_ONE_SHOT);

            final EuiccManager euiccManager = context.getSystemService(EuiccManager.class);
            final int subId = getArguments().getInt(KEY_SUBSCRIPTION_ID);
            euiccManager.deleteSubscription(subId, pendingIntent);
            mDeleteStarted = true;
        }

        return progressDialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (mReceiver != null) {
            final Context context = getContext();
            if (context != null) {
                context.unregisterReceiver(mReceiver);
            }
            mReceiver = null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_DELETE_SIM_PROGRESS;
    }
}
