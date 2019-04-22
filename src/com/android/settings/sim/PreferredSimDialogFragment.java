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

package com.android.settings.sim;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

/**
 * Presents a dialog asking the user if they want to update all services to use a given "preferred"
 * SIM. Typically this would be used in a case where a device goes from having multiple SIMs down to
 * only one.
 */
public class PreferredSimDialogFragment extends SimDialogFragment implements
        DialogInterface.OnClickListener {
    private static final String TAG = "PreferredSimDialogFrag";

    public static PreferredSimDialogFragment newInstance() {
        final PreferredSimDialogFragment fragment = new PreferredSimDialogFragment();
        final Bundle args = initArguments(SimDialogActivity.PREFERRED_PICK,
                R.string.sim_preferred_title);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getTitleResId())
                .setPositiveButton(R.string.yes, this)
                .setNegativeButton(R.string.no, null)
                .create();
        updateDialog(dialog);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int buttonClicked) {
        if (buttonClicked != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        final SimDialogActivity activity = (SimDialogActivity) getActivity();
        final SubscriptionInfo info = getPreferredSubscription();
        if (info != null) {
            activity.onSubscriptionSelected(getDialogType(), info.getSubscriptionId());
        }
    }

    public SubscriptionInfo getPreferredSubscription() {
        final Activity activity = getActivity();
        final int slotId = activity.getIntent().getIntExtra(SimDialogActivity.PREFERRED_SIM,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        return getSubscriptionManager().getActiveSubscriptionInfoForSimSlotIndex(slotId);
    }

    private void updateDialog(AlertDialog dialog) {
        final SubscriptionInfo info = getPreferredSubscription();
        if (info == null) {
            dismiss();
            return;
        }
        final String message =
                getContext().getString(R.string.sim_preferred_message, info.getDisplayName());
        dialog.setMessage(message);
    }

    @Override
    public void updateDialog() {
        updateDialog((AlertDialog) getDialog());
    }

    @VisibleForTesting
    protected SubscriptionManager getSubscriptionManager() {
        return getContext().getSystemService(SubscriptionManager.class);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_PREFERRED_SIM_PICKER;
    }
}
