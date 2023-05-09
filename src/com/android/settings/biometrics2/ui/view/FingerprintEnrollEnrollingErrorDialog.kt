/*
 * Copyright 2023 The Android Open Source Project
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
import android.hardware.biometrics.BiometricConstants
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.android.settings.R
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT

/**
 * Fingerprint error dialog, will be shown when an error occurs during fingerprint enrollment.
 */
class FingerprintEnrollEnrollingErrorDialog : DialogFragment() {

    private var mViewModel: FingerprintEnrollEnrollingViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val value = mViewModel!!.errorDialogLiveData.value!!
        return AlertDialog.Builder(requireActivity())
                .setTitle(value.errTitle)
                .setMessage(value.errMsg)
                .setCancelable(false)
                .setPositiveButton(
                        R.string.security_settings_fingerprint_enroll_dialog_ok
                ) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    mViewModel?.onErrorDialogAction(
                            if (value.errMsgId == BiometricConstants.BIOMETRIC_ERROR_TIMEOUT)
                                FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
                            else
                                FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH)
                }
                .create()
                .apply { setCanceledOnTouchOutside(false) }
    }

    override fun onAttach(context: Context) {
        mViewModel = ViewModelProvider(requireActivity())[
                FingerprintEnrollEnrollingViewModel::class.java]
        super.onAttach(context)
    }
}
