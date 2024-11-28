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

package com.android.settings.fuelgauge

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT
import com.android.settingslib.Utils
import com.android.settingslib.fuelgauge.BatteryUtils
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.UsageProgressBarPreference

// LINT.IfChange
class BatteryHeaderPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider {

    @VisibleForTesting
    var batteryBroadcastReceiver: BatteryBroadcastReceiver? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.summary_placeholder

    override fun createWidget(context: Context) = UsageProgressBarPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
        if (preference is UsageProgressBarPreference) {
            quickUpdateHeaderPreference(preference)
        }
    }

    override fun isIndexable(context: Context) = false

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        batteryBroadcastReceiver = BatteryBroadcastReceiver(context).apply {
            setBatteryChangedListener {
                if (it != BATTERY_NOT_PRESENT) {
                    context.notifyPreferenceChange(KEY)
                }
            }
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        batteryBroadcastReceiver?.register();
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        batteryBroadcastReceiver?.unRegister();
    }

    companion object {
        private const val KEY = "battery_header"
        private const val BATTERY_MAX_LEVEL: Long = 100L

        private fun quickUpdateHeaderPreference(preference: UsageProgressBarPreference) {
            val batteryIntent = BatteryUtils.getBatteryIntent(preference.context) ?: return
            val batteryLevel: Int = Utils.getBatteryLevel(batteryIntent)
            preference.apply {
                setUsageSummary(com.android.settings.Utils.formatPercentage(batteryLevel))
                setPercent(batteryLevel.toLong(), BATTERY_MAX_LEVEL)
                setBottomSummary("")
            }
        }
    }
}
// LINT.ThenChange(BatteryHeaderPreferenceController.java)
