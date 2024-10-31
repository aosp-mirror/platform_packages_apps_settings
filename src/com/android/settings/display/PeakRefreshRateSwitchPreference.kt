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
import android.util.Log
import com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE
import com.android.internal.display.RefreshRateSettingsUtils.findHighestRefreshRateAmongAllDisplays
import com.android.internal.display.RefreshRateSettingsUtils.findHighestRefreshRateForDefaultDisplay
import com.android.server.display.feature.flags.Flags
import com.android.settings.R
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding
import kotlin.math.roundToInt

// LINT.IfChange
class PeakRefreshRateSwitchPreference :
    SwitchPreference("peak_refresh_rate", R.string.peak_refresh_rate_title),
    SwitchPreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    private var propertiesChangedListener: DeviceConfig.OnPropertiesChangedListener? = null

    override fun storage(context: Context) = SettingsSystemStore.get(context)

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_smooth_display) &&
            (getPeakRefreshRate(context) > DEFAULT_REFRESH_RATE)

    override fun getSummary(context: Context) =
        context.getString(
            R.string.peak_refresh_rate_summary,
            getPeakRefreshRate(context).roundToInt(),
        )

    override fun onStart(context: PreferenceLifecycleContext) {
        val listener =
            object : DeviceConfig.OnPropertiesChangedListener {
                // Got notified if any property has been changed in NAMESPACE_DISPLAY_MANAGER. The
                // KEY_PEAK_REFRESH_RATE_DEFAULT value could be added, changed, removed or
                // unchanged.
                // Just force a UI update for any case.
                override fun onPropertiesChanged(properties: DeviceConfig.Properties) =
                    context.notifyPreferenceChange(this@PeakRefreshRateSwitchPreference)
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

    private fun getPeakRefreshRate(context: Context): Float =
        Math.round(
                when {
                    Flags.backUpSmoothDisplayAndForcePeakRefreshRate() ->
                        findHighestRefreshRateAmongAllDisplays(context)
                    else -> findHighestRefreshRateForDefaultDisplay(context)
                }
            )
            .toFloat()

    private fun getDefaultPeakRefreshRate(context: Context): Float {
        var defaultPeakRefreshRate =
            DeviceConfig.getFloat(
                DeviceConfig.NAMESPACE_DISPLAY_MANAGER,
                DisplayManager.DeviceConfig.KEY_PEAK_REFRESH_RATE_DEFAULT,
                INVALIDATE_REFRESH_RATE,
            )

        if (defaultPeakRefreshRate == INVALIDATE_REFRESH_RATE) {
            defaultPeakRefreshRate =
                context.resources
                    .getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate)
                    .toFloat()
        }

        Log.d(TAG, "DeviceConfig getDefaultPeakRefreshRate : $defaultPeakRefreshRate")
        return defaultPeakRefreshRate
    }

    companion object {
        private const val TAG: String = "PeakRefreshRateSwitchPreference"
        private const val INVALIDATE_REFRESH_RATE: Float = -1f
    }
}
// LINT.ThenChange(PeakRefreshRatePreferenceController.java)
