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

import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

/** Pin Management landing page (Settings > Supervision > Manage Pin). */
@ProvidePreferenceScreen(SupervisionPinManagementScreen.KEY)
class SupervisionPinManagementScreen : PreferenceScreenCreator {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_pin_management_preference_title

    // TODO(b/391994031): dynamically update the summary according to PIN status.
    override val summary: Int
        get() = R.string.supervision_pin_management_preference_summary_add

    // TODO(b/391994031): dynamically update the icon according to PIN status.
    override val icon: Int
        get() = R.drawable.ic_pin

    override fun fragmentClass() = SupervisionPinManagementFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            // TODO(b/391992481) implement the screen.
        }

    companion object {
        const val KEY = "supervision_pin_management"
    }
}
