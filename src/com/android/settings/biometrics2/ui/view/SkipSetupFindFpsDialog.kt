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
package com.android.settings.biometrics2.ui.view

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.android.settings.R
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel

/**
 * Skip dialog which shows when user clicks "Do it later" button in FingerprintFindSensor page.
 */
class SkipSetupFindFpsDialog : DialogFragment() {

    private var mViewModel: FingerprintEnrollFindSensorViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        requireActivity().bindSkipSetupFindFpsDialog {
            _: DialogInterface?, _: Int -> mViewModel?.onSkipDialogButtonClick()
        }

    override fun onAttach(context: Context) {
        mViewModel = ViewModelProvider(requireActivity())[
            FingerprintEnrollFindSensorViewModel::class.java
        ]
        super.onAttach(context)
    }
}

fun Context.bindSkipSetupFindFpsDialog(
    positiveButtonClickListener: DialogInterface.OnClickListener
): AlertDialog =
    AlertDialog.Builder(this, R.style.Theme_AlertDialog)
        .setTitle(R.string.setup_fingerprint_enroll_skip_title)
        .setPositiveButton(R.string.skip_anyway_button_label, positiveButtonClickListener)
        .setNegativeButton(R.string.go_back_button_label, null)
        .setMessage(R.string.setup_fingerprint_enroll_skip_after_adding_lock_text)
        .create()
