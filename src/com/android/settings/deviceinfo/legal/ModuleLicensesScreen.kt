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
package com.android.settings.deviceinfo.legal

import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

// LINT.IfChange
@ProvidePreferenceScreen(ModuleLicensesScreen.KEY)
class ModuleLicensesScreen : PreferenceScreenCreator, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.module_license_title

    // We need to avoid directly assign fragment attribute in the bind() API. So we need to create
    // a screen and provide it to its parent screen LegalSettingsScreen.
    // By the way, we also need to set the isFlagEnabled() as false. Let system render the legacy
    // UI. The hierarchy will be added while migrating this page.
    override fun isFlagEnabled(context: Context) = false

    override fun fragmentClass() = ModuleLicensesDashboard::class.java

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {}

    override fun isAvailable(context: Context): Boolean {
        val modules = context.packageManager.getInstalledModules(/* flags= */ 0)
        return modules.any {
            try {
                ModuleLicenseProvider.getPackageAssetManager(context.packageManager, it.packageName)
                    .list("")
                    ?.contains(ModuleLicenseProvider.GZIPPED_LICENSE_FILE_NAME) == true
            } catch (e: Exception) {
                false
            }
        }
    }

    companion object {
        const val KEY = "module_license"
    }
}
// LINT.ThenChange(ModuleLicensesListPreferenceController.java)
