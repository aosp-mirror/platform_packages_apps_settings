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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
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
            boolean isAlphanumericMode, boolean forFingerprint, boolean forFace,
            boolean forBiometrics) {
        SetupSkipDialog dialog = new SetupSkipDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FRP_SUPPORTED, isFrpSupported);
        args.putBoolean(ARG_LOCK_TYPE_PATTERN, isPatternMode);
        args.putBoolean(ARG_LOCK_TYPE_ALPHANUMERIC, isAlphanumericMode);
        args.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, forFingerprint);
        args.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, forFace);
        args.putBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, forBiometrics);
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
        final boolean forFace =
                args.getBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE);
        final boolean forFingerprint =
                args.getBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT);
        final boolean forBiometrics =
                args.getBoolean(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS);
        if (forFace || forFingerprint || forBiometrics) {
            final boolean hasFace = forFace || forBiometrics;
            final boolean hasFingerprint = forFingerprint || forBiometrics;

            final int titleId;
            final int msgResId;
            if (args.getBoolean(ARG_LOCK_TYPE_PATTERN)) {
                titleId = getPatternSkipTitleRes(hasFace, hasFingerprint);
                msgResId = getPatternSkipMessageRes(hasFace, hasFingerprint);
            } else if (args.getBoolean(ARG_LOCK_TYPE_ALPHANUMERIC)) {
                titleId = getPasswordSkipTitleRes(hasFace, hasFingerprint);
                msgResId = getPasswordSkipMessageRes(hasFace, hasFingerprint);
            } else {
                titleId = getPinSkipTitleRes(hasFace, hasFingerprint);
                msgResId = getPinSkipMessageRes(hasFace, hasFingerprint);
            }

            return new AlertDialog.Builder(getContext())
                    .setPositiveButton(R.string.skip_lock_screen_dialog_button_label, this)
                    .setNegativeButton(R.string.cancel_lock_screen_dialog_button_label, this)
                    .setTitle(titleId)
                    .setMessage(msgResId);
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

    @StringRes
    private int getPatternSkipTitleRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_pattern_skip_biometrics_title;
        } else if (hasFace) {
            return R.string.lock_screen_pattern_skip_face_title;
        } else if (hasFingerprint) {
            return R.string.lock_screen_pattern_skip_fingerprint_title;
        } else {
            return R.string.lock_screen_pattern_skip_title;
        }
    }

    @StringRes
    private int getPatternSkipMessageRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_pattern_skip_biometrics_message;
        } else if (hasFace) {
            return R.string.lock_screen_pattern_skip_face_message;
        } else if (hasFingerprint) {
            return R.string.lock_screen_pattern_skip_fingerprint_message;
        } else {
            return R.string.lock_screen_pattern_skip_message;
        }
    }

    @StringRes
    private int getPasswordSkipTitleRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_password_skip_biometrics_title;
        } else if (hasFace) {
            return R.string.lock_screen_password_skip_face_title;
        } else if (hasFingerprint) {
            return R.string.lock_screen_password_skip_fingerprint_title;
        } else {
            return R.string.lock_screen_password_skip_title;
        }
    }

    @StringRes
    private int getPasswordSkipMessageRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_password_skip_biometrics_message;
        } else if (hasFace) {
            return R.string.lock_screen_password_skip_face_message;
        } else if (hasFingerprint) {
            return R.string.lock_screen_password_skip_fingerprint_message;
        } else {
            return R.string.lock_screen_password_skip_message;
        }
    }

    @StringRes
    private int getPinSkipTitleRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_pin_skip_biometrics_title;
        } else if (hasFace) {
            return R.string.lock_screen_pin_skip_face_title;
        } else if (hasFingerprint) {
            return R.string.lock_screen_pin_skip_fingerprint_title;
        } else {
            return R.string.lock_screen_pin_skip_title;
        }
    }

    @StringRes
    private int getPinSkipMessageRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_pin_skip_biometrics_message;
        } else if (hasFace) {
            return R.string.lock_screen_pin_skip_face_message;
        } else if (hasFingerprint) {
            return R.string.lock_screen_pin_skip_fingerprint_message;
        } else {
            return R.string.lock_screen_pin_skip_message;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        Activity activity = getActivity();
        switch (button) {
            case DialogInterface.BUTTON_POSITIVE:
                activity.setResult(RESULT_SKIP);
                activity.finish();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                View view = activity.getCurrentFocus();
                if(view != null) {
                    view.requestFocus();
                    InputMethodManager imm = (InputMethodManager) activity
                            .getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
                break;
        }
    }

    public void show(FragmentManager manager) {
        show(manager, TAG_SKIP_DIALOG);
    }
}