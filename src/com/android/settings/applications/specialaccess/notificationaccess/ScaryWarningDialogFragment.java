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
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;


public class ScaryWarningDialogFragment extends InstrumentedDialogFragment {
    private static final String KEY_COMPONENT = "c";
    private static final String KEY_LABEL = "l";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_NOTIFICATION_ACCESS_GRANT;
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

        return new AlertDialog.Builder(getContext())
                .setView(getDialogView(getContext(), label, parent, cn))
                .setCancelable(true)
                .create();
    }

    private View getDialogView(Context context, CharSequence label,
            NotificationAccessDetails parent, ComponentName cn) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = inflater.inflate(R.layout.enable_nls_dialog_content, null);

        Drawable icon = null;
        try {
            icon = context.getPackageManager().getApplicationIcon(cn.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
        }

        ImageView appIcon = content.findViewById(R.id.app_icon);
        if (icon != null) {
            appIcon.setImageDrawable(icon);
        } else {
            appIcon.setVisibility(View.GONE);
        }

        final String title = context.getResources().getString(
                R.string.notification_listener_security_warning_title, label);
        ((TextView) content.findViewById(R.id.title)).setText(title);

        final String prompt = context.getResources().getString(
                R.string.nls_warning_prompt, label);
        ((TextView) content.findViewById(R.id.prompt)).setText(prompt);

        Button allowButton = content.findViewById(R.id.allow_button);
        allowButton.setOnClickListener((view) -> {
            parent.enable(cn);
            dismiss();
        });
        Button denyButton = content.findViewById(R.id.deny_button);
        denyButton.setOnClickListener((view) -> {
            dismiss();
        });
        return content;
    }
}