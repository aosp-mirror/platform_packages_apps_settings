/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.development;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.internal.annotations.Initializer;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/** Dialog when user interacts 16K pages developer option and data is f2fs */
public class EnableExt4WarningDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "EnableExt4WarningDialog";

    private EnableExt4DialogHost mHost;

    @Initializer
    private void setHost(@NonNull EnableExt4DialogHost host) {
        this.mHost = host;
    }

    /** This method is used to show warning dialog to reformat data to /ext4 */
    public static void show(
            @NonNull Fragment hostFragment, @NonNull EnableExt4DialogHost dialogHost) {
        final FragmentManager manager = hostFragment.getActivity().getSupportFragmentManager();
        Fragment existingFragment = manager.findFragmentByTag(TAG);
        if (existingFragment == null) {
            existingFragment = new EnableExt4WarningDialog();
        }

        if (existingFragment instanceof EnableExt4WarningDialog) {
            existingFragment.setTargetFragment(hostFragment, 0 /* requestCode */);
            ((EnableExt4WarningDialog) existingFragment).setHost(dialogHost);
            ((EnableExt4WarningDialog) existingFragment).show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ENABLE_16K_PAGES;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_format_ext4_title)
                .setIcon(R.drawable.ic_delete_accent)
                .setMessage(R.string.confirm_format_ext4_text)
                .setPositiveButton(R.string.confirm_ext4_button_text, this /* onClickListener */)
                .setNegativeButton(android.R.string.cancel, this /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int buttonId) {
        if (buttonId == DialogInterface.BUTTON_POSITIVE) {
            mHost.onExt4DialogConfirmed();
        } else {
            mHost.onExt4DialogDismissed();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        mHost.onExt4DialogDismissed();
    }
}
