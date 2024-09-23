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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.Build
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen
class FirmwareVersionScreen : PreferenceScreenCreator, PreferenceSummaryProvider {

    override fun isFlagEnabled(context: Context) = Flags.catalystFirmwareVersion()

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.firmware_version

    override fun getSummary(context: Context): CharSequence? =
        Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY

    override val keywords: Int
        get() = R.string.keywords_android_version

    override fun fragmentClass() = FirmwareVersionSettings::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(this) {
            +PreferenceWidget("os_firmware_version", R.string.firmware_version)
            +PreferenceWidget("security_key", R.string.security_patch)
            +PreferenceWidget("module_version", R.string.module_version)
            +BasebandVersionPreference()
            +KernelVersionPreference()
            +SimpleBuildNumberPreference()
        }

    private class PreferenceWidget(override val key: String, override val title: Int) :
        PreferenceMetadata

    companion object {
        const val KEY = "firmware_version"
    }
}
