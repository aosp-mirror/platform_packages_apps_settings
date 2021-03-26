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
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Dialog asks if users want to empty trash files.
 */
public class EmptyTrashFragment extends InstrumentedDialogFragment {
    private static final String TAG_EMPTY_TRASH = "empty_trash";

    /** Shows the empty trash dialog. */
    public static void show(Fragment parent) {
        final EmptyTrashFragment dialog = new EmptyTrashFragment();
        dialog.setTargetFragment(parent, 0 /* requestCode */);
        dialog.show(parent.getFragmentManager(), TAG_EMPTY_TRASH);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_EMPTY_TRASH;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder.setTitle(R.string.storage_trash_dialog_title)
                .setMessage(R.string.storage_trash_dialog_ask_message)
                .setPositiveButton(R.string.storage_trash_dialog_confirm, (dialog, which) -> {
                    // TODO(170918505): Implement the logic in worker thread.
                }).setNegativeButton(android.R.string.cancel, null)
                .create();
    }
}
