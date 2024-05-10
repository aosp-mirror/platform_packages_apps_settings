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

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_SUW;

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

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class SetupSkipDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String EXTRA_FRP_SUPPORTED = ":settings:frp_supported";

    private static final String ARG_FRP_SUPPORTED = "frp_supported";
    // The key indicates type of screen lock credential types(PIN/Pattern/Password)
    private static final String ARG_LOCK_CREDENTIAL_TYPE = "lock_credential_type";
    // The key indicates type of lock screen setup is alphanumeric for password setup.
    private static final String TAG_SKIP_DIALOG = "skip_dialog";
    public static final int RESULT_SKIP = Activity.RESULT_FIRST_USER + 10;

    public static SetupSkipDialog newInstance(@LockPatternUtils.CredentialType int credentialType,
            boolean isFrpSupported, boolean forFingerprint, boolean forFace,
            boolean forBiometrics, boolean isSuw) {
        SetupSkipDialog dialog = new SetupSkipDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_LOCK_CREDENTIAL_TYPE, credentialType);
        args.putBoolean(ARG_FRP_SUPPORTED, isFrpSupported);
        args.putBoolean(EXTRA_KEY_FOR_FINGERPRINT, forFingerprint);
        args.putBoolean(EXTRA_KEY_FOR_FACE, forFace);
        args.putBoolean(EXTRA_KEY_FOR_BIOMETRICS, forBiometrics);
        args.putBoolean(EXTRA_KEY_IS_SUW, isSuw);
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

    private AlertDialog.Builder getBiometricsBuilder(
            @LockPatternUtils.CredentialType int credentialType, boolean isSuw, boolean hasFace,
            boolean hasFingerprint) {
        final boolean isFaceSupported = hasFace && (!isSuw || BiometricUtils.isFaceSupportedInSuw(
                getContext()));
        final int msgResId;
        final int screenLockResId;
        switch (credentialType) {
            case CREDENTIAL_TYPE_PATTERN:
                screenLockResId = R.string.unlock_set_unlock_pattern_title;
                msgResId = getPatternSkipMessageRes(hasFace && isFaceSupported, hasFingerprint);
                break;
            case CREDENTIAL_TYPE_PASSWORD:
                screenLockResId = R.string.unlock_set_unlock_password_title;
                msgResId = getPasswordSkipMessageRes(hasFace && isFaceSupported, hasFingerprint);
                break;
            case CREDENTIAL_TYPE_PIN:
            default:
                screenLockResId = R.string.unlock_set_unlock_pin_title;
                msgResId = getPinSkipMessageRes(hasFace && isFaceSupported, hasFingerprint);
                break;
        }
        return new AlertDialog.Builder(getContext())
                .setPositiveButton(R.string.skip_lock_screen_dialog_button_label, this)
                .setNegativeButton(R.string.cancel_lock_screen_dialog_button_label, this)
                .setTitle(getSkipSetupTitle(screenLockResId, hasFingerprint,
                        hasFace && isFaceSupported))
                .setMessage(msgResId);
    }

    @NonNull
    public AlertDialog.Builder onCreateDialogBuilder() {
        Bundle args = getArguments();
        final boolean isSuw = args.getBoolean(EXTRA_KEY_IS_SUW);
        final boolean forBiometrics = args.getBoolean(EXTRA_KEY_FOR_BIOMETRICS);
        final boolean forFace = args.getBoolean(EXTRA_KEY_FOR_FACE);
        final boolean forFingerprint = args.getBoolean(EXTRA_KEY_FOR_FINGERPRINT);
        @LockPatternUtils.CredentialType
        final int credentialType = args.getInt(ARG_LOCK_CREDENTIAL_TYPE);

        if (forFace || forFingerprint || forBiometrics) {
            final boolean hasFace = Utils.hasFaceHardware(getContext());
            final boolean hasFingerprint = Utils.hasFingerprintHardware(getContext());
            return getBiometricsBuilder(credentialType, isSuw, hasFace, hasFingerprint);
        }

        return new AlertDialog.Builder(getContext())
                .setPositiveButton(R.string.skip_anyway_button_label, this)
                .setNegativeButton(R.string.go_back_button_label, this)
                .setTitle(R.string.lock_screen_intro_skip_title)
                .setMessage(args.getBoolean(ARG_FRP_SUPPORTED) ?
                        R.string.lock_screen_intro_skip_dialog_text_frp :
                        R.string.lock_screen_intro_skip_dialog_text);
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

    private String getSkipSetupTitle(int screenTypeResId, boolean hasFingerprint,
            boolean hasFace) {
        return getString(R.string.lock_screen_skip_setup_title,
                BiometricUtils.getCombinedScreenLockOptions(getContext(),
                        getString(screenTypeResId), hasFingerprint, hasFace));
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