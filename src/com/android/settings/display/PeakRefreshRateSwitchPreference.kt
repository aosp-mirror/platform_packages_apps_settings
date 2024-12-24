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
import android.hardware.display.DisplayManager
import android.provider.DeviceConfig
import android.provider.Settings.System.PEAK_REFRESH_RATE
import com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE
import com.android.internal.display.RefreshRateSettingsUtils.findHighestRefreshRateAmongAllDisplays
import com.android.internal.display.RefreshRateSettingsUtils.findHighestRefreshRateForDefaultDisplay
import com.android.server.display.feature.flags.Flags
import com.android.settings.R
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObservableDelegate
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import kotlin.math.roundToInt

// LINT.IfChange
class PeakRefreshRateSwitchPreference :
    SwitchPreference(KEY, R.string.peak_refresh_rate_title),
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    private var propertiesChangedListener: DeviceConfig.OnPropertiesChangedListener? = null

    override fun storage(context: Context): KeyValueStore =
        PeakRefreshRateStore(context, SettingsSystemStore.get(context))

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
        context.resources.getBoolean(R.bool.config_show_smooth_display) &&
            context.peakRefreshRate > DEFAULT_REFRESH_RATE

    override fun getSummary(context: Context): CharSequence? =
        context.getString(R.string.peak_refresh_rate_summary, context.peakRefreshRate.roundToInt())

    override fun onStart(context: PreferenceLifecycleContext) {
        val listener =
            DeviceConfig.OnPropertiesChangedListener {
                // Got notified if any property has been changed in NAMESPACE_DISPLAY_MANAGER. The
                // KEY_PEAK_REFRESH_RATE_DEFAULT value could be added, changed, removed or
                // unchanged.
                // Just force a UI update for any case.
                context.notifyPreferenceChange(KEY)
            }

        propertiesChangedListener = listener

        DeviceConfig.addOnPropertiesChangedListener(
            DeviceConfig.NAMESPACE_DISPLAY_MANAGER,
            HandlerExecutor.main,
            listener,
        )
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        propertiesChangedListener?.let {
            DeviceConfig.removeOnPropertiesChangedListener(it)
            propertiesChangedListener = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class PeakRefreshRateStore(
        private val context: Context,
        private val settingsStore: SettingsStore,
    ) : KeyedObservableDelegate<String>(settingsStore), KeyValueStore {

        override fun contains(key: String) = settingsStore.contains(key)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
            if (key != KEY) return super.getDefaultValue(key, valueType)
            return context.defaultPeakRefreshRate.refreshRateAsBoolean(context) as T
        }

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
            if (key != KEY) return null
            val refreshRate = settingsStore.getFloat(KEY) ?: context.defaultPeakRefreshRate
            return refreshRate.refreshRateAsBoolean(context) as T
        }

        private fun Float.refreshRateAsBoolean(context: Context) =
            this.isInfinite() || roundToInt() == context.peakRefreshRate.roundToInt()

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            when {
                key != KEY -> {}
                value == null -> settingsStore.setFloat(KEY, null)
                else -> {
                    val peakRefreshRate =
                        if (value as Boolean) context.refreshRateIfON() else DEFAULT_REFRESH_RATE
                    settingsStore.setFloat(KEY, peakRefreshRate)
                }
            }

        private fun Context.refreshRateIfON() =
            when {
                Flags.backUpSmoothDisplayAndForcePeakRefreshRate() -> Float.POSITIVE_INFINITY
                else -> peakRefreshRate
            }
    }

    companion object {
        const val KEY = PEAK_REFRESH_RATE
        private const val INVALIDATE_REFRESH_RATE: Float = -1f

        private val Context.peakRefreshRate: Float
            get() =
                Math.round(
                        when {
                            Flags.backUpSmoothDisplayAndForcePeakRefreshRate() ->
                                findHighestRefreshRateAmongAllDisplays(this)
                            else -> findHighestRefreshRateForDefaultDisplay(this)
                        }
                    )
                    .toFloat()

        private val Context.defaultPeakRefreshRate: Float
            get() {
                val defaultPeakRefreshRate =
                    DeviceConfig.getFloat(
                        DeviceConfig.NAMESPACE_DISPLAY_MANAGER,
                        DisplayManager.DeviceConfig.KEY_PEAK_REFRESH_RATE_DEFAULT,
                        INVALIDATE_REFRESH_RATE,
                    )
                if (defaultPeakRefreshRate != INVALIDATE_REFRESH_RATE) return defaultPeakRefreshRate
                return resources
                    .getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate)
                    .toFloat()
            }
    }
}
// LINT.ThenChange(PeakRefreshRatePreferenceController.java)
