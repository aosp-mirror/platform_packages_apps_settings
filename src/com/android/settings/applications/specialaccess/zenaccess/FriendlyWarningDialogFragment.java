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
 * limitations under the License.
 */

package com.android.settings.applications.specialaccess.zenaccess;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Warning dialog when revoking zen access warning that zen rule instances will be deleted.
 */
public class FriendlyWarningDialogFragment extends InstrumentedDialogFragment {
    static final String KEY_PKG = "p";
    static final String KEY_LABEL = "l";


    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ZEN_ACCESS_REVOKE;
    }

    public FriendlyWarningDialogFragment setPkgInfo(String pkg, CharSequence label) {
        Bundle args = new Bundle();
        args.putString(KEY_PKG, pkg);
        args.putString(KEY_LABEL, TextUtils.isEmpty(label) ? pkg : label.toString());
        setArguments(args);
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        final String pkg = args.getString(KEY_PKG);
        final String label = args.getString(KEY_LABEL);

        final String title = getResources().getString(
                R.string.zen_access_revoke_warning_dialog_title, label);
        final String summary = getResources()
                .getString(R.string.zen_access_revoke_warning_dialog_summary);
        return new AlertDialog.Builder(getContext())
                .setMessage(summary)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(R.string.okay,
                        (dialog, id) -> {
                            ZenAccessController.deleteRules(getContext(), pkg);
                            ZenAccessController.setAccess(getContext(), pkg, false);
                        })
                .setNegativeButton(R.string.cancel,
                        (dialog, id) -> {
                            // pass
                        })
                .create();
    }
}
