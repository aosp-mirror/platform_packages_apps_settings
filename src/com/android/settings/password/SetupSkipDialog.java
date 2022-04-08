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
 * limitations under the License
 */

package com.android.settings.password;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class SetupSkipDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String EXTRA_FRP_SUPPORTED = ":settings:frp_supported";

    private static final String ARG_FRP_SUPPORTED = "frp_supported";
    // The key indicates type of lock screen is pattern setup.
    private static final String ARG_LOCK_TYPE_PATTERN = "lock_type_pattern";
    // The key indicates type of lock screen setup is alphanumeric for password setup.
    private static final String ARG_LOCK_TYPE_ALPHANUMERIC = "lock_type_alphanumeric";
    private static final String TAG_SKIP_DIALOG = "skip_dialog";
    public static final int RESULT_SKIP = Activity.RESULT_FIRST_USER + 10;

    public static SetupSkipDialog newInstance(boolean isFrpSupported, boolean isPatternMode,
            boolean isAlphanumericMode, boolean isFingerprintSupported, boolean isFaceSupported) {
        SetupSkipDialog dialog = new SetupSkipDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FRP_SUPPORTED, isFrpSupported);
        args.putBoolean(ARG_LOCK_TYPE_PATTERN, isPatternMode);
        args.putBoolean(ARG_LOCK_TYPE_ALPHANUMERIC, isAlphanumericMode);
        args.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, isFingerprintSupported);
        args.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, isFaceSupported);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FINGERPRINT_SKIP_SETUP;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return onCreateDialogBuilder().create();
    }

    @NonNull
    public AlertDialog.Builder onCreateDialogBuilder() {
        Bundle args = getArguments();
        final boolean isFaceSupported =
                args.getBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE);
        final boolean isFingerprintSupported =
                args.getBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT);
        if (isFaceSupported || isFingerprintSupported) {
            final int titleId;

            if (args.getBoolean(ARG_LOCK_TYPE_PATTERN)) {
                titleId = R.string.lock_screen_pattern_skip_title;
            } else {
                titleId = args.getBoolean(ARG_LOCK_TYPE_ALPHANUMERIC) ?
                    R.string.lock_screen_password_skip_title : R.string.lock_screen_pin_skip_title;
            }

            return new AlertDialog.Builder(getContext())
                    .setPositiveButton(R.string.skip_lock_screen_dialog_button_label, this)
                    .setNegativeButton(R.string.cancel_lock_screen_dialog_button_label, this)
                    .setTitle(titleId)
                    .setMessage(isFaceSupported ?
                            R.string.face_lock_screen_setup_skip_dialog_text :
                            R.string.fingerprint_lock_screen_setup_skip_dialog_text);
        } else {
            return new AlertDialog.Builder(getContext())
                    .setPositiveButton(R.string.skip_anyway_button_label, this)
                    .setNegativeButton(R.string.go_back_button_label, this)
                    .setTitle(R.string.lock_screen_intro_skip_title)
                    .setMessage(args.getBoolean(ARG_FRP_SUPPORTED) ?
                            R.string.lock_screen_intro_skip_dialog_text_frp :
                            R.string.lock_screen_intro_skip_dialog_text);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        switch (button) {
            case DialogInterface.BUTTON_POSITIVE:
                Activity activity = getActivity();
                activity.setResult(RESULT_SKIP);
                activity.finish();
                break;
        }
    }

    public void show(FragmentManager manager) {
        show(manager, TAG_SKIP_DIALOG);
    }
}