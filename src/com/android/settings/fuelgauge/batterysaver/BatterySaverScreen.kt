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
package com.android.settings.fuelgauge.batterysaver

import android.content.Context
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen(BatterySaverScreen.KEY)
open class BatterySaverScreen : PreferenceScreenCreator {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.battery_saver

    override val keywords: Int
        get() = R.string.keywords_battery_saver

    override fun isFlagEnabled(context: Context) = Flags.catalystBatterySaverScreen()

    override fun fragmentClass() = BatterySaverSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) { +BatterySaverPreference() order -100 }

    companion object {
        const val KEY = "battery_saver_screen"
    }
}
