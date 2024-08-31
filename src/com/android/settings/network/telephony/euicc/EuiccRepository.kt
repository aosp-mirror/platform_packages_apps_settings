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

package com.android.settings.network.telephony.euicc

import android.content.Context
import android.os.SystemProperties
import android.provider.Settings
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import android.util.Log
import com.android.settings.network.SubscriptionUtil
import com.android.settingslib.development.DevelopmentSettingsEnabler
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class EuiccRepository
@JvmOverloads
constructor(
    private val context: Context,
    private val isEuiccProvisioned: () -> Boolean = {
        val euiccProvisioned by context.settingsGlobalBoolean(Settings.Global.EUICC_PROVISIONED)
        euiccProvisioned
    },
    private val isDevelopmentSettingsEnabled: () -> Boolean = {
        DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context)
    },
) {

    private val euiccManager = context.getSystemService(EuiccManager::class.java)
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)

    fun showEuiccSettingsFlow() =
        flow { emit(showEuiccSettings()) }
            .distinctUntilChanged()
            .conflate()
            .flowOn(Dispatchers.Default)

    /**
     * Whether to show the entry point to eUICC settings.
     *
     * We show the entry point on any device which supports eUICC as long as either the eUICC was
     * ever provisioned (that is, at least one profile was ever downloaded onto it), or if the user
     * has enabled development mode.
     */
    fun showEuiccSettings(): Boolean {
        if (!SubscriptionUtil.isSimHardwareVisible(context)) return false
        if (euiccManager == null || !euiccManager.isEnabled) {
            Log.w(TAG, "EuiccManager is not enabled.")
            return false
        }
        if (isEuiccProvisioned()) {
            Log.i(TAG, "showEuiccSettings: euicc provisioned")
            return true
        }
        val ignoredCids =
            SystemProperties.get(KEY_ESIM_CID_IGNORE).split(',').filter { it.isNotEmpty() }
        val cid = SystemProperties.get(KEY_CID)
        if (cid in ignoredCids) {
            Log.i(TAG, "showEuiccSettings: cid ignored")
            return false
        }
        if (isDevelopmentSettingsEnabled()) {
            Log.i(TAG, "showEuiccSettings: development settings enabled")
            return true
        }
        val enabledEsimUiByDefault =
            SystemProperties.getBoolean(KEY_ENABLE_ESIM_UI_BY_DEFAULT, true)
        Log.i(TAG, "showEuiccSettings: enabledEsimUiByDefault=$enabledEsimUiByDefault")
        return enabledEsimUiByDefault && isCurrentCountrySupported()
    }

    /**
     * Loop through all the device logical slots to check whether the user's current country
     * supports eSIM.
     */
    private fun isCurrentCountrySupported(): Boolean {
        val euiccManager = euiccManager ?: return false
        val telephonyManager = telephonyManager ?: return false
        val visitedCountrySet = mutableSetOf<String>()
        for (slotIndex in 0 until telephonyManager.getActiveModemCount()) {
            val countryCode = telephonyManager.getNetworkCountryIso(slotIndex)
            if (
                countryCode.isNotEmpty() &&
                    visitedCountrySet.add(countryCode) &&
                    euiccManager.isSupportedCountry(countryCode)
            ) {
                Log.i(TAG, "isCurrentCountrySupported: $countryCode is supported")
                return true
            }
        }
        Log.i(TAG, "isCurrentCountrySupported: no country is supported")
        return false
    }

    companion object {
        private const val TAG = "EuiccRepository"

        /** CID of the device. */
        private const val KEY_CID: String = "ro.boot.cid"

        /** CIDs of devices which should not show anything related to eSIM. */
        private const val KEY_ESIM_CID_IGNORE: String = "ro.setupwizard.esim_cid_ignore"

        /**
         * System Property which is used to decide whether the default eSIM UI will be shown, the
         * default value is false.
         */
        private const val KEY_ENABLE_ESIM_UI_BY_DEFAULT: String =
            "esim.enable_esim_system_ui_by_default"
    }
}
