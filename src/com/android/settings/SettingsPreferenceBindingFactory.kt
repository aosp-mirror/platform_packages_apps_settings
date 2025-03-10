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

package com.android.settings

import androidx.preference.Preference
import com.android.settingslib.RestrictedPreferenceHelperProvider
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.preference.DefaultPreferenceBindingFactory
import com.android.settingslib.preference.PreferenceBinding

/** Preference binding factory for settings app. */
class SettingsPreferenceBindingFactory : DefaultPreferenceBindingFactory() {
    override fun bind(
        preference: Preference,
        node: PreferenceHierarchyNode,
        preferenceBinding: PreferenceBinding?,
    ) {
        super.bind(preference, node, preferenceBinding)

        // handle restriction consistently
        val metadata = node.metadata
        if (metadata is PreferenceRestrictionMixin) {
            if (preference is RestrictedPreferenceHelperProvider) {
                preference.getRestrictedPreferenceHelper().apply {
                    useAdminDisabledSummary(metadata.useAdminDisabledSummary)
                    val context = preference.context
                    val restrictionKeys = metadata.restrictionKeys
                    if (!context.hasBaseUserRestriction(restrictionKeys)) {
                        setDisabledByAdmin(context.getRestrictionEnforcedAdmin(restrictionKeys))
                    }
                }
            }
        }
    }
}
