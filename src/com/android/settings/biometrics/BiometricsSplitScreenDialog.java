/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics;

import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FACE;

import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_SKIP;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricAuthenticator;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * This alert dialog shows when fingerprint is being added in multi window mode.
 */
public class BiometricsSplitScreenDialog extends InstrumentedDialogFragment {
    private static final String KEY_BIOMETRICS_MODALITY = "biometrics_modality";
    private static final String KEU_DESTROY_ACTIVITY = "destroy_activity";

    @BiometricAuthenticator.Modality
    private int mBiometricsModality;
    private boolean mDestroyActivity;

    /**
     * Returns the new instance of the class
     * @param biometricsModality Biometric modality.
     * @param destroyActivity Whether to destroy the activity
     * @return the current {@link BiometricsSplitScreenDialog}
     */
    public static BiometricsSplitScreenDialog newInstance(
            @BiometricAuthenticator.Modality int biometricsModality, boolean destroyActivity) {
        final BiometricsSplitScreenDialog dialog = new BiometricsSplitScreenDialog();
        final Bundle args = new Bundle();
        args.putInt(KEY_BIOMETRICS_MODALITY, biometricsModality);
        args.putBoolean(KEU_DESTROY_ACTIVITY, destroyActivity);
        dialog.setArguments(args);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mBiometricsModality = getArguments().getInt(KEY_BIOMETRICS_MODALITY);
        mDestroyActivity = getArguments().getBoolean(KEU_DESTROY_ACTIVITY);
        int titleId;
        int messageId;
        switch (mBiometricsModality) {
            case TYPE_FACE:
                titleId = R.string.biometric_settings_add_face_in_split_mode_title;
                messageId = R.string.biometric_settings_add_face_in_split_mode_message;
                break;
            default:
                titleId = R.string.biometric_settings_add_fingerprint_in_split_mode_title;
                messageId = R.string.biometric_settings_add_fingerprint_in_split_mode_message;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleId)
                .setMessage(messageId)
                .setCancelable(false)
                .setPositiveButton(
                        R.string.biometric_settings_add_biometrics_in_split_mode_ok,
                        (DialogInterface.OnClickListener) (dialog, which) -> {
                            dialog.dismiss();
                            if (mDestroyActivity) {
                                getActivity().setResult(RESULT_SKIP);
                                getActivity().finish();
                            }
                        });
        return builder.create();
    }

    @Override
    public int getMetricsCategory() {
        switch (mBiometricsModality) {
            case TYPE_FACE:
                return SettingsEnums.DIALOG_ADD_FACE_ERROR_IN_SPLIT_MODE;
            default:
                return SettingsEnums.DIALOG_ADD_FINGERPRINT_ERROR_IN_SPLIT_MODE;
        }
    }
}
