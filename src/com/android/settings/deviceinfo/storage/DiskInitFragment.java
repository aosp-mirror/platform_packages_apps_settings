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

package com.android.settings.deviceinfo.storage;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.deviceinfo.StorageWizardInit;

/** A dialog which guides users to initialize a specified unsupported disk. */
public class DiskInitFragment extends InstrumentedDialogFragment {

    private static final String TAG_DISK_INIT = "disk_init";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_VOLUME_INIT;
    }

    /** Shows the dialog for the specified diskId from DiskInfo. */
    public static void show(Fragment parent, int resId, String diskId) {
        final Bundle args = new Bundle();
        args.putInt(Intent.EXTRA_TEXT, resId);
        args.putString(DiskInfo.EXTRA_DISK_ID, diskId);

        final DiskInitFragment dialog = new DiskInitFragment();
        dialog.setArguments(args);
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), TAG_DISK_INIT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final StorageManager storageManager = context.getSystemService(StorageManager.class);
        final int resId = getArguments().getInt(Intent.EXTRA_TEXT);
        final String diskId = getArguments().getString(DiskInfo.EXTRA_DISK_ID);
        final DiskInfo disk = storageManager.findDiskById(diskId);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setMessage(TextUtils.expandTemplate(getText(resId), disk.getDescription()))
                .setPositiveButton(R.string.storage_menu_set_up, (dialog, which) -> {
                    final Intent intent = new Intent(context, StorageWizardInit.class);
                    intent.putExtra(DiskInfo.EXTRA_DISK_ID, diskId);
                    startActivity(intent); })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}

