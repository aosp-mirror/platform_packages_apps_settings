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

package com.android.settings.display.darkmode

import android.Manifest
import android.content.Context
import android.os.PowerManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.BooleanValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenBinding
import com.android.settingslib.preference.PreferenceScreenCreator

// LINT.IfChange
@ProvidePreferenceScreen
class DarkModeScreen(context: Context) :
    PreferenceScreenCreator,
    PreferenceScreenBinding,
    PersistentPreference<Boolean>,
    BooleanValue,
    PreferenceSummaryProvider {

    private val darkModeStorage = DarkModeStorage(context)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.dark_ui_mode

    override val keywords: Int
        get() = R.string.keywords_dark_ui_mode

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(Manifest.permission.MODIFY_DAY_NIGHT_MODE)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun isFlagEnabled(context: Context) = Flags.catalystDarkUiMode()

    override fun fragmentClass() = DarkModeSettingsFragment::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(this) {}

    override fun storage(context: Context): KeyValueStore = darkModeStorage

    override fun createWidget(context: Context) = PrimarySwitchPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is DarkModePreference) preference.setCatalystEnabled(true)
        (preference as PrimarySwitchPreference).apply {
            isSwitchEnabled = isEnabled()
            isChecked = darkModeStorage.getBoolean(KEY) == true
        }
    }

    override fun isEnabled(context: Context) = !context.isPowerSaveMode()

    override fun getSummary(context: Context): CharSequence? {
        val active = darkModeStorage.getBoolean(KEY) == true
        return when {
            !context.isPowerSaveMode() -> AutoDarkTheme.getStatus(context, active)
            active -> context.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_on)
            else -> context.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_off)
        }
    }

    companion object {
        const val KEY = "dark_ui_mode"

        private fun Context.isPowerSaveMode() =
            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
}
// LINT.ThenChange(../DarkUIPreferenceController.java)
