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

import androidx.preference.Preference
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceCategoryBinding
import com.android.settingslib.widget.theme.R

/**
 * A [PreferenceCategory] that does not have a title, and hides the space reserved for displaying
 * the title label above the category.
 */
class TitlelessPreferenceGroup(override val key: String) :
    PreferenceCategory(key, title = 0), PreferenceCategoryBinding {

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        preference.layoutResource = R.layout.settingslib_preference_category_no_title
        super.bind(preference, metadata)
    }
}
