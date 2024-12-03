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
package com.android.settings.network.tether

import android.content.Context
import android.net.TetheringManager
import android.os.UserManager
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.TetherPreferenceController
import com.android.settings.wifi.tether.WifiHotspotSwitchPreference
import com.android.settingslib.TetherUtil
import com.android.settingslib.Utils
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen
class TetherScreen :
    PreferenceScreenCreator, PreferenceAvailabilityProvider, PreferenceRestrictionMixin {

    override val key: String
        get() = KEY

    override val icon: Int
        get() = R.drawable.ic_wifi_tethering

    override val keywords: Int
        get() = R.string.keywords_hotspot_tethering

    override fun getPreferenceTitle(context: Context): CharSequence? =
        if (TetherPreferenceController.isTetherConfigDisallowed(context)) {
            context.getText(R.string.tether_settings_title_all)
        } else {
            val tetheringManager = context.getSystemService(TetheringManager::class.java)!!
            context.getText(Utils.getTetheringLabel(tetheringManager))
        }

    override fun isAvailable(context: Context) = TetherUtil.isTetherAvailable(context)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_TETHERING)

    override fun isFlagEnabled(context: Context) = Flags.catalystTetherSettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = TetherSettings::class.java

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(this) {
        +WifiHotspotSwitchPreference(context)
    }

    companion object {
        const val KEY = "tether_settings"
    }
}
