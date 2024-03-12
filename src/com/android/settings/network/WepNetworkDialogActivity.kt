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

package com.android.settings.network

import android.app.settings.SettingsEnums
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.wifi.ConfigureWifiSettings
import com.android.settingslib.spa.SpaBaseDialogActivity
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.SettingsAlertDialogWithIcon
import com.android.settingslib.wifi.WifiUtils.Companion.SSID

class WepNetworkDialogActivity : SpaBaseDialogActivity() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val wifiManager = context.getSystemService(WifiManager::class.java)
        SettingsAlertDialogWithIcon(
            onDismissRequest = { finish() },
            confirmButton = AlertDialogButton(
                getString(R.string.wifi_settings_ssid_block_button_close)
            ) { finish() },
            dismissButton = if (wifiManager?.isWepSupported == true)
                AlertDialogButton(
                    getString(R.string.wifi_settings_wep_networks_button_allow)
                ) {
                    SubSettingLauncher(context)
                        .setTitleText(context.getText(R.string.network_and_internet_preferences_title))
                        .setSourceMetricsCategory(SettingsEnums.CONFIGURE_WIFI)
                        .setDestination(ConfigureWifiSettings::class.java.getName())
                        .launch()
                    finish()
                } else null,
            title = String.format(
                getString(R.string.wifi_settings_wep_networks_blocked_title),
                intent.getStringExtra(SSID) ?: SSID
            ),
            text = {
                Text(
                    if (wifiManager?.isWepSupported == true)
                        getString(R.string.wifi_settings_wep_networks_summary_toggle_off)
                    else getString(R.string.wifi_settings_wep_networks_summary_blocked_by_carrier),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            })
    }
}