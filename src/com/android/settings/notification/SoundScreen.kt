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
package com.android.settings.notification

import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.SoundSettingsActivity
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen
class SoundScreen : PreferenceScreenCreator, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.sound_settings

    override val keywords: Int
        get() = R.string.keywords_sounds

    override fun getIcon(context: Context) =
        when {
            Flags.homepageRevamp() -> R.drawable.ic_volume_up_filled
            else -> R.drawable.ic_volume_up_24dp
        }

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystSoundScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = SoundSettings::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(this) {
            +MediaVolumePreference() order -180
            +CallVolumePreference() order -170
            +SeparateRingVolumePreference() order -155
            +DialPadTonePreference() order -50
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SoundSettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "sound_screen"
    }
}
