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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorPrivacyManager
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener
import android.hardware.SensorPrivacyManager.Sensors.CAMERA
import android.os.PowerManager
import android.os.UserManager
import android.provider.Settings
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settingslib.RestrictedSwitchPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObservableDelegate
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.TwoStatePreference
import com.android.settingslib.preference.PreferenceBindingPlaceholder
import com.android.settingslib.preference.SwitchPreferenceBinding

// LINT.IfChange
class AdaptiveSleepPreference :
    TwoStatePreference,
    SwitchPreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceBindingPlaceholder, // not needed once controller class is cleaned up
    PreferenceAvailabilityProvider,
    PreferenceRestrictionMixin {

    private var broadcastReceiver: BroadcastReceiver? = null
    private var sensorPrivacyChangedListener: OnSensorPrivacyChangedListener? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.adaptive_sleep_title

    override val summary: Int
        get() = R.string.adaptive_sleep_description

    override fun isIndexable(context: Context) = false

    override fun isEnabled(context: Context) =
        super<PreferenceRestrictionMixin>.isEnabled(context) && context.canBeEnabled()

    override val restrictionKeys: Array<String>
        get() = arrayOf(UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT)

    override fun isAvailable(context: Context) = context.isAdaptiveSleepSupported()

    override fun createWidget(context: Context) = RestrictedSwitchPreference(context)

    override fun storage(context: Context): KeyValueStore = Storage(context)

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Boolean?, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    @Suppress("UNCHECKED_CAST")
    private class Storage(
        private val context: Context,
        private val settingsStore: SettingsStore = SettingsSecureStore.get(context),
    ) : KeyedObservableDelegate<String>(settingsStore), KeyValueStore {

        override fun contains(key: String) = settingsStore.contains(key)

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (context.canBeEnabled() && settingsStore.getBoolean(key) == true) as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            settingsStore.setBoolean(key, value as Boolean?)
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    context.notifyPreferenceChange(KEY)
                }
            }
        context.registerReceiver(
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )
        broadcastReceiver = receiver

        val listener = OnSensorPrivacyChangedListener { _, _ ->
            context.notifyPreferenceChange(KEY)
        }
        SensorPrivacyManager.getInstance(context).addSensorPrivacyListener(CAMERA, listener)
        sensorPrivacyChangedListener = listener
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        broadcastReceiver?.let { context.unregisterReceiver(it) }
        sensorPrivacyChangedListener?.let {
            SensorPrivacyManager.getInstance(context).removeSensorPrivacyListener(it)
        }
    }

    companion object {
        const val KEY = Settings.Secure.ADAPTIVE_SLEEP

        @Suppress("DEPRECATION")
        private fun Context.canBeEnabled() =
            AdaptiveSleepPreferenceController.hasSufficientPermission(packageManager) &&
                getSystemService(PowerManager::class.java)?.isPowerSaveMode != true &&
                !SensorPrivacyManager.getInstance(this).isSensorPrivacyEnabled(CAMERA)
    }
}
// LINT.ThenChange(AdaptiveSleepPreferenceController.java)
