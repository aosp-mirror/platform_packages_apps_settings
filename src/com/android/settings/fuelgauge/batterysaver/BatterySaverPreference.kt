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

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.android.settings.R
import com.android.settings.fuelgauge.BatterySaverReceiver
import com.android.settings.fuelgauge.BatterySaverReceiver.BatterySaverListener
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.fuelgauge.BatterySaverLogging.SAVER_ENABLED_SETTINGS
import com.android.settingslib.fuelgauge.BatterySaverUtils
import com.android.settingslib.fuelgauge.BatteryStatus
import com.android.settingslib.fuelgauge.BatteryUtils
import com.android.settingslib.metadata.MainSwitchPreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit

// LINT.IfChange
class BatterySaverPreference :
    MainSwitchPreference(KEY, R.string.battery_saver_master_switch_title),
    PreferenceLifecycleProvider {

    private var batterySaverReceiver: BatterySaverReceiver? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun storage(context: Context) = BatterySaverStore(context)

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Boolean?, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun isEnabled(context: Context) =
        !BatteryStatus(BatteryUtils.getBatteryIntent(context)).isPluggedIn

    override fun onStart(context: PreferenceLifecycleContext) {
        BatterySaverReceiver(context).apply {
            batterySaverReceiver = this
            setBatterySaverListener(
                object : BatterySaverListener {
                    override fun onPowerSaveModeChanged() {
                        handler.postDelayed(
                            { context.notifyPreferenceChange(this@BatterySaverPreference) },
                            SWITCH_ANIMATION_DURATION,
                        )
                    }

                    override fun onBatteryChanged(pluggedIn: Boolean) =
                        context.notifyPreferenceChange(this@BatterySaverPreference)
                }
            )
            setListening(true)
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        batterySaverReceiver?.setListening(false)
        batterySaverReceiver = null
        handler.removeCallbacksAndMessages(null /* token */)
    }

    @Suppress("UNCHECKED_CAST")
    class BatterySaverStore(private val context: Context) :
        NoOpKeyedObservable<String>(), KeyValueStore {
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
    }

    companion object {
        private const val KEY = "battery_saver"
        private const val SWITCH_ANIMATION_DURATION: Long = 350L
    }
}
// LINT.ThenChange(BatterySaverButtonPreferenceController.java)
