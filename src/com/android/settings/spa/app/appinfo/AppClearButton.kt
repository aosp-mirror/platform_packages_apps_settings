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

import android.content.pm.ApplicationInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter

class AppClearButton(
    private val packageInfoPresenter: PackageInfoPresenter,
) {
    private val context = packageInfoPresenter.context

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton? {
        if (!app.isInstantApp) return null

        return clearButton()
    }

    @Composable
    private fun clearButton(): ActionButton {
        val dialogPresenter = confirmDialogPresenter()
        return ActionButton(
            text = context.getString(R.string.clear_instant_app_data),
            imageVector = Icons.Outlined.Delete,
            onClick = dialogPresenter::open,
        )
    }

    @Composable
    private fun confirmDialogPresenter() = rememberAlertDialogPresenter(
        confirmButton = AlertDialogButton(
            text = stringResource(R.string.clear_instant_app_data),
            onClick = packageInfoPresenter::clearInstantApp,
        ),
        dismissButton = AlertDialogButton(stringResource(R.string.cancel)),
        title = stringResource(R.string.clear_instant_app_data),
        text = { Text(stringResource(R.string.clear_instant_app_confirmation)) },
    )
}
