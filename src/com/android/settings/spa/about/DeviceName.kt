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

package com.android.settings.spa.about

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.deviceinfo.DeviceNamePreferenceController
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel

object DeviceNamePreference {

    @Composable
    fun EntryItem() {
        val context = LocalContext.current
        val deviceNamePresenter = remember { DeviceNamePresenter(context) }
        // TODO: Instead of a AlertDialog, it should be a dialog that accepts text input.
        val dialogPresenter = rememberAlertDialogPresenter(
            confirmButton = AlertDialogButton(
                stringResource(R.string.okay), onClick = DeviceNamePreference::confirmChange
            ),
            dismissButton = AlertDialogButton(stringResource(R.string.cancel)),
            title = stringResource(R.string.my_device_info_device_name_preference_title),
            text = { Text(deviceNamePresenter.deviceName) },
        )
        Preference(object : PreferenceModel {
            override val title =
                stringResource(R.string.my_device_info_device_name_preference_title)
            override val summary = { deviceNamePresenter.deviceName }
            override val onClick = dialogPresenter::open
        })

    }

    private fun confirmChange() {
        // TODO: Save the change of the device name.
    }
}

class DeviceNamePresenter(val context: Context) {
    private val deviceNamePreferenceController =
        DeviceNamePreferenceController(context, "unused_key")

    val deviceName: String get() = deviceNamePreferenceController.summary.toString()
}
