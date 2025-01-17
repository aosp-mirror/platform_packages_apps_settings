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
import android.os.UserManager
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenBinding
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen(AutoBrightnessScreen.KEY)
class AutoBrightnessScreen :
    PreferenceScreenCreator,
    PreferenceScreenBinding, // binding for screen page
    PrimarySwitchPreferenceBinding, // binding for screen entry point widget
    PreferenceAvailabilityProvider,
    PreferenceRestrictionMixin,
    BooleanValuePreference {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.auto_brightness_title

    override fun isFlagEnabled(context: Context) = Flags.catalystScreenBrightnessMode()

    override fun fragmentClass() = AutoBrightnessSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {}

    override fun storage(context: Context): KeyValueStore =
        AutoBrightnessDataStore(SettingsSystemStore.get(context))

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

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

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(
            com.android.internal.R.bool.config_automatic_brightness_available
        )

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_BRIGHTNESS)

    override val useAdminDisabledSummary: Boolean
        get() = true

    override fun bind(preference: Preference, metadata: PreferenceMetadata) =
        when (preference) {
            is PreferenceScreen -> super<PreferenceScreenBinding>.bind(preference, metadata)
            else -> super<PrimarySwitchPreferenceBinding>.bind(preference, metadata)
        }

    /**
     * The datastore for brightness, which is persisted as integer but the external type is boolean.
     */
    @Suppress("UNCHECKED_CAST")
    private class AutoBrightnessDataStore(private val settingsStore: SettingsStore) :
        AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

        override fun contains(key: String) = settingsStore.contains(SCREEN_BRIGHTNESS_MODE)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
            DEFAULT_VALUE.toBoolean() as T

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (settingsStore.getInt(SCREEN_BRIGHTNESS_MODE) ?: DEFAULT_VALUE).toBoolean() as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            settingsStore.setInt(SCREEN_BRIGHTNESS_MODE, (value as? Boolean)?.toBrightnessMode())

        override fun onFirstObserverAdded() {
            // observe the underlying storage key
            settingsStore.addObserver(SCREEN_BRIGHTNESS_MODE, this, HandlerExecutor.main)
        }

        override fun onKeyChanged(key: String, reason: Int) {
            // forward data change to preference hierarchy key
            notifyChange(KEY, reason)
        }

        override fun onLastObserverRemoved() {
            settingsStore.removeObserver(SCREEN_BRIGHTNESS_MODE, this)
        }

        /** Converts brightness mode integer to boolean. */
        private fun Int.toBoolean() = this == SCREEN_BRIGHTNESS_MODE_AUTOMATIC

        /** Converts boolean value to brightness mode integer. */
        private fun Boolean.toBrightnessMode() =
            if (this) SCREEN_BRIGHTNESS_MODE_AUTOMATIC else SCREEN_BRIGHTNESS_MODE_MANUAL
    }

    companion object {
        const val KEY = "auto_brightness_entry"
        private const val DEFAULT_VALUE = SCREEN_BRIGHTNESS_MODE_MANUAL
    }
}
