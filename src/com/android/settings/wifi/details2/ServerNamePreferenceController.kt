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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.wifi.flags.Flags
import com.android.wifitrackerlib.WifiEntry

class ServerNamePreferenceController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    private lateinit var wifiEntry: WifiEntry

    fun setWifiEntry(entry: WifiEntry) {
        wifiEntry = entry
    }

    override fun getAvailabilityStatus(): Int {
        return if (Flags.androidVWifiApi() && wifiEntry.certificateInfo?.domain != null) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE
    }

    @Composable
    override fun Content() {
        ServerName()
    }

    @Composable
    fun ServerName() {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.server_name_title)
            override val summary = { wifiEntry.certificateInfo?.domain ?: "" }
        })
    }
}