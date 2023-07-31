/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class DisableLogPersistWarningDialog extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "DisableLogPersistDlg";

    public static void show(LogPersistDialogHost host) {
        if (!(host instanceof Fragment)) {
            return;
        }
        final Fragment hostFragment = (Fragment) host;
        final FragmentManager manager = hostFragment.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final DisableLogPersistWarningDialog dialog =
                    new DisableLogPersistWarningDialog();
            dialog.setTargetFragment(hostFragment, 0 /* requestCode */);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_LOG_PERSIST;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(com.android.settingslib.R.string.dev_logpersist_clear_warning_title)
                .setMessage(com.android.settingslib.R.string.dev_logpersist_clear_warning_message)
                .setPositiveButton(android.R.string.ok, this /* onClickListener */)
                .setNegativeButton(android.R.string.cancel, this /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final LogPersistDialogHost host = (LogPersistDialogHost) getTargetFragment();
        if (host == null) {
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            host.onDisableLogPersistDialogConfirmed();
        } else {
            host.onDisableLogPersistDialogRejected();
        }
    }
}
