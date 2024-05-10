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

package com.android.settings.biometrics.fingerprint2.ui.settings.fragment

import android.app.Dialog
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_FINGERPRINT_LAST_DELETE_MESSAGE
import android.app.admin.DevicePolicyResources.UNDEFINED
import android.app.settings.SettingsEnums
import android.content.DialogInterface
import android.os.Bundle
import android.os.UserManager
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintData
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val KEY_IS_LAST_FINGERPRINT = "IS_LAST_FINGERPRINT"

class FingerprintDeletionDialog : InstrumentedDialogFragment() {
  private lateinit var fingerprintViewModel: FingerprintData
  private var isLastFingerprint: Boolean = false
  private lateinit var alertDialog: AlertDialog
  lateinit var onClickListener: DialogInterface.OnClickListener
  lateinit var onNegativeClickListener: DialogInterface.OnClickListener
  lateinit var onCancelListener: DialogInterface.OnCancelListener

  override fun getMetricsCategory(): Int {
    return SettingsEnums.DIALOG_FINGERPINT_EDIT
  }

  override fun onCancel(dialog: DialogInterface) {
    onCancelListener.onCancel(dialog)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val fp = requireArguments().get(KEY_FINGERPRINT) as android.hardware.fingerprint.Fingerprint
    fingerprintViewModel = FingerprintData(fp.name.toString(), fp.biometricId, fp.deviceId)
    isLastFingerprint = requireArguments().getBoolean(KEY_IS_LAST_FINGERPRINT)
    val title = getString(R.string.fingerprint_delete_title, fingerprintViewModel.name)
    var message = getString(R.string.fingerprint_v2_delete_message, fingerprintViewModel.name)
    val context = requireContext()

    if (isLastFingerprint) {
      val isProfileChallengeUser = UserManager.get(context).isManagedProfile(context.userId)
      val messageId =
        if (isProfileChallengeUser) {
          WORK_PROFILE_FINGERPRINT_LAST_DELETE_MESSAGE
        } else {
          UNDEFINED
        }
      val defaultMessageId =
        if (isProfileChallengeUser) {
          R.string.fingerprint_last_delete_message_profile_challenge
        } else {
          R.string.fingerprint_last_delete_message
        }
      val devicePolicyManager = requireContext().getSystemService(DevicePolicyManager::class.java)
      message =
        devicePolicyManager?.resources?.getString(messageId) {
          message + "\n\n" + context.getString(defaultMessageId)
        }
          ?: ""
    }

    alertDialog =
      AlertDialog.Builder(requireActivity())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(
          R.string.security_settings_fingerprint_enroll_dialog_delete,
          onClickListener
        )
        .setNegativeButton(R.string.cancel, onNegativeClickListener)
        .create()
    return alertDialog
  }

  companion object {
    private const val KEY_FINGERPRINT = "fingerprint"
    suspend fun showInstance(
        fp: FingerprintData,
        lastFingerprint: Boolean,
        target: FingerprintSettingsV2Fragment,
    ) = suspendCancellableCoroutine { continuation ->
      val dialog = FingerprintDeletionDialog()
      dialog.onClickListener = DialogInterface.OnClickListener { _, _ -> continuation.resume(true) }
      dialog.onNegativeClickListener =
        DialogInterface.OnClickListener { _, _ -> continuation.resume(false) }
      dialog.onCancelListener = DialogInterface.OnCancelListener { continuation.resume(false) }

      continuation.invokeOnCancellation { dialog.dismiss() }
      val bundle = Bundle()
      bundle.putObject(
        KEY_FINGERPRINT,
        android.hardware.fingerprint.Fingerprint(fp.name, fp.fingerId, fp.deviceId)
      )
      bundle.putBoolean(KEY_IS_LAST_FINGERPRINT, lastFingerprint)
      dialog.arguments = bundle
      dialog.show(target.parentFragmentManager, FingerprintDeletionDialog::class.java.toString())
    }
  }
}
