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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.wifi.flags.Flags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** Controller that controls whether the Wi-Fi Wakeup feature should be enabled. */
class WepNetworksPreferenceController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    private lateinit var preference: Preference

    var wifiManager = context.getSystemService(WifiManager::class.java)!!

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun getAvailabilityStatus() = if (Flags.wepUsage()) AVAILABLE
    else UNSUPPORTED_ON_DEVICE

    @Composable
    override fun Content() {
        val checked by wepAllowedFlow.flow.collectAsStateWithLifecycle(initialValue = null)
        SwitchPreference(object : SwitchPreferenceModel {
            override val title = stringResource(R.string.wifi_allow_wep_networks)
            override val summary = { getSummary() }
            override val checked = { checked }
            override val changeable: () -> Boolean
                get() = { carrierAllowed }
            override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                wifiManager.setWepAllowed(newChecked)
                wepAllowedFlow.override(newChecked)
            }
        })
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