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
import android.text.BidiFormatter
import android.view.View.LAYOUT_DIRECTION_RTL
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class SimpleBuildNumberPreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceBinding {

    override val key: String
        get() = "os_build_number"

    override val title: Int
        get() = R.string.build_number

    override fun isIndexable(context: Context) = false

    override fun getSummary(context: Context): CharSequence? {
        val isRtl = context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL
        return BidiFormatter.getInstance(isRtl).unicodeWrap(Build.DISPLAY)
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
        preference.isCopyingEnabled = true
    }
}
// LINT.ThenChange(SimpleBuildNumberPreferenceController.java)
