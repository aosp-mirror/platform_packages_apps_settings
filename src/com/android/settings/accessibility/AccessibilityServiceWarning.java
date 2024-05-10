/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.android.settings.R;

import java.util.Locale;

/**
 * Utility class for creating the dialog that asks users for explicit permission for an
 * accessibility service to access user data before the service is enabled
 */
public class AccessibilityServiceWarning {
    private static final View.OnTouchListener filterTouchListener = (View v, MotionEvent event) -> {
        // Filter obscured touches by consuming them.
        if (((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0)
                || ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0)) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Toast.makeText(v.getContext(), R.string.touch_filtered_warning,
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    };

    /**
     * The interface to execute the uninstallation action.
     */
    interface UninstallActionPerformer {
        void uninstallPackage();
    }

    /**
     * Returns a {@link Dialog} to be shown to confirm that they want to enable a service.
     * @deprecated Use {@link com.android.internal.accessibility.dialog.AccessibilityServiceWarning}
     */
    @Deprecated
    public static Dialog createCapabilitiesDialog(@NonNull Context context,
            @NonNull AccessibilityServiceInfo info, @NonNull View.OnClickListener listener,
            @NonNull UninstallActionPerformer performer) {
        final AlertDialog ad = new AlertDialog.Builder(context)
                .setView(createEnableDialogContentView(context, info, listener, performer))
                .create();

        Window window = ad.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.privateFlags |= SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
        window.setAttributes(params);
        ad.create();
        ad.setCanceledOnTouchOutside(true);

        return ad;
    }

    private static View createEnableDialogContentView(Context context,
            @NonNull AccessibilityServiceInfo info, View.OnClickListener listener,
            UninstallActionPerformer performer) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = inflater.inflate(R.layout.enable_accessibility_service_dialog_content,
                null);

        final Drawable icon;
        if (info.getResolveInfo().getIconResource() == 0) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_accessibility_generic);
        } else {
            icon = info.getResolveInfo().loadIcon(context.getPackageManager());
        }

        ImageView permissionDialogIcon = content.findViewById(
                R.id.permissionDialog_icon);
        permissionDialogIcon.setImageDrawable(icon);

        TextView permissionDialogTitle = content.findViewById(R.id.permissionDialog_title);
        permissionDialogTitle.setText(context.getString(R.string.enable_service_title,
                getServiceName(context, info)));

        Button permissionAllowButton = content.findViewById(
                R.id.permission_enable_allow_button);
        Button permissionDenyButton = content.findViewById(
                R.id.permission_enable_deny_button);
        permissionAllowButton.setOnClickListener(listener);
        permissionAllowButton.setOnTouchListener(filterTouchListener);
        permissionDenyButton.setOnClickListener(listener);

        final Button uninstallButton = content.findViewById(
                R.id.permission_enable_uninstall_button);
        // Shows an uninstall button to help users quickly remove the non-system App due to the
        // required permissions.
        if (!AccessibilityUtil.isSystemApp(info)) {
            uninstallButton.setVisibility(View.VISIBLE);
            uninstallButton.setOnClickListener(v -> performer.uninstallPackage());
        }
        return content;
    }

    /** Returns a {@link Dialog} to be shown to confirm that they want to disable a service. */
    public static Dialog createDisableDialog(Context context,
            AccessibilityServiceInfo info, DialogInterface.OnClickListener listener) {
        CharSequence serviceName = getServiceName(context, info);

        return new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.disable_service_title, serviceName))
                .setCancelable(true)
                .setPositiveButton(R.string.accessibility_dialog_button_stop, listener)
                .setNegativeButton(R.string.accessibility_dialog_button_cancel, listener)
                .create();
    }

    // Get the service name and bidi wrap it to protect from bidi side effects.
    private static CharSequence getServiceName(Context context, AccessibilityServiceInfo info) {
        final Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        final CharSequence label =
                info.getResolveInfo().loadLabel(context.getPackageManager());
        return BidiFormatter.getInstance(locale).unicodeWrap(label);
    }
}
