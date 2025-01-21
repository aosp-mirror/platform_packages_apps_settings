/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.supervision

import android.app.supervision.flags.Flags
import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

/**
 * Supervision settings landing page (Settings > Supervision).
 *
 * This screen typically includes three parts:
 * 1. Primary switch to toggle supervision on and off.
 * 2. List of supervision features. Individual features like website filters or bedtime schedules
 *    will be listed in a group and link out to their own respective settings pages. Features
 *    implemented by supervision client apps can also be dynamically injected into this group.
 * 3. Entry point to supervision PIN management settings page.
 */
@ProvidePreferenceScreen(SupervisionDashboardScreen.KEY)
class SupervisionDashboardScreen : PreferenceScreenCreator {

    override fun isFlagEnabled(context: Context) = Flags.enableSupervisionSettingsScreen()

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_settings_title

    override val summary: Int
        get() = R.string.supervision_settings_summary

    override val icon: Int
        get() = R.drawable.ic_account_child_invert

    override val keywords: Int
        get() = R.string.keywords_supervision_settings

    override fun fragmentClass() = SupervisionDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +SupervisionMainSwitchPreference()
            +TitlelessPreferenceGroup("supervision_features_group_1") += {
                // Empty category for dynamic injection targeting.
            }
        }

    companion object {
        const val KEY = "top_level_supervision"
    }
}
