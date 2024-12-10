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

package com.android.settings.wifi.tether

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.net.TetheringManager
import android.net.TetheringManager.TETHERING_WIFI
import android.net.wifi.WifiClient
import android.net.wifi.WifiManager
import android.os.UserManager
import android.text.BidiFormatter
import android.util.Log
import androidx.preference.Preference
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.core.SubSettingLauncher
import com.android.settings.datausage.DataSaverMainSwitchPreference.Companion.KEY as DATA_SAVER_KEY
import com.android.settings.wifi.WifiUtils.canShowWifiHotspot
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.TetherUtil
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.wifi.WifiUtils.Companion.getWifiTetherSummaryForConnectedDevices

// LINT.IfChange
@Suppress("MissingPermission", "NewApi", "UNCHECKED_CAST")
class WifiHotspotSwitchPreference(context: Context, dataSaverStore: KeyValueStore) :
    SwitchPreference(KEY, R.string.wifi_hotspot_checkbox_text),
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceRestrictionMixin {

    private val wifiHotspotStore = WifiHotspotStore(context, dataSaverStore)

    override fun isAvailable(context: Context) =
        canShowWifiHotspot(context) &&
            TetherUtil.isTetherAvailable(context) &&
            !Utils.isMonkeyRunning()

    override fun getSummary(context: Context): CharSequence? =
        when (context.wifiManager?.wifiApState) {
            WifiManager.WIFI_AP_STATE_ENABLING -> context.getString(R.string.wifi_tether_starting)
            WifiManager.WIFI_AP_STATE_ENABLED -> {
                val sapClientsSize = wifiHotspotStore.sapClientsSize
                if (sapClientsSize == null) {
                    context.getString(
                        R.string.wifi_tether_enabled_subtext,
                        BidiFormatter.getInstance().unicodeWrap(context.wifiSsid),
                    )
                } else {
                    getWifiTetherSummaryForConnectedDevices(context, sapClientsSize)
                }
            }
            WifiManager.WIFI_AP_STATE_DISABLING -> context.getString(R.string.wifi_tether_stopping)
            WifiManager.WIFI_AP_STATE_DISABLED ->
                context.getString(R.string.wifi_hotspot_off_subtext)
            else ->
                when (wifiHotspotStore.sapFailureReason) {
                    WifiManager.SAP_START_FAILURE_NO_CHANNEL ->
                        context.getString(R.string.wifi_sap_no_channel_error)
                    else -> context.getString(R.string.wifi_error)
                }
        }

    override fun intent(context: Context): Intent? =
        SubSettingLauncher(context)
            .apply {
                setDestination(WifiTetherSettings::class.java.name)
                setTitleRes(R.string.wifi_hotspot_checkbox_text)
                setSourceMetricsCategory(SettingsEnums.WIFI_TETHER_SETTINGS)
            }
            .toIntent()

    override fun isEnabled(context: Context) =
        wifiHotspotStore.dataSaverStore.getBoolean(DATA_SAVER_KEY) != true &&
            super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_WIFI_TETHERING)

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Boolean?, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.HIGH_SENSITIVITY

    override fun createWidget(context: Context) = PrimarySwitchPreference(context)

    override fun storage(context: Context): KeyValueStore = wifiHotspotStore

    private class WifiHotspotStore(
        private val context: Context,
        val dataSaverStore: KeyValueStore,
    ) :
        AbstractKeyedDataObservable<String>(),
        KeyValueStore,
        WifiTetherSoftApManager.WifiTetherSoftApCallback,
        TetheringManager.StartTetheringCallback,
        KeyedObserver<String> {

        private var wifiTetherSoftApManager: WifiTetherSoftApManager? = null
        var sapFailureReason: Int? = null
        var sapClientsSize: Int? = null

        override fun contains(key: String) =
            key == KEY && context.wifiManager != null && context.tetheringManager != null

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
            val wifiApState = context.wifiManager?.wifiApState
            val value =
                wifiApState == WifiManager.WIFI_AP_STATE_ENABLING ||
                    wifiApState == WifiManager.WIFI_AP_STATE_ENABLED
            return value as T?
        }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value !is Boolean) return
            val tetheringManager = context.tetheringManager ?: return
            if (value) {
                tetheringManager.startTethering(TETHERING_WIFI, HandlerExecutor.main, this)
            } else {
                tetheringManager.stopTethering(TETHERING_WIFI)
            }
        }

        override fun onFirstObserverAdded() {
            val apManager = WifiTetherSoftApManager(context.wifiManager, this)
            wifiTetherSoftApManager = apManager
            apManager.registerSoftApCallback()
            dataSaverStore.addObserver(DATA_SAVER_KEY, this, HandlerExecutor.main)
        }

        override fun onLastObserverRemoved() {
            dataSaverStore.removeObserver(DATA_SAVER_KEY, this)
            wifiTetherSoftApManager?.unRegisterSoftApCallback()
        }

        override fun onStateChanged(state: Int, failureReason: Int) {
            Log.d(TAG, "onStateChanged(),state=$state,failureReason=$failureReason")
            sapFailureReason = failureReason
            if (state == WifiManager.WIFI_AP_STATE_DISABLED) sapClientsSize = null
            notifyChange(KEY, DataChangeReason.UPDATE)
        }

        override fun onConnectedClientsChanged(clients: List<WifiClient>?) {
            sapClientsSize = clients?.size ?: 0
            Log.d(TAG, "onConnectedClientsChanged(),sapClientsSize=$sapClientsSize")
            notifyChange(KEY, DataChangeReason.UPDATE)
        }

        override fun onTetheringStarted() {}

        override fun onTetheringFailed(error: Int) {
            Log.e(TAG, "onTetheringFailed(),error=$error")
        }

        override fun onKeyChanged(key: String, reason: Int) =
            notifyChange(KEY, DataChangeReason.UPDATE)
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as PrimarySwitchPreference).apply {
            isChecked = preferenceDataStore!!.getBoolean(key, false)
            isSwitchEnabled = isEnabled
        }
    }

    companion object {
        const val TAG = "WifiHotspotSwitchPreference"
        const val KEY = "wifi_tether"

        private val Context.wifiManager: WifiManager?
            get() = applicationContext.getSystemService(WifiManager::class.java)

        @Suppress("DEPRECATION")
        private val Context.wifiSsid
            get() = wifiManager?.softApConfiguration?.ssid

        private val Context.tetheringManager: TetheringManager?
            get() = applicationContext.getSystemService(TetheringManager::class.java)
    }
}
// LINT.ThenChange(WifiTetherPreferenceController.java)
