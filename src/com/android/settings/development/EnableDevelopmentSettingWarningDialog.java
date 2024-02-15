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
import androidx.fragment.app.FragmentManager;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.R;

public class EnableDevelopmentSettingWarningDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String TAG = "EnableDevSettingDlg";

    public static void show(
            DevelopmentSettingsDashboardFragment host) {
        final EnableDevelopmentSettingWarningDialog dialog =
                new EnableDevelopmentSettingWarningDialog();
        dialog.setTargetFragment(host, 0 /* requestCode */);
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ENABLE_DEVELOPMENT_OPTIONS;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.dev_settings_warning_message)
                .setTitle(R.string.dev_settings_warning_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final DevelopmentSettingsDashboardFragment host =
                (DevelopmentSettingsDashboardFragment) getTargetFragment();
        if (which == DialogInterface.BUTTON_POSITIVE) {
            host.onEnableDevelopmentOptionsConfirmed();
        } else {
            host.onEnableDevelopmentOptionsRejected();
        }
    }
}
