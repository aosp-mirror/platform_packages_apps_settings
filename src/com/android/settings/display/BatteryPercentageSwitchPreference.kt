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

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObservableDelegate
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

// LINT.IfChange
class BatteryPercentageSwitchPreference :
    SwitchPreference(KEY, R.string.battery_percentage, R.string.battery_percentage_description),
    SwitchPreferenceBinding,
    PreferenceAvailabilityProvider,
    Preference.OnPreferenceChangeListener {

    override fun storage(context: Context): KeyValueStore =
        BatteryPercentageStorage(context, SettingsSystemStore.get(context))

    override fun isAvailable(context: Context): Boolean =
        Utils.isBatteryPresent(context) &&
            context.resources.getBoolean(
                com.android.internal.R.bool.config_battery_percentage_setting_available
            )

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val showPercentage = newValue as Boolean

        featureFactory.metricsFeatureProvider.action(
            preference.context,
            SettingsEnums.OPEN_BATTERY_PERCENTAGE,
            showPercentage,
        )
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private class BatteryPercentageStorage(
        private val context: Context,
        private val settingsStore: SettingsStore,
    ) : KeyedObservableDelegate<String>(settingsStore), KeyValueStore {

        override fun contains(key: String) = settingsStore.contains(KEY)

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (settingsStore.getBoolean(key) ?: getDefaultValue(key, valueType)) as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            settingsStore.setBoolean(key, value as Boolean)
        }

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
            context.resources.getBoolean(
                com.android.internal.R.bool.config_defaultBatteryPercentageSetting
            ) as T
    }

    companion object {
        const val KEY = Settings.System.SHOW_BATTERY_PERCENT
    }
}
// LINT.ThenChange(BatteryPercentagePreferenceController.java)
