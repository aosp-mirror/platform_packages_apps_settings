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
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen
class NetworkProviderScreen : PreferenceScreenCreator, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.provider_internet_settings

    override val icon: Int
        get() = R.drawable.ic_settings_wireless

    override val keywords: Int
        get() = R.string.keywords_internet

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_internet_settings)

    override fun isFlagEnabled(context: Context) = Flags.catalystInternetSettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = NetworkProviderSettings::class.java

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(this) {}

    companion object {
        const val KEY = "internet_settings"
    }
}
