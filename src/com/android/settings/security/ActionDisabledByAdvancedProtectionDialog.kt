/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.security;

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.settings.R

import androidx.appcompat.app.AlertDialog;

class ActionDisabledByAdvancedProtectionDialog : Activity(), DialogInterface.OnDismissListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dialogView = layoutInflater.inflate(R.layout.support_details_dialog, null) as ViewGroup
        val builder = AlertDialog.Builder(this)
            .setPositiveButton(R.string.okay, null)
            .setView(dialogView)
            .setOnDismissListener(this)
        initializeDialogView(dialogView)
        builder.show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        finish()
    }

    private fun initializeDialogView(dialogView: View) {
        setSupportTitle(dialogView)
        setSupportDetails(dialogView)
    }

    private fun setSupportTitle(root: View) {
        val titleView: TextView = root.findViewById(R.id.admin_support_dialog_title) ?: return
        titleView.setText(R.string.disabled_by_advanced_protection_title)
    }

    private fun setSupportDetails(root: View) {
        val textView: TextView = root.findViewById(R.id.admin_support_msg)
        textView.setText(R.string.disabled_by_advanced_protection_message)
    }
}
