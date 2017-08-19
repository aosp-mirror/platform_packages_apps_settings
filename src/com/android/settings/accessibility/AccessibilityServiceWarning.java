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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.storage.StorageManager;
import android.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.List;
import java.util.Locale;

import static android.view.WindowManager.LayoutParams.PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

/**
 * Utility class for creating the dialog that asks users for explicit permission to grant
 * all of the requested capabilities to an accessibility service before the service is enabled
 */
public class AccessibilityServiceWarning {
    public static Dialog createCapabilitiesDialog(Activity parentActivity,
            AccessibilityServiceInfo info, DialogInterface.OnClickListener listener) {
        final AlertDialog ad = new AlertDialog.Builder(parentActivity)
                .setTitle(parentActivity.getString(R.string.enable_service_title,
                        getServiceName(parentActivity, info)))
                .setView(createEnableDialogContentView(parentActivity, info))
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, listener)
                .create();

        final View.OnTouchListener filterTouchListener = (View v, MotionEvent event) -> {
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

        Window window = ad.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.privateFlags |= PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
        window.setAttributes(params);
        ad.create();
        ad.getButton(AlertDialog.BUTTON_POSITIVE).setOnTouchListener(filterTouchListener);
        ad.setCanceledOnTouchOutside(true);

        return ad;
    }

    /**
     * Return whether the device is encrypted with legacy full disk encryption. Newer devices
     * should be using File Based Encryption.
     *
     * @return true if device is encrypted
     */
    private static boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    /**
     * Get a content View for a dialog to confirm that they want to enable a service.
     *
     * @param context A valid context
     * @param info The info about a service
     * @return A content view suitable for viewing
     */
    private static View createEnableDialogContentView(Context context,
            AccessibilityServiceInfo info) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = inflater.inflate(R.layout.enable_accessibility_service_dialog_content,
                null);

        TextView encryptionWarningView = (TextView) content.findViewById(
                R.id.encryption_warning);
        if (isFullDiskEncrypted()) {
            String text = context.getString(R.string.enable_service_encryption_warning,
                    getServiceName(context, info));
            encryptionWarningView.setText(text);
            encryptionWarningView.setVisibility(View.VISIBLE);
        } else {
            encryptionWarningView.setVisibility(View.GONE);
        }

        TextView capabilitiesHeaderView = (TextView) content.findViewById(
                R.id.capabilities_header);
        capabilitiesHeaderView.setText(context.getString(R.string.capabilities_list_title,
                getServiceName(context, info)));

        LinearLayout capabilitiesView = (LinearLayout) content.findViewById(R.id.capabilities);

        // This capability is implicit for all services.
        View capabilityView = inflater.inflate(
                com.android.internal.R.layout.app_permission_item_old, null);

        ImageView imageView = (ImageView) capabilityView.findViewById(
                com.android.internal.R.id.perm_icon);
        imageView.setImageDrawable(context.getDrawable(
                com.android.internal.R.drawable.ic_text_dot));

        TextView labelView = (TextView) capabilityView.findViewById(
                com.android.internal.R.id.permission_group);
        labelView.setText(context.getString(
                R.string.capability_title_receiveAccessibilityEvents));

        TextView descriptionView = (TextView) capabilityView.findViewById(
                com.android.internal.R.id.permission_list);
        descriptionView.setText(
                context.getString(R.string.capability_desc_receiveAccessibilityEvents));

        List<AccessibilityServiceInfo.CapabilityInfo> capabilities =
                info.getCapabilityInfos(context);

        capabilitiesView.addView(capabilityView);

        // Service-specific capabilities.
        final int capabilityCount = capabilities.size();
        for (int i = 0; i < capabilityCount; i++) {
            AccessibilityServiceInfo.CapabilityInfo capability = capabilities.get(i);

            capabilityView = inflater.inflate(
                    com.android.internal.R.layout.app_permission_item_old, null);

            imageView = (ImageView) capabilityView.findViewById(
                    com.android.internal.R.id.perm_icon);
            imageView.setImageDrawable(context.getDrawable(
                    com.android.internal.R.drawable.ic_text_dot));

            labelView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_group);
            labelView.setText(context.getString(capability.titleResId));

            descriptionView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_list);
            descriptionView.setText(context.getString(capability.descResId));

            capabilitiesView.addView(capabilityView);
        }

        return content;
    }

    // Get the service name and bidi wrap it to protect from bidi side effects.
    private static CharSequence getServiceName(Context context, AccessibilityServiceInfo info) {
        final Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        final CharSequence label =
                info.getResolveInfo().loadLabel(context.getPackageManager());
        return BidiFormatter.getInstance(locale).unicodeWrap(label);
    }
}
