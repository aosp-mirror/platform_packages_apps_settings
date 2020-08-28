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
package com.android.settings.applications.specialaccess.notificationaccess;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;


public class ScaryWarningDialogFragment extends InstrumentedDialogFragment {
    private static final String KEY_COMPONENT = "c";
    private static final String KEY_LABEL = "l";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_SERVICE_ACCESS_WARNING;
    }

    public ScaryWarningDialogFragment setServiceInfo(ComponentName cn, CharSequence label,
            Fragment target) {
        Bundle args = new Bundle();
        args.putString(KEY_COMPONENT, cn.flattenToString());
        args.putCharSequence(KEY_LABEL, label);
        setArguments(args);
        setTargetFragment(target, 0);
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final CharSequence label = args.getCharSequence(KEY_LABEL);
        final ComponentName cn = ComponentName.unflattenFromString(args
                .getString(KEY_COMPONENT));
        NotificationAccessDetails parent = (NotificationAccessDetails) getTargetFragment();

        final String title = getResources().getString(
                R.string.notification_listener_security_warning_title, label);
        final String summary = getResources().getString(
                R.string.notification_listener_security_warning_summary, label);
        return new AlertDialog.Builder(getContext())
                .setMessage(summary)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(R.string.allow,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                parent.enable(cn);
                            }
                        })
                .setNegativeButton(R.string.deny,
                        (dialog, id) -> {
                            // pass
                        })
                .create();
    }
}