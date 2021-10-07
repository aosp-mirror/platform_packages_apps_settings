/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Confirmation dialog shown to users with accessibility enabled who are trying to start the
 * non-accessibility enrollment flow.
 */
public class FaceEnrollAccessibilityDialog extends InstrumentedDialogFragment {
    private AlertDialog.OnClickListener mPositiveButtonListener;

    /**
     * @return new instance of the dialog
     */
    public static FaceEnrollAccessibilityDialog newInstance() {
        return new FaceEnrollAccessibilityDialog();
    }

    public void setPositiveButtonListener(AlertDialog.OnClickListener listener) {
        mPositiveButtonListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final int titleResId =
                R.string.security_settings_face_enroll_education_accessibility_dialog_message;
        final int negativeButtonResId =
                R.string.security_settings_face_enroll_education_accessibility_dialog_negative;
        final int positiveButtonResId =
                R.string.security_settings_face_enroll_education_accessibility_dialog_positive;

        builder.setMessage(titleResId)
                .setNegativeButton(negativeButtonResId, (dialog, which) -> {
                    dialog.cancel();
                })
                .setPositiveButton(positiveButtonResId, (dialog, which) -> {
                    mPositiveButtonListener.onClick(dialog, which);
                });

        return builder.create();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE_ENROLL_INTRO;
    }
}
