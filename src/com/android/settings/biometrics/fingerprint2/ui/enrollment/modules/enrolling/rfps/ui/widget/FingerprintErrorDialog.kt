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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.widget

import android.app.AlertDialog
import android.app.Dialog
import android.app.settings.SettingsEnums
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.FingerEnrollState
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "FingerprintErrorDialog"

/** A Dialog used for fingerprint enrollment when an error occurs. */
class FingerprintErrorDialog : InstrumentedDialogFragment() {
  private lateinit var onContinue: DialogInterface.OnClickListener
  private lateinit var onTryAgain: DialogInterface.OnClickListener
  private lateinit var onCancelListener: DialogInterface.OnCancelListener

  override fun onCancel(dialog: DialogInterface) {
    Log.d(TAG, "onCancel $dialog")
    onCancelListener.onCancel(dialog)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Log.d(TAG, "onCreateDialog $this")
    val errorString = requireArguments().getInt(KEY_MESSAGE)
    val errorTitle = requireArguments().getInt(KEY_TITLE)
    val builder = AlertDialog.Builder(requireContext())
    val shouldShowTryAgain = requireArguments().getBoolean(KEY_SHOULD_TRY_AGAIN)
    builder.setTitle(errorTitle).setMessage(errorString).setCancelable(false)

    if (shouldShowTryAgain) {
      builder
        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_try_again) {
          dialog,
          which ->
          dialog.dismiss()
          onTryAgain.onClick(dialog, which)
        }
        .setNegativeButton(R.string.security_settings_fingerprint_enroll_dialog_ok) { dialog, which
          ->
          dialog.dismiss()
          onContinue.onClick(dialog, which)
        }
    } else {
      builder.setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok) {
        dialog,
        which ->
        dialog.dismiss()
        onContinue.onClick(dialog, which)
      }
    }

    val dialog = builder.create()
    dialog.setCanceledOnTouchOutside(false)
    return dialog
  }

  override fun getMetricsCategory(): Int {
    return SettingsEnums.DIALOG_FINGERPINT_ERROR
  }

  companion object {
    private const val KEY_MESSAGE = "fingerprint_message"
    private const val KEY_TITLE = "fingerprint_title"
    private const val KEY_SHOULD_TRY_AGAIN = "should_try_again"

    suspend fun showInstance(
        error: FingerEnrollState.EnrollError,
        fragment: Fragment,
    ) = suspendCancellableCoroutine { continuation ->
      val dialog = FingerprintErrorDialog()
      dialog.onTryAgain = DialogInterface.OnClickListener { _, _ -> continuation.resume(true) }

      dialog.onContinue = DialogInterface.OnClickListener { _, _ -> continuation.resume(false) }

      dialog.onCancelListener =
        DialogInterface.OnCancelListener {
          Log.d(TAG, "onCancelListener clicked $dialog")
          continuation.resume(null)
        }

      continuation.invokeOnCancellation { Log.d(TAG, "invokeOnCancellation $dialog") }

      val bundle = Bundle()
      bundle.putInt(
        KEY_TITLE,
        error.errTitle,
      )
      bundle.putInt(
        KEY_MESSAGE,
        error.errString,
      )
      bundle.putBoolean(
        KEY_SHOULD_TRY_AGAIN,
        error.shouldRetryEnrollment,
      )
      dialog.arguments = bundle
      Log.d(TAG, "showing dialog $dialog")
      dialog.show(fragment.parentFragmentManager, FingerprintErrorDialog::class.java.toString())
    }
  }
}
