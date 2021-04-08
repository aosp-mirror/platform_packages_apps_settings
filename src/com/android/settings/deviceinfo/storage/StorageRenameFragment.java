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
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Dialog that allows editing of volume nickname.
 */
public class StorageRenameFragment extends InstrumentedDialogFragment {
    private static final String TAG_RENAME = "rename";

    /** Shows the rename dialog. */
    public static void show(Fragment parent, VolumeInfo vol) {
        final StorageRenameFragment dialog = new StorageRenameFragment();
        dialog.setTargetFragment(parent, 0 /* requestCode */);
        final Bundle args = new Bundle();
        args.putString(VolumeRecord.EXTRA_FS_UUID, vol.getFsUuid());
        dialog.setArguments(args);
        dialog.show(parent.getFragmentManager(), TAG_RENAME);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_VOLUME_RENAME;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final StorageManager storageManager = context.getSystemService(StorageManager.class);

        final String fsUuid = getArguments().getString(VolumeRecord.EXTRA_FS_UUID);
        final VolumeRecord rec = storageManager.findRecordByUuid(fsUuid);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

        final View view = dialogInflater.inflate(R.layout.dialog_edittext, null, false);
        final EditText nickname = (EditText) view.findViewById(R.id.edittext);
        nickname.setText(rec.getNickname());
        nickname.requestFocus();

        return builder.setTitle(R.string.storage_rename_title)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) ->
                    // TODO: move to background thread
                    storageManager.setVolumeNickname(fsUuid, nickname.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
