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
package com.android.settings.biometrics.fingerprint

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.settings.R

class UdfpsEnrollCalibrationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(requireActivity(), R.style.Theme_AlertDialog)
                    .setTitle(arguments!!.getInt(KEY_TITLE_TEXT_ID))
                    .setMessage(arguments!!.getInt(KEY_MESSAGE_TEXT_ID))
                    .setPositiveButton(arguments!!.getInt(KEY_DISMISS_BUTTON_TEXT_ID)) {
                        dialog: DialogInterface?, _: Int -> dialog?.dismiss()
                    }
                    .create().also {
                        isCancelable = false
                    }

    companion object {

        private const val KEY_TITLE_TEXT_ID = "title_text_id"
        private const val KEY_MESSAGE_TEXT_ID = "message_text_id"
        private const val KEY_DISMISS_BUTTON_TEXT_ID = "dismiss_button_text_id"

        @JvmStatic
        fun newInstance(
                @StringRes titleTextId: Int,
                @StringRes messageTextId: Int,
                @StringRes dismissButtonTextId: Int
        ) = UdfpsEnrollCalibrationDialog().apply {
            arguments = Bundle().apply {
                putInt(KEY_TITLE_TEXT_ID, titleTextId)
                putInt(KEY_MESSAGE_TEXT_ID, messageTextId)
                putInt(KEY_DISMISS_BUTTON_TEXT_ID, dismissButtonTextId)
            }
        }
    }
}
