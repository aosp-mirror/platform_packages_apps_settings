/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import static android.os.storage.DiskInfo.EXTRA_DISK_ID;

import static com.android.settings.deviceinfo.StorageWizardBase.EXTRA_FORMAT_FORGET_UUID;
import static com.android.settings.deviceinfo.StorageWizardBase.EXTRA_FORMAT_PRIVATE;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class StorageWizardFormatConfirm extends InstrumentedDialogFragment {
    private static final String TAG_FORMAT_WARNING = "format_warning";

    public static void showPublic(FragmentActivity activity, String diskId) {
        show(activity, diskId, null, false);
    }

    public static void showPublic(FragmentActivity activity, String diskId, String forgetUuid) {
        show(activity, diskId, forgetUuid, false);
    }

    public static void showPrivate(FragmentActivity activity, String diskId) {
        show(activity, diskId, null, true);
    }

    private static void show(FragmentActivity activity, String diskId, String formatForgetUuid,
            boolean formatPrivate) {
        final Bundle args = new Bundle();
        args.putString(EXTRA_DISK_ID, diskId);
        args.putString(EXTRA_FORMAT_FORGET_UUID, formatForgetUuid);
        args.putBoolean(EXTRA_FORMAT_PRIVATE, formatPrivate);

        final StorageWizardFormatConfirm fragment = new StorageWizardFormatConfirm();
        fragment.setArguments(args);
        fragment.show(activity.getSupportFragmentManager(), TAG_FORMAT_WARNING);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_VOLUME_FORMAT;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();

        final Bundle args = getArguments();
        final String diskId = args.getString(EXTRA_DISK_ID);
        final String formatForgetUuid = args.getString(EXTRA_FORMAT_FORGET_UUID);
        final boolean formatPrivate = args.getBoolean(EXTRA_FORMAT_PRIVATE, false);

        final DiskInfo disk = context.getSystemService(StorageManager.class)
                .findDiskById(diskId);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(TextUtils.expandTemplate(
                getText(R.string.storage_wizard_format_confirm_v2_title),
                disk.getShortDescription()));
        builder.setMessage(TextUtils.expandTemplate(
                getText(R.string.storage_wizard_format_confirm_v2_body),
                disk.getDescription(),
                disk.getShortDescription(),
                disk.getShortDescription()));

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(
                TextUtils.expandTemplate(getText(R.string.storage_wizard_format_confirm_v2_action),
                        disk.getShortDescription()),
                (dialog, which) -> {
                    final Intent intent = new Intent(context, StorageWizardFormatProgress.class);
                    intent.putExtra(EXTRA_DISK_ID, diskId);
                    intent.putExtra(EXTRA_FORMAT_FORGET_UUID, formatForgetUuid);
                    intent.putExtra(EXTRA_FORMAT_PRIVATE, formatPrivate);
                    context.startActivity(intent);
                });

        return builder.create();
    }
}
