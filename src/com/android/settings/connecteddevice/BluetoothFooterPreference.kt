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

package com.android.settings.connecteddevice

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.bluetooth.Utils
import com.android.settings.core.SubSettingLauncher
import com.android.settings.location.BluetoothScanningFragment
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.FooterPreference

class BluetoothFooterPreference(private val bluetoothDataStore: BluetoothDataStore) :
    PreferenceMetadata, PreferenceBinding, PreferenceSummaryProvider {

    override val key: String
        get() = KEY

    override fun isIndexable(context: Context) = false

    override fun dependencies(context: Context) = arrayOf(BluetoothPreference.KEY)

    override fun intent(context: Context): Intent? = subSettingLauncher(context).toIntent()

    override fun createWidget(context: Context) = FooterPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
        val bluetoothDisabled = bluetoothDataStore.getBoolean(BluetoothPreference.KEY) != true
        val footerPreference = preference as FooterPreference
        val context = preference.context
        if (bluetoothDisabled && Utils.isBluetoothScanningEnabled(context)) {
            footerPreference.setLearnMoreText(context.getString(R.string.bluetooth_scan_change))
            footerPreference.setLearnMoreAction { subSettingLauncher(context).launch() }
        } else {
            footerPreference.setLearnMoreText("")
            footerPreference.setLearnMoreAction(null)
        }
    }

    private fun subSettingLauncher(context: Context) =
        SubSettingLauncher(context)
            .setDestination(BluetoothScanningFragment::class.java.name)
            .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_FRAGMENT)

    override fun getSummary(context: Context): CharSequence? {
        val bluetoothDisabled = bluetoothDataStore.getBoolean(BluetoothPreference.KEY) != true
        val resId =
            if (bluetoothDisabled && Utils.isBluetoothScanningEnabled(context)) {
                when (isAutoOnFeatureAvailable()) {
                    true -> R.string.bluetooth_scanning_on_info_message_auto_on_available
                    else -> R.string.bluetooth_scanning_on_info_message
                }
            } else {
                when (isAutoOnFeatureAvailable()) {
                    true -> R.string.bluetooth_empty_list_bluetooth_off_auto_on_available
                    else -> R.string.bluetooth_empty_list_bluetooth_off
                }
            }
        return context.getString(resId)
    }

    private fun isAutoOnFeatureAvailable() =
        try {
            bluetoothDataStore.bluetoothAdapter?.isAutoOnSupported == true
        } catch (e: Exception) {
            Log.e(TAG, "isAutoOnSupported failed", e)
            false
        }

    companion object {
        const val KEY = "bluetooth_screen_footer"
        const val TAG = "BluetoothFooterPreference"
    }
}
