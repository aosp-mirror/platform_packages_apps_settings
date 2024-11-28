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
import android.os.UserManager
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen
class MobileNetworkListScreen : PreferenceScreenCreator, PreferenceRestrictionMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.provider_network_settings_title

    override val icon: Int
        get() = R.drawable.ic_sim_card

    override val keywords: Int
        get() = R.string.keywords_more_mobile_networks

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)

    override fun isFlagEnabled(context: Context) = Flags.catalystMobileNetworkList()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = MobileNetworkListFragment::class.java

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(this) {}

    companion object {
        const val KEY = "mobile_network_list"
    }
}
