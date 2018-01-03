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
 * limitations under the License.
 */

package com.android.settings.security;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class UnificationConfirmationDialog extends InstrumentedDialogFragment {
    private static final String EXTRA_COMPLIANT = "compliant";

    public static UnificationConfirmationDialog newIntance(boolean compliant) {
        UnificationConfirmationDialog
                dialog = new UnificationConfirmationDialog();
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_COMPLIANT, compliant);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (manager.findFragmentByTag(tag) == null) {
            // Prevent opening multiple dialogs if tapped on button quickly
            super.show(manager, tag);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SecuritySettingsV2 parentFragment = ((SecuritySettingsV2) getParentFragment());
        final boolean compliant = getArguments().getBoolean(EXTRA_COMPLIANT);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.lock_settings_profile_unification_dialog_title)
                .setMessage(compliant ? R.string.lock_settings_profile_unification_dialog_body
                        : R.string.lock_settings_profile_unification_dialog_uncompliant_body)
                .setPositiveButton(
                        compliant ? R.string.lock_settings_profile_unification_dialog_confirm
                                : R.string.lock_settings_profile_unification_dialog_uncompliant_confirm,
                        (dialog, whichButton) -> {
                            if (compliant) {
                                parentFragment.launchConfirmDeviceLockForUnification();
                            }    else {
                                parentFragment.unifyUncompliantLocks();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ((SecuritySettingsV2) getParentFragment()).updateUnificationPreference();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_UNIFICATION_CONFIRMATION;
    }
}
