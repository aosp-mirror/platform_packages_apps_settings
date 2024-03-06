/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Information dialog shown when enabling back animations
 */
public class BackAnimationPreferenceDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String TAG = "BackAnimationDlg";

    private BackAnimationPreferenceDialog() {
    }

    /**
     * Show this dialog.
     */
    public static void show(@NonNull Fragment host) {
        FragmentActivity activity = host.getActivity();
        if (activity == null) {
            return;
        }
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            BackAnimationPreferenceDialog dialog = new BackAnimationPreferenceDialog();
            dialog.setTargetFragment(host, 0 /* requestCode */);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_BACK_ANIMATIONS;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(com.android.settingslib.R.string.back_navigation_animation)
                .setMessage(com.android.settingslib.R.string.back_navigation_animation_dialog)
                .setPositiveButton(android.R.string.ok, this /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Do nothing
    }
}
