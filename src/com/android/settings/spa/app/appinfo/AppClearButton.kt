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

package com.android.settings.spa.app.appinfo

import android.content.pm.PackageInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton

class AppClearButton(
    private val packageInfoPresenter: PackageInfoPresenter,
) {
    private val context = packageInfoPresenter.context

    private var openConfirmDialog by mutableStateOf(false)

    fun getActionButton(packageInfo: PackageInfo): ActionButton? {
        val app = packageInfo.applicationInfo
        if (!app.isInstantApp) return null

        return clearButton()
    }

    private fun clearButton() = ActionButton(
        text = context.getString(R.string.clear_instant_app_data),
        imageVector = Icons.Outlined.Delete,
    ) { openConfirmDialog = true }

    @Composable
    fun ClearConfirmDialog() {
        if (!openConfirmDialog) return
        AlertDialog(
            onDismissRequest = { openConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        openConfirmDialog = false
                        packageInfoPresenter.clearInstantApp()
                    },
                ) {
                    Text(stringResource(R.string.clear_instant_app_data))
                }
            },
            dismissButton = {
                TextButton(onClick = { openConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text(stringResource(R.string.clear_instant_app_data))
            },
            text = {
                Text(stringResource(R.string.clear_instant_app_confirmation))
            },
        )
    }
}
