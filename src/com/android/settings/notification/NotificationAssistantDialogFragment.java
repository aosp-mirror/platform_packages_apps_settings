/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class NotificationAssistantDialogFragment extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {
    static final String KEY_COMPONENT = "c";

    public static NotificationAssistantDialogFragment newInstance(Fragment target,
            ComponentName cn) {
        final NotificationAssistantDialogFragment dialogFragment =
                new NotificationAssistantDialogFragment();
        final Bundle args = new Bundle();
        args.putString(KEY_COMPONENT, cn == null ? "" : cn.flattenToString());
        dialogFragment.setArguments(args);
        dialogFragment.setTargetFragment(target, 0);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String summary = getResources()
                .getString(R.string.notification_assistant_security_warning_summary);
        return new AlertDialog.Builder(getContext())
                .setMessage(summary)
                .setCancelable(true)
                .setPositiveButton(R.string.okay, this)
                .create();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEFAULT_NOTIFICATION_ASSISTANT;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Bundle args = getArguments();
        final ComponentName cn = ComponentName.unflattenFromString(args
                .getString(KEY_COMPONENT));
        ConfigureNotificationSettings parent = (ConfigureNotificationSettings) getTargetFragment();
        parent.enableNAS(cn);
    }
}
