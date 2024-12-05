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

import android.content.Context
import com.android.settings.R
import com.android.settings.datausage.DataSaverScreen
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen
class NetworkDashboardScreen : PreferenceScreenCreator, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.network_dashboard_title

    override fun getIcon(context: Context) =
        when {
            Flags.homepageRevamp() -> R.drawable.ic_settings_wireless_filled
            else -> R.drawable.ic_settings_wireless
        }

    override fun isFlagEnabled(context: Context) = Flags.catalystNetworkProviderAndInternetScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = NetworkDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(this) {
            +MobileNetworkListScreen.KEY order -15
            +DataSaverScreen.KEY order 10
        }

    companion object {
        const val KEY = "network_provider_and_internet_screen"
    }
}
