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

package com.android.settings.security;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.CredentialStorage;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.password.ChooseLockGeneric;

/**
 * Prompt for key guard configuration confirmation.
 */
public class ConfigureKeyGuardDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "ConfigureKeyGuardDialog";

    private boolean mConfigureConfirmed;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CONFIGURE_KEYGUARD_DIALOG;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(R.string.credentials_configure_lock_screen_hint)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        mConfigureConfirmed = (button == DialogInterface.BUTTON_POSITIVE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mConfigureConfirmed) {
            mConfigureConfirmed = false;
            startPasswordSetup();
            return;
        } else {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @VisibleForTesting
    void startPasswordSetup() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                CredentialStorage.MIN_PASSWORD_QUALITY);
        startActivity(intent);
    }
}
