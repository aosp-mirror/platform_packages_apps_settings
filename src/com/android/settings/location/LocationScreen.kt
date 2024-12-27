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
package com.android.settings.location

import android.content.Context
import android.location.LocationManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen(LocationScreen.KEY)
class LocationScreen : PreferenceScreenCreator, PreferenceSummaryProvider, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.location_settings_title

    override val keywords: Int
        get() = R.string.keywords_location

    override fun getSummary(context: Context): CharSequence? {
        var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (locationManager.isLocationEnabled) {
            context.getString(R.string.location_settings_loading_app_permission_stats)
        } else {
            context.getString(R.string.location_settings_summary_location_off)
        }
    }

    override fun getIcon(context: Context) =
        when {
            Flags.homepageRevamp() -> R.drawable.ic_settings_location_filled
            else -> R.drawable.ic_settings_location
        }

    override fun isFlagEnabled(context: Context) = Flags.catalystLocationSettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = LocationSettings::class.java

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {}

    companion object {
        const val KEY = "location_settings"
    }
}
