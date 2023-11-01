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
import android.app.settings.SettingsEnums
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.text.TextUtils
import android.util.Log
import android.widget.ImeAwareEditText
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintData
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "FingerprintSettingsRenameDialog"

class FingerprintSettingsRenameDialog : InstrumentedDialogFragment() {
  lateinit var onClickListener: DialogInterface.OnClickListener
  lateinit var onCancelListener: DialogInterface.OnCancelListener

  override fun onCancel(dialog: DialogInterface) {
    Log.d(TAG, "onCancel $dialog")
    onCancelListener.onCancel(dialog)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Log.d(TAG, "onCreateDialog $this")
    val fp = requireArguments().get(KEY_FINGERPRINT) as android.hardware.fingerprint.Fingerprint
    val fingerprintViewModel = FingerprintData(fp.name.toString(), fp.biometricId, fp.deviceId)

    val context = requireContext()
    val alertDialog =
      AlertDialog.Builder(context)
        .setView(R.layout.fingerprint_rename_dialog)
        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, onClickListener)
        .create()
    alertDialog.setOnShowListener {
      (dialog?.findViewById(R.id.fingerprint_rename_field) as ImeAwareEditText?)?.apply {
        val name = fingerprintViewModel.name
        setText(name)
        filters = this@FingerprintSettingsRenameDialog.getFilters()
        selectAll()
        requestFocus()
        scheduleShowSoftInput()
      }
    }

    return alertDialog
  }

  private fun getFilters(): Array<InputFilter> {
    val filter: InputFilter =
      object : InputFilter {

        override fun filter(
          source: CharSequence,
          start: Int,
          end: Int,
          dest: Spanned?,
          dstart: Int,
          dend: Int
        ): CharSequence? {
          for (index in start until end) {
            val c = source[index]
            // KXMLSerializer does not allow these characters,
            // see KXmlSerializer.java:162.
            if (c.code < 0x20) {
              return ""
            }
          }
          return null
        }
      }
    return arrayOf(filter)
  }

  override fun getMetricsCategory(): Int {
    return SettingsEnums.DIALOG_FINGERPINT_EDIT
  }

  companion object {
    private const val KEY_FINGERPRINT = "fingerprint"

    suspend fun showInstance(fp: FingerprintData, target: FingerprintSettingsV2Fragment) =
      suspendCancellableCoroutine { continuation ->
        val dialog = FingerprintSettingsRenameDialog()
        val onClick =
          DialogInterface.OnClickListener { _, _ ->
            val dialogTextField =
              dialog.requireDialog().requireViewById(R.id.fingerprint_rename_field)
                as ImeAwareEditText
            val newName = dialogTextField.text.toString()
            if (!TextUtils.equals(newName, fp.name)) {
              Log.d(TAG, "rename $fp.name to $newName for $dialog")
              continuation.resume(Pair(fp, newName))
            } else {
              continuation.resume(null)
            }
          }

        dialog.onClickListener = onClick
        dialog.onCancelListener =
          DialogInterface.OnCancelListener {
            Log.d(TAG, "onCancelListener clicked $dialog")
            continuation.resume(null)
          }

        continuation.invokeOnCancellation {
          Log.d(TAG, "invokeOnCancellation $dialog")
          dialog.dismiss()
        }

        val bundle = Bundle()
        bundle.putObject(
          KEY_FINGERPRINT,
          android.hardware.fingerprint.Fingerprint(fp.name, fp.fingerId, fp.deviceId)
        )
        dialog.arguments = bundle
        Log.d(TAG, "showing dialog $dialog")
        dialog.show(
          target.parentFragmentManager,
          FingerprintSettingsRenameDialog::class.java.toString()
        )
      }
  }
}
