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
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.BooleanValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceRestrictionProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenBinding
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen
class AutoBrightnessScreen :
    PreferenceScreenCreator,
    PreferenceScreenBinding,
    PreferenceAvailabilityProvider,
    PreferenceRestrictionProvider,
    PersistentPreference<Boolean>,
    BooleanValue {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.auto_brightness_title

    override fun isFlagEnabled(context: Context) = Flags.catalystScreenBrightnessMode()

    override fun fragmentClass() = AutoBrightnessSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(this) {}

    override fun storage(context: Context) = SettingsSystemStore.get(context)

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(
            com.android.internal.R.bool.config_automatic_brightness_available
        )

    override fun isEnabled(context: Context) =
        !UserManager.get(context)
            .hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_BRIGHTNESS, Process.myUserHandle())

    override fun isRestricted(context: Context) =
        RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
            context,
            UserManager.DISALLOW_CONFIG_BRIGHTNESS,
            UserHandle.myUserId(),
        ) != null

    override fun createWidget(context: Context) = PrimarySwitchPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as PrimarySwitchPreference).apply {
            useAdminDisabledSummary(true)
            isSwitchEnabled = isEnabled
            isChecked =
                storage(preference.context).getBoolean(KEY)
                    ?: getDefault(SCREEN_BRIGHTNESS_MODE_MANUAL)
        }
    }

    private fun getDefault(brightnessDefault: Int): Boolean =
        brightnessDefault == SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    companion object {
        const val KEY = Settings.System.SCREEN_BRIGHTNESS_MODE
    }
}
