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

package com.android.settings.deviceinfo.aboutphone

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen(MyDeviceInfoScreen.KEY)
class MyDeviceInfoScreen :
    PreferenceScreenCreator, PreferenceSummaryProvider, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.about_settings

    override fun getSummary(context: Context): CharSequence? {
        return Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: Build.MODEL
    }

    override fun getIcon(context: Context): Int {
        return when (Flags.homepageRevamp()) {
            true -> R.drawable.ic_settings_about_device_filled
            false -> R.drawable.ic_settings_about_device
        }
    }

    override fun isFlagEnabled(context: Context) = Flags.catalystMyDeviceInfoPrefScreen()

    override fun fragmentClass() = MyDeviceInfoFragment::class.java

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {}

    override fun hasCompleteHierarchy() = false

    companion object {
        const val KEY = "my_device_info_pref_screen"
    }
}
