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

package com.android.settings.accessibility

import android.content.Context
import android.provider.Settings.System.FONT_SCALE
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

internal class TextReadingFontSizePreference : PreferenceMetadata, PreferenceBinding {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.title_font_size

    override val summary: Int
        get() = R.string.short_summary_font_size

    override val keywords: Int
        get() = R.string.keywords_font_size

    override fun createWidget(context: Context) =
        AccessibilitySeekBarPreference(context, /* attrs= */ null).apply {
            setIconStart(R.drawable.ic_remove_24dp)
            setIconStartContentDescription(R.string.font_size_make_smaller_desc)
            setIconEnd(R.drawable.ic_add_24dp)
            setIconEndContentDescription(R.string.font_size_make_larger_desc)
        }

    companion object {
        const val KEY = FONT_SCALE
    }
}
