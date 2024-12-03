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

import android.content.Context
import android.net.wifi.WifiManager
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import androidx.preference.Preference
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.network.SatelliteRepository
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.widget.GenericSwitchController
import com.android.settingslib.RestrictedSwitchPreference
import com.android.settingslib.WirelessUtils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.*
import com.android.settingslib.preference.SwitchPreferenceBinding
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// LINT.IfChange
class WifiSwitchPreference :
    SwitchPreference(KEY, R.string.wifi),
    SwitchPreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceRestrictionMixin {

    // TODO(b/372733639) Remove WifiEnabler and migrate to catalyst
    private var wifiEnabler: WifiEnabler? = null

    override val keywords: Int
        get() = R.string.keywords_wifi

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CHANGE_WIFI_STATE)

    override val useAdminDisabledSummary: Boolean
        get() = true

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Boolean?, myUid: Int, callingUid: Int) =
        when {
            isRadioAllowed(context, value) && !isSatelliteOn(context) -> ReadWritePermit.ALLOW
            else -> ReadWritePermit.DISALLOW
        }

    override fun storage(context: Context): KeyValueStore = WifiSwitchStore(context)

    @Suppress("UNCHECKED_CAST")
    private class WifiSwitchStore(private val context: Context) :
        NoOpKeyedObservable<String>(),
        KeyValueStore {

        override fun contains(key: String) =
            key == KEY && context.getSystemService(WifiManager::class.java) != null

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
            context.getSystemService(WifiManager::class.java)?.isWifiEnabled as T?

        @Suppress("DEPRECATION")
        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value is Boolean) {
                context.getSystemService(WifiManager::class.java)?.isWifiEnabled = value
            }
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<RestrictedSwitchPreference>(KEY).let {
            it.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
                    if (!isRadioAllowed(context, newValue as Boolean?)) {
                        Log.w(TAG, "Don't set APM, AIRPLANE_MODE_RADIOS is not allowed")
                        return@OnPreferenceChangeListener false
                    }
                    if (isSatelliteOn(context)) {
                        Log.w(TAG, "Don't set APM, the satellite is on")
                        return@OnPreferenceChangeListener false
                    }
                    return@OnPreferenceChangeListener true
                }
            val widget = GenericSwitchController(it)
            wifiEnabler = WifiEnabler(context, widget, featureFactory.metricsFeatureProvider)
            Log.i(TAG, "Create WifiEnabler:$wifiEnabler")
        }
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        wifiEnabler?.resume(context)
    }

    override fun onPause(context: PreferenceLifecycleContext) {
        wifiEnabler?.pause()
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        wifiEnabler?.teardownSwitchController()
        wifiEnabler = null
    }

    private fun isRadioAllowed(context: Context, newValue: Boolean?): Boolean {
        newValue?.let { if (!it) return true } ?: return false
        return WirelessUtils.isRadioAllowed(context, Settings.Global.RADIO_WIFI)
    }

    private fun isSatelliteOn(context: Context): Boolean {
        try {
            return SatelliteRepository(context)
                .requestIsSessionStarted(Executors.newSingleThreadExecutor())
                .get(2000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Error to get satellite status : $e")
        }
        return false
    }

    companion object {
        const val TAG = "WifiSwitchPreference"
        const val KEY = "main_toggle_wifi"
    }
}
// LINT.ThenChange(WifiSwitchPreferenceController.java)
