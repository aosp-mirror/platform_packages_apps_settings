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
import android.hardware.display.AmbientDisplayConfiguration
import android.os.SystemProperties
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController.isAodSuppressedByBedtime
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class AmbientDisplayAlwaysOnPreference :
    SwitchPreference(KEY, R.string.doze_always_on_title, R.string.doze_always_on_summary),
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceRestrictionMixin {

    override val keywords: Int
        get() = R.string.keywords_always_show_time_info

    override val restrictionKeys: Array<String>
        get() = arrayOf(UserManager.DISALLOW_AMBIENT_DISPLAY)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun isAvailable(context: Context) =
        !SystemProperties.getBoolean(PROP_AWARE_AVAILABLE, false) &&
            AmbientDisplayConfiguration(context).alwaysOnAvailableForUser(UserHandle.myUserId())

    override fun getSummary(context: Context): CharSequence? =
        context.getText(
            when {
                isAodSuppressedByBedtime(context) -> R.string.aware_summary_when_bedtime_on
                else -> R.string.doze_always_on_summary
            }
        )

    override fun storage(context: Context): KeyValueStore = Storage(context)

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

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

    /**
     * Datastore of the preference.
     *
     * The preference key and underlying storage key are the different, leverage
     * [AbstractKeyedDataObservable] to redirect data change event.
     */
    @Suppress("UNCHECKED_CAST")
    class Storage(
        private val context: Context,
        private val settingsStore: SettingsStore = SettingsSecureStore.get(context),
    ) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

        override fun contains(key: String) = settingsStore.contains(DOZE_ALWAYS_ON)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
            context.resources.getBoolean(com.android.internal.R.bool.config_dozeAlwaysOnEnabled)
                as T

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            settingsStore.getValue(DOZE_ALWAYS_ON, valueType) ?: getDefaultValue(key, valueType)

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            settingsStore.setValue(DOZE_ALWAYS_ON, valueType, value)

        override fun onFirstObserverAdded() {
            // observe the underlying storage key
            settingsStore.addObserver(DOZE_ALWAYS_ON, this, HandlerExecutor.main)
        }

        override fun onKeyChanged(key: String, reason: Int) {
            // forward data change to preference hierarchy key
            notifyChange(KEY, reason)
        }

        override fun onLastObserverRemoved() {
            settingsStore.removeObserver(DOZE_ALWAYS_ON, this)
        }
    }

    companion object {
        const val KEY = "ambient_display_always_on"
        private const val PROP_AWARE_AVAILABLE = "ro.vendor.aware_available"
    }
}
// LINT.ThenChange(AmbientDisplayAlwaysOnPreferenceController.java)
