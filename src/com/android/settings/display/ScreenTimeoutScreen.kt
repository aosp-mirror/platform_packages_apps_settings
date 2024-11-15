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

package com.android.settings.display

import android.content.Context
import com.android.settings.R
import com.android.settings.Settings.ScreenTimeoutActivity
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

// TODO(b/368359967): The entry point logic is not yet migrated
@ProvidePreferenceScreen
class ScreenTimeoutScreen : PreferenceScreenCreator {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.screen_timeout

    override fun isFlagEnabled(context: Context) = Flags.catalystScreenTimeout()

    override fun fragmentClass() = ScreenTimeoutSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(this) { +AdaptiveSleepPreference() }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ScreenTimeoutActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "screen_timeout"
    }
}
