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

package com.android.settings.biometrics2.ui.view;

import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.ErrorDialogData;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ERROR_DIALOG_ACTION_RESTART;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.biometrics.BiometricConstants;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Fingerprint error dialog, will be shown when an error occurs during fingerprint enrollment.
 */
public class FingerprintEnrollEnrollingErrorDialog extends InstrumentedDialogFragment {

    private FingerprintEnrollEnrollingViewModel mViewModel;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final ErrorDialogData data = mViewModel.getErrorDialogLiveData().getValue();
        final int errMsgId = data.getErrMsgId();
        final boolean canAssumeUdfps = mViewModel.canAssumeUdfps();
        final boolean wasTimeout = errMsgId == BiometricConstants.BIOMETRIC_ERROR_TIMEOUT;

        builder.setTitle(data.getErrTitle())
                .setMessage(data.getErrMsg())
                .setCancelable(false);
        if (wasTimeout && canAssumeUdfps) {
            builder.setPositiveButton(
                    R.string.security_settings_fingerprint_enroll_dialog_try_again,
                    (dialog, which) -> {
                            dialog.dismiss();
                            mViewModel.onErrorDialogAction(FINGERPRINT_ERROR_DIALOG_ACTION_RESTART);
                    });
            builder.setNegativeButton(
                    R.string.security_settings_fingerprint_enroll_dialog_ok,
                    (dialog, which) -> {
                            dialog.dismiss();
                            mViewModel.onErrorDialogAction(
                                    FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT);
                    });
        } else {
            builder.setPositiveButton(
                    R.string.security_settings_fingerprint_enroll_dialog_ok,
                    (dialog, which) -> {
                        dialog.dismiss();
                        mViewModel.onErrorDialogAction(wasTimeout
                                ? FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
                                : FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH);
                    });
        }
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FINGERPINT_ERROR;
    }

    @Override
    public void onAttach(Context context) {
        mViewModel = new ViewModelProvider(getActivity()).get(
                FingerprintEnrollEnrollingViewModel.class);
        super.onAttach(context);
    }
}
