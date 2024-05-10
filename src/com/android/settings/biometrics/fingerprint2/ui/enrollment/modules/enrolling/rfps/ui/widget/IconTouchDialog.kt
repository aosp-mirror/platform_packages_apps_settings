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
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "IconTouchDialog"

/** Dialog shown when the user taps the Progress bar a certain amount of times. */
class IconTouchDialog : InstrumentedDialogFragment() {
  lateinit var onDismissListener: DialogInterface.OnClickListener
  lateinit var onCancelListener: DialogInterface.OnCancelListener

  override fun onCancel(dialog: DialogInterface) {
    Log.d(TAG, "onCancel $dialog")
    onCancelListener.onCancel(dialog)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder: AlertDialog.Builder = AlertDialog.Builder(activity, R.style.Theme_AlertDialog)
    builder
      .setTitle(R.string.security_settings_fingerprint_enroll_touch_dialog_title)
      .setMessage(R.string.security_settings_fingerprint_enroll_touch_dialog_message)
      .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok) { dialog, which ->
        dialog.dismiss()
        onDismissListener.onClick(dialog, which)
      }
      .setOnCancelListener { onCancelListener.onCancel(it) }
    return builder.create()
  }

  override fun getMetricsCategory(): Int {
    return SettingsEnums.DIALOG_FINGERPRINT_ICON_TOUCH
  }

  companion object {
    suspend fun showInstance(fragment: Fragment) = suspendCancellableCoroutine { continuation ->
      val dialog = IconTouchDialog()
      dialog.onDismissListener =
        DialogInterface.OnClickListener { _, _ -> continuation.resume("Done") }
      dialog.onCancelListener =
        DialogInterface.OnCancelListener { _ -> continuation.resume("OnCancel") }

      continuation.invokeOnCancellation { Log.d(TAG, "invokeOnCancellation $dialog") }

      dialog.show(fragment.parentFragmentManager, IconTouchDialog::class.java.toString())
    }
  }
}
