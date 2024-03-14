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

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/** Dialog when user interacts 16K pages developer option and device is not OEM unlocked */
public class Enable16KOemUnlockDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "Enable16KOemUnlockDialog";

    /** This method is used to prompt user to do OEM unlock before using 16k */
    public static void show(@NonNull Fragment hostFragment) {
        final FragmentManager manager = hostFragment.getActivity().getSupportFragmentManager();
        Fragment existingFragment = manager.findFragmentByTag(TAG);
        if (existingFragment == null) {
            existingFragment = new Enable16KOemUnlockDialog();
        }

        if (existingFragment instanceof Enable16KOemUnlockDialog) {
            existingFragment.setTargetFragment(hostFragment, 0 /* requestCode */);
            ((Enable16KOemUnlockDialog) existingFragment).show(manager, TAG);
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
                .setTitle(R.string.confirm_oem_unlock_for_16k_title)
                .setMessage(R.string.confirm_oem_unlock_for_16k_text)
                .setPositiveButton(android.R.string.ok, this /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int buttonId) {
        // Do nothing. OEM unlock has to be done by user
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}
