/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/** Dialog fragment for reboot confirmation when enabling certain features. */
public class RebootConfirmationDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String TAG = "FreeformPrefRebootDlg";

    private final int mMessageId;
    private final int mCancelButtonId;
    private final RebootConfirmationDialogHost mHost;

    /** Show an instance of this dialog. */
    public static void show(Fragment fragment, int messageId, RebootConfirmationDialogHost host) {
        show(fragment, messageId, R.string.reboot_dialog_reboot_later, host);
    }

    /** Show an instance of this dialog with cancel button string set as cancelButtonId */
    public static void show(
            Fragment fragment,
            int messageId,
            int cancelButtonId,
            RebootConfirmationDialogHost host) {
        final FragmentManager manager = fragment.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final RebootConfirmationDialogFragment dialog =
                    new RebootConfirmationDialogFragment(messageId, cancelButtonId, host);
            dialog.show(manager, TAG);
        }
    }

    private RebootConfirmationDialogFragment(
            int messageId, int cancelButtonId, RebootConfirmationDialogHost host) {
        mMessageId = messageId;
        mCancelButtonId = cancelButtonId;
        mHost = host;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REBOOT_CONFIRMATION_DIALOG;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstances) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(mMessageId)
                .setPositiveButton(R.string.reboot_dialog_reboot_now, this)
                .setNegativeButton(mCancelButtonId, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mHost.onRebootConfirmed(getContext());
        } else {
            mHost.onRebootCancelled();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mHost.onRebootDialogDismissed();
    }
}
