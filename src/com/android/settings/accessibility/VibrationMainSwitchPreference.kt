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
package com.android.settings.accessibility

import android.content.Context
import android.os.VibrationAttributes
import android.os.Vibrator
import android.provider.Settings
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObservableDelegate
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.MainSwitchPreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit

/** Accessibility settings for vibration. */
// LINT.IfChange
class VibrationMainSwitchPreference :
    MainSwitchPreference(
        key = Settings.System.VIBRATE_ON,
        title = R.string.accessibility_vibration_primary_switch_title,
    ),
    PreferenceLifecycleProvider,
    OnCheckedChangeListener {
    override val keywords: Int
        get() = R.string.keywords_accessibility_vibration_primary_switch

    lateinit var vibrator: Vibrator

    override fun storage(context: Context): KeyValueStore =
        VibrationMainSwitchToggleStorage(SettingsSystemStore.get(context))

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override fun onResume(context: PreferenceLifecycleContext) {
        vibrator = context.getSystemService(Vibrator::class.java)
        context
            .findPreference<com.android.settingslib.widget.MainSwitchPreference>(key)
            ?.addOnSwitchChangeListener(this)
    }

    override fun onPause(context: PreferenceLifecycleContext) {
        context
            .findPreference<com.android.settingslib.widget.MainSwitchPreference>(key)
            ?.removeOnSwitchChangeListener(this)
    }

    override fun onCheckedChanged(button: CompoundButton, isChecked: Boolean) {
        if (isChecked) {
            // Play a haptic as preview for the main toggle only when touch feedback is enabled.
            VibrationPreferenceConfig.playVibrationPreview(
                vibrator,
                VibrationAttributes.USAGE_TOUCH,
            )
        }
    }

    /** Provides SettingsStore for vibration main switch with custom default value. */
    @Suppress("UNCHECKED_CAST")
    private class VibrationMainSwitchToggleStorage(private val settingsStore: SettingsStore) :
        KeyedObservableDelegate<String>(settingsStore), KeyValueStore {

        override fun contains(key: String) = settingsStore.contains(key)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
            DEFAULT_VALUE as T

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (settingsStore.getBoolean(key) ?: DEFAULT_VALUE) as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            settingsStore.setBoolean(key, value as Boolean?)
        }
    }

    companion object {
        const val DEFAULT_VALUE = true
    }
}
// LINT.ThenChange(VibrationMainSwitchPreferenceController.java)
