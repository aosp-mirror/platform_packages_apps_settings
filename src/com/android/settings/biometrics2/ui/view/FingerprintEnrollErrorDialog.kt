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
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_UNABLE_TO_PROCESS
import android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.settings.R
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog.getErrorMessage
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog.getErrorTitle
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog.getSetupErrorMessage
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollErrorDialogViewModel
import com.android.settings.biometrics2.ui.viewmodel.FingerprintErrorDialogSetResultAction.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH
import com.android.settings.biometrics2.ui.viewmodel.FingerprintErrorDialogSetResultAction.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
import kotlinx.coroutines.launch

/**
 * Fingerprint error dialog, will be shown when an error occurs during fingerprint enrollment.
 */
class FingerprintEnrollErrorDialog : DialogFragment() {

    private val viewModel: FingerprintEnrollErrorDialogViewModel?
        get() = activity?.let {
            ViewModelProvider(it)[FingerprintEnrollErrorDialogViewModel::class.java]
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val errorMsgId: Int = requireArguments().getInt(KEY_ERROR_MSG_ID)
        val okButtonSetResultAction =
            if (errorMsgId == BiometricConstants.BIOMETRIC_ERROR_TIMEOUT)
                FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
            else
                FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH
        return requireActivity().bindFingerprintEnrollEnrollingErrorDialog(
                errorMsgId = errorMsgId,
                isSuw = viewModel!!.isSuw,
                tryAgainButtonClickListener = { dialog: DialogInterface?, _: Int ->
                    activity?.lifecycleScope?.launch {
                        Log.d(TAG, "tryAgain flow")
                        viewModel?.triggerRetry()
                        dialog?.dismiss()
                    }
                },
                okButtonClickListener = { dialog: DialogInterface?, _: Int ->
                    activity?.lifecycleScope?.launch {
                        Log.d(TAG, "ok flow as $okButtonSetResultAction")
                        viewModel?.setResultAndFinish(okButtonSetResultAction)
                        dialog?.dismiss()
                    }
                }
            )
    }

    companion object {
        private const val TAG = "FingerprintEnrollErrorDialog"
        private const val KEY_ERROR_MSG_ID = "error_msg_id"

        fun newInstance(errorMsgId: Int): FingerprintEnrollErrorDialog {
            val dialog = FingerprintEnrollErrorDialog()
            val args = Bundle()
            args.putInt(KEY_ERROR_MSG_ID, errorMsgId)
            dialog.arguments = args
            return dialog
        }
    }
}

fun Context.bindFingerprintEnrollEnrollingErrorDialog(
    errorMsgId: Int,
    isSuw: Boolean,
    tryAgainButtonClickListener: DialogInterface.OnClickListener,
    okButtonClickListener: DialogInterface.OnClickListener
): AlertDialog = AlertDialog.Builder(this)
    .setTitle(getString(getErrorTitle(errorMsgId)))
    .setMessage(
        getString(
            if (isSuw)
                getSetupErrorMessage(errorMsgId)
            else
                getErrorMessage(errorMsgId)
        )
    )
    .setCancelable(false).apply {
        if (errorMsgId == FINGERPRINT_ERROR_UNABLE_TO_PROCESS) {
            setPositiveButton(
                R.string.security_settings_fingerprint_enroll_dialog_try_again,
                tryAgainButtonClickListener
            )
            setNegativeButton(
                R.string.security_settings_fingerprint_enroll_dialog_ok,
                okButtonClickListener
            )
        } else {
            setPositiveButton(
                R.string.security_settings_fingerprint_enroll_dialog_ok,
                okButtonClickListener
            )
        }
    }
    .create()
    .apply { setCanceledOnTouchOutside(false) }