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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class DeleteSimProfileConfirmationDialog extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {
    public static final String TAG = "confirm_delete_sim";
    public static final String KEY_SUBSCRIPTION_INFO = "subscription_info";
    private SubscriptionInfo mInfo;

    public static DeleteSimProfileConfirmationDialog newInstance(SubscriptionInfo info) {
        final DeleteSimProfileConfirmationDialog dialog =
                new DeleteSimProfileConfirmationDialog();
        final Bundle args = new Bundle();
        args.putParcelable(KEY_SUBSCRIPTION_INFO, info);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mInfo = getArguments().getParcelable(KEY_SUBSCRIPTION_INFO);
        Context context = getContext();
        final String message = context.getString(R.string.mobile_network_erase_sim_dialog_body,
                mInfo.getCarrierName(), mInfo.getCarrierName());
        return new AlertDialog.Builder(context)
                .setTitle(R.string.mobile_network_erase_sim_dialog_title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.mobile_network_erase_sim_dialog_ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            beginDeletionWithProgress();
        }
    }

    @VisibleForTesting
    void beginDeletionWithProgress() {
        final DeleteSimProfileProgressDialog progress =
                DeleteSimProfileProgressDialog.newInstance(mInfo.getSubscriptionId());
        progress.setTargetFragment(getTargetFragment(), 0);
        progress.show(getFragmentManager(), DeleteSimProfileProgressDialog.TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_DELETE_SIM_CONFIRMATION;
    }
}
