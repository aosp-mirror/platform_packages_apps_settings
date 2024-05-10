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

package com.android.settings.spa.app

import android.os.UserManager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.applications.manageapplications.ResetAppsHelper
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.AlertDialogPresenter
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spa.widget.scaffold.MoreOptionsScope
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.template.scaffold.RestrictedMenuItem

@Composable
fun MoreOptionsScope.ResetAppPreferences(onClick: () -> Unit) {
    RestrictedMenuItem(
        text = stringResource(R.string.reset_app_preferences),
        restrictions = remember {
            Restrictions(keys = listOf(UserManager.DISALLOW_APPS_CONTROL))
        },
        onClick = onClick,
    )
}

@Composable
fun rememberResetAppDialogPresenter(): AlertDialogPresenter {
    val context = LocalContext.current
    return rememberAlertDialogPresenter(
        confirmButton = AlertDialogButton(stringResource(R.string.reset_app_preferences_button)) {
            ResetAppsHelper(context).resetApps()
        },
        dismissButton = AlertDialogButton(stringResource(R.string.cancel)),
        title = stringResource(R.string.reset_app_preferences_title),
        text = { Text(stringResource(R.string.reset_app_preferences_desc)) },
    )
}
