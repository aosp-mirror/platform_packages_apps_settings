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

package com.android.settings.wifi.calling

import android.Manifest.permission.MODIFY_PHONE_STATE
import android.Manifest.permission.READ_PRECISE_PHONE_STATE
import android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.ims.ImsMmTelManager
import android.util.Log
import com.android.settings.R
import com.android.settings.network.ims.WifiCallingQueryImsState
import com.android.settings.network.telephony.wificalling.WifiCallingRepository
import com.android.settings.widget.SettingsMainSwitchPreference
import com.android.settings.wifi.calling.WifiCallingSettingsForSub.getCarrierActivityIntent
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.and
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Metadata of the "Use Wi-Fi calling" preference.
 *
 * TODO(b/372732219): apply metadata to UI
 */
class WifiCallingMainSwitchPreference(private val subId: Int) :
    BooleanValuePreference, BooleanValuePreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.wifi_calling_main_switch_title

    override fun isEnabled(context: Context) =
        context.isCallStateIdle(subId) &&
            WifiCallingQueryImsState(context, subId).isAllowUserControl

    override fun isAvailable(context: Context) =
        SubscriptionManager.isValidSubscriptionId(subId) &&
            runBlocking { WifiCallingRepository(context, subId).wifiCallingReadyFlow().first() }

    override fun createWidget(context: Context) = SettingsMainSwitchPreference(context)

    override fun getReadPermissions(context: Context) =
        Permissions.anyOf(READ_PRIVILEGED_PHONE_STATE, READ_PRECISE_PHONE_STATE)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) =
        Permissions.anyOf(READ_PRIVILEGED_PHONE_STATE, READ_PRECISE_PHONE_STATE) and
            MODIFY_PHONE_STATE

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when {
            value == true &&
                (DisclaimerItemFactory.create(context, subId).isNotEmpty() ||
                    getCarrierActivityIntent(context, subId) != null) ->
                ReadWritePermit.REQUIRE_USER_AGREEMENT
            else -> ReadWritePermit.ALLOW
        }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun storage(context: Context): KeyValueStore = WifiCallingStore(context, subId)

    @Suppress("UNCHECKED_CAST")
    private class WifiCallingStore(context: Context, private val subId: Int) :
        NoOpKeyedObservable<String>(), KeyValueStore {
        private val queryIms = WifiCallingQueryImsState(context, subId)

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (queryIms.isEnabledByUser && queryIms.isAllowUserControl) as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value is Boolean) {
                try {
                    ImsMmTelManager.createForSubscriptionId(subId).isVoWiFiSettingEnabled = value
                } catch (e: Exception) {
                    Log.w(TAG, "fail to enable wifi calling", e)
                }
            }
        }
    }

    companion object {
        // TODO(b/372732219): The key is different from XML to avoid applying metadata to UI.
        const val KEY = "wifi_calling_switch"
        const val TAG = KEY

        private fun Context.isCallStateIdle(subId: Int) =
            getSystemService(TelephonyManager::class.java)?.getCallState(subId) ==
                TelephonyManager.CALL_STATE_IDLE
    }
}
