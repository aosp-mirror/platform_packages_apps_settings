/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import android.app.settings.SettingsEnums;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricErrorDialog;

/**
 * Fingerprint error dialog, will be shown when an error occurs during fingerprint enrollment.
 */
public class FingerprintErrorDialog extends BiometricErrorDialog {
    public static void showErrorDialog(BiometricEnrollBase host, int errMsgId) {
        final CharSequence errMsg = host.getText(getErrorMessage(errMsgId));
        final FingerprintErrorDialog dialog = newInstance(errMsg, errMsgId);
        final FragmentManager fragmentManager = host.getSupportFragmentManager();
        if (!fragmentManager.isDestroyed()) {
            dialog.show(fragmentManager, FingerprintErrorDialog.class.getName());
        }
    }

    private static int getErrorMessage(int errMsgId) {
        switch (errMsgId) {
            case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
                // This message happens when the underlying crypto layer decides to revoke the
                // enrollment auth token.
                return R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message;
            case FingerprintManager.FINGERPRINT_ERROR_BAD_CALIBRATION:
                return R.string.security_settings_fingerprint_bad_calibration;
            default:
                // There's nothing specific to tell the user about. Ask them to try again.
                return R.string.security_settings_fingerprint_enroll_error_generic_dialog_message;
        }
    }

    private static FingerprintErrorDialog newInstance(CharSequence msg, int msgId) {
        FingerprintErrorDialog dialog = new FingerprintErrorDialog();
        Bundle args = new Bundle();
        args.putCharSequence(KEY_ERROR_MSG, msg);
        args.putInt(KEY_ERROR_ID, msgId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getTitleResId() {
        return R.string.security_settings_fingerprint_enroll_error_dialog_title;
    }

    @Override
    public int getOkButtonTextResId() {
        return R.string.security_settings_fingerprint_enroll_dialog_ok;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FINGERPINT_ERROR;
    }
}
