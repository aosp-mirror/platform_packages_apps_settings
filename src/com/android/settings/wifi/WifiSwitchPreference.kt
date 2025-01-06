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

package com.android.settings.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.UserManager
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.network.SatelliteRepository.Companion.isSatelliteOn
import com.android.settings.network.SatelliteWarningDialogActivity
import com.android.settings.wifi.utils.isWifiEnabled
import com.android.settings.wifi.utils.wifiManager
import com.android.settingslib.RestrictedSwitchPreference
import com.android.settingslib.WirelessUtils
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

// LINT.IfChange
class WifiSwitchPreference :
    SwitchPreference(KEY, R.string.wifi),
    SwitchPreferenceBinding,
    OnPreferenceChangeListener,
    PreferenceLifecycleProvider,
    PreferenceRestrictionMixin {

    override val keywords: Int
        get() = R.string.keywords_wifi

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CHANGE_WIFI_STATE)

    override val useAdminDisabledSummary: Boolean
        get() = true

    override fun createWidget(context: Context) = RestrictedSwitchPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val context = preference.context

        // Show dialog and do nothing under satellite mode.
        if (isSatelliteOn(context)) {
            context.startActivity(
                Intent(context, SatelliteWarningDialogActivity::class.java)
                    .putExtra(
                        SatelliteWarningDialogActivity.EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG,
                        SatelliteWarningDialogActivity.TYPE_IS_WIFI,
                    )
            )
            return false
        }

        // Show toast message if Wi-Fi is not allowed in airplane mode
        if (newValue == true && !context.isRadioAllowed()) {
            Toast.makeText(context, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun getReadPermissions(context: Context) =
        Permissions.allOf(Manifest.permission.ACCESS_WIFI_STATE)

    override fun getWritePermissions(context: Context) =
        Permissions.anyOf(
            Manifest.permission.NETWORK_SETTINGS,
            Manifest.permission.CHANGE_WIFI_STATE,
        )

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when {
            (value == true && !context.isRadioAllowed()) || isSatelliteOn(context) ->
                ReadWritePermit.DISALLOW
            else -> ReadWritePermit.ALLOW
        }

    override val sensitivityLevel
        get() = SensitivityLevel.LOW_SENSITIVITY

    override fun storage(context: Context): KeyValueStore = WifiSwitchStore(context)

    @Suppress("UNCHECKED_CAST")
    private class WifiSwitchStore(private val context: Context) :
        AbstractKeyedDataObservable<String>(), KeyValueStore {

        private var broadcastReceiver: BroadcastReceiver? = null

        override fun contains(key: String) = key == KEY && context.wifiManager != null

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
            context.isWifiEnabled as T?

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value is Boolean) {
                context.isWifiEnabled = value
            }
        }

        override fun onFirstObserverAdded() {
            broadcastReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val wifiState = intent.wifiState
                        // do not notify for enabling/disabling state
                        if (
                            wifiState == WifiManager.WIFI_STATE_ENABLED ||
                                wifiState == WifiManager.WIFI_STATE_DISABLED
                        ) {
                            notifyChange(KEY, DataChangeReason.UPDATE)
                        }
                    }
                }
            context.registerReceiver(
                broadcastReceiver,
                IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION),
            )
        }

        override fun onLastObserverRemoved() {
            broadcastReceiver?.let { context.unregisterReceiver(it) }
        }
    }

    companion object {
        const val KEY = "main_toggle_wifi"

        private fun Context.isRadioAllowed() =
            WirelessUtils.isRadioAllowed(this, Settings.Global.RADIO_WIFI)

        private val Intent.wifiState
            get() = getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
    }
}
// LINT.ThenChange(WifiSwitchPreferenceController.java)
