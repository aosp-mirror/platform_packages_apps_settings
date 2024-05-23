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

package com.android.settings.wifi

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.SettingsAlertDialogWithIcon
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.wifi.flags.Flags
import com.android.wifitrackerlib.WifiEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** Controller that controls whether the WEP network can be connected. */
class WepNetworksPreferenceController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    var wifiManager = context.getSystemService(WifiManager::class.java)!!

    override fun getAvailabilityStatus() = if (Flags.androidVWifiApi()) AVAILABLE
    else UNSUPPORTED_ON_DEVICE

    @Composable
    override fun Content() {
        val checked by wepAllowedFlow.flow.collectAsStateWithLifecycle(initialValue = null)
        var openDialog by rememberSaveable { mutableStateOf(false) }
        val wifiInfo = wifiManager.connectionInfo
        SwitchPreference(object : SwitchPreferenceModel {
            override val title = stringResource(R.string.wifi_allow_wep_networks)
            override val summary = { getSummary() }
            override val checked = { checked }
            override val changeable: () -> Boolean
                get() = { carrierAllowed }
            override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                if (!newChecked && wifiInfo.currentSecurityType == WifiEntry.SECURITY_WEP) {
                    openDialog = true
                } else {
                    wifiManager.setWepAllowed(newChecked)
                    wepAllowedFlow.override(newChecked)
                }
            }
        })
        if (openDialog) {
            SettingsAlertDialogWithIcon(
                onDismissRequest = { openDialog = false },
                confirmButton = AlertDialogButton(
                    stringResource(R.string.wifi_disconnect_button_text)
                ) {
                    wifiManager.setWepAllowed(false)
                    wepAllowedFlow.override(false)
                    openDialog = false
                },
                dismissButton =
                AlertDialogButton(
                    stringResource(R.string.wifi_cancel)
                ) { openDialog = false },
                title = String.format(
                    stringResource(R.string.wifi_settings_wep_networks_disconnect_title),
                    wifiInfo.ssid.removeSurrounding("\"")
                ),
                text = {
                    Text(
                        stringResource(R.string.wifi_settings_wep_networks_disconnect_summary),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                })
        }
    }

    override fun getSummary(): String = mContext.getString(
        if (carrierAllowed) {
            R.string.wifi_allow_wep_networks_summary
        } else {
            R.string.wifi_allow_wep_networks_summary_carrier_not_allow
        }
    )

    private val carrierAllowed: Boolean
        get() = wifiManager.isWepSupported

    val wepAllowedFlow = OverridableFlow(callbackFlow {
        wifiManager.queryWepAllowed(Dispatchers.Default.asExecutor(), ::trySend)

        awaitClose { }
    })
}