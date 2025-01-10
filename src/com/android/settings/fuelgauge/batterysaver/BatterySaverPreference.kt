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
package com.android.settings.fuelgauge.batterysaver

import android.Manifest
import android.content.Context
import android.os.PowerManager
import com.android.settings.R
import com.android.settings.fuelgauge.BatterySaverReceiver
import com.android.settings.fuelgauge.BatterySaverReceiver.BatterySaverListener
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.fuelgauge.BatterySaverLogging.SAVER_ENABLED_SETTINGS
import com.android.settingslib.fuelgauge.BatterySaverUtils
import com.android.settingslib.fuelgauge.BatteryStatus
import com.android.settingslib.fuelgauge.BatteryUtils
import com.android.settingslib.metadata.MainSwitchPreference
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// LINT.IfChange
class BatterySaverPreference :
    MainSwitchPreference(KEY, R.string.battery_saver_master_switch_title) {

    override fun storage(context: Context) = BatterySaverStore(context)

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermissions(context: Context) =
        Permissions.anyOf(Manifest.permission.DEVICE_POWER, Manifest.permission.POWER_SAVER)

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

    override fun isEnabled(context: Context) =
        !BatteryStatus(BatteryUtils.getBatteryIntent(context)).isPluggedIn

    @Suppress("UNCHECKED_CAST")
    class BatterySaverStore(private val context: Context) :
        AbstractKeyedDataObservable<String>(), KeyValueStore, BatterySaverListener {
        private lateinit var batterySaverReceiver: BatterySaverReceiver
        private lateinit var scope: CoroutineScope

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            context.isPowerSaveMode() as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            BatterySaverUtils.setPowerSaveMode(
                context,
                value as Boolean,
                /* needFirstTimeWarning= */ false,
                SAVER_ENABLED_SETTINGS,
            )
        }

        private fun Context.isPowerSaveMode() =
            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

        override fun onFirstObserverAdded() {
            scope = CoroutineScope(Dispatchers.Main)
            batterySaverReceiver =
                BatterySaverReceiver(context).apply {
                    setBatterySaverListener(this@BatterySaverStore)
                    setListening(true)
                }
        }

        override fun onLastObserverRemoved() {
            scope.cancel()
            batterySaverReceiver.setListening(false)
        }

        override fun onPowerSaveModeChanged() {
            scope.launch {
                delay(SWITCH_ANIMATION_DURATION)
                notifyChange(KEY, PreferenceChangeReason.VALUE)
            }
        }

        override fun onBatteryChanged(pluggedIn: Boolean) =
            notifyChange(KEY, PreferenceChangeReason.STATE)
    }

    companion object {
        private const val KEY = "battery_saver"
        private const val SWITCH_ANIMATION_DURATION: Long = 350L
    }
}
// LINT.ThenChange(BatterySaverButtonPreferenceController.java)
