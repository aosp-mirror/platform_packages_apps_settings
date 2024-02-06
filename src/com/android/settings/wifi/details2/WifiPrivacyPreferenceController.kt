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

package com.android.settings.wifi.details2

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.android.settings.R
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.wifi.flags.Flags

class WifiPrivacyPreferenceController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    private var wifiEntryKey: String? = null

    var wifiManager = context.getSystemService(WifiManager::class.java)!!

    fun setWifiEntryKey(key: String?) {
        wifiEntryKey = key
    }

    override fun getAvailabilityStatus() =
        if (Flags.androidVWifiApi() && wifiManager.isConnectedMacRandomizationSupported) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    @Composable
    override fun Content() {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.wifi_privacy_settings)
            override val icon = @Composable {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_wifi_privacy_24dp),
                    contentDescription = null
                )
            }
            override val onClick: () -> Unit =
                {
                    wifiEntryKey?.let {
                        mContext.startSpaActivity(WifiPrivacyPageProvider.getRoute(it))
                    }
                }
        })
    }
}