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

package com.android.settings.biometrics2.ui.view;

import static android.content.DialogInterface.OnClickListener;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Skip dialog which shows when user clicks "Do it later" button in FingerprintFindSensor page.
 */
public class SkipSetupFindFpsDialog extends InstrumentedDialogFragment {

    private FingerprintEnrollFindSensorViewModel mViewModel;
    private final OnClickListener mOnSkipClickListener =
            (d, w) -> mViewModel.onSkipDialogButtonClick();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FINGERPRINT_SKIP_SETUP;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return onCreateDialogBuilder().create();
    }

    /**
     * Returns builder for this dialog
     */
    @NonNull
    private AlertDialog.Builder onCreateDialogBuilder() {
        return new AlertDialog.Builder(getActivity(), R.style.Theme_AlertDialog)
                .setTitle(R.string.setup_fingerprint_enroll_skip_title)
                .setPositiveButton(R.string.skip_anyway_button_label, mOnSkipClickListener)
                .setNegativeButton(R.string.go_back_button_label, null)
                .setMessage(R.string.setup_fingerprint_enroll_skip_after_adding_lock_text);
    }

    @Override
    public void onAttach(Context context) {
        mViewModel = new ViewModelProvider(getActivity()).get(
                FingerprintEnrollFindSensorViewModel.class);
        super.onAttach(context);
    }
}
