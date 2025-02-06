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

package com.android.settings.widget

import android.content.Context
import androidx.preference.Preference
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBindingPlaceholder
import com.android.settingslib.preference.BooleanValuePreferenceBinding

/** Base metadata of `MainSwitchBar`. */
interface MainSwitchBarMetadata :
    BooleanValuePreference, BooleanValuePreferenceBinding, PreferenceBindingPlaceholder {

    /**
     * Disable main switch bar when user toggles the switch. By this way, user cannot toggle again
     * before last operation is done.
     *
     * Side-effect: If the underlying operation is completed quickly, this will causes UI flicker.
     * Override and return `false` to get rid of the flicker.
     */
    val disableWidgetOnCheckedChanged: Boolean
        get() = true

    override fun createWidget(context: Context) = MainSwitchBarPreference(context, this)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as MainSwitchBarPreference).updateVisibility()
    }
}
