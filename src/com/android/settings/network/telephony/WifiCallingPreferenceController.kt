/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network.telephony

import android.content.Context
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.ims.ImsMmTelManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.network.telephony.wificalling.WifiCallingRepository
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preference controller for "Wifi Calling".
 *
 * TODO: Remove the class once Provider Model is always enabled in the future.
 */
open class WifiCallingPreferenceController @JvmOverloads constructor(
    context: Context,
    key: String,
    private val callStateFlowFactory: (subId: Int) -> Flow<Int> = context::callStateFlow,
    private val wifiCallingRepositoryFactory: (subId: Int) -> WifiCallingRepository = { subId ->
        WifiCallingRepository(context, subId)
    },
) : TelephonyBasePreferenceController(context, key) {

    private lateinit var preference: Preference
    private lateinit var callingPreferenceCategoryController: CallingPreferenceCategoryController

    private val resourcesForSub by lazy {
        SubscriptionManager.getResourcesForSubId(mContext, mSubId)
    }

    fun init(
        subId: Int,
        callingPreferenceCategoryController: CallingPreferenceCategoryController,
    ): WifiCallingPreferenceController {
        mSubId = subId
        this.callingPreferenceCategoryController = callingPreferenceCategoryController
        return this
    }

    /**
     * Note: Visibility also controlled by [onViewCreated].
     */
    override fun getAvailabilityStatus(subId: Int) =
        if (SubscriptionManager.isValidSubscriptionId(subId)) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        // Not call super here, to avoid preference.isVisible changed unexpectedly
        preference = screen.findPreference(preferenceKey)!!
        preference.intent?.putExtra(Settings.EXTRA_SUB_ID, mSubId)
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        wifiCallingRepositoryFactory(mSubId).wifiCallingReadyFlow()
            .collectLatestWithLifecycle(viewLifecycleOwner) {
                preference.isVisible = it
                callingPreferenceCategoryController.updateChildVisible(preferenceKey, it)
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                update()
            }
        }

        callStateFlowFactory(mSubId).collectLatestWithLifecycle(viewLifecycleOwner) {
            preference.isEnabled = (it == TelephonyManager.CALL_STATE_IDLE)
        }
    }

    private suspend fun update() {
        val simCallManager = mContext.getSystemService(TelecomManager::class.java)
            ?.getSimCallManagerForSubscription(mSubId)
        if (simCallManager != null) {
            val intent = withContext(Dispatchers.Default) {
                MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext, simCallManager)
            } ?: return // Do nothing in this case since preference is invisible
            val title = withContext(Dispatchers.Default) {
                mContext.packageManager.resolveActivity(intent, 0)
                    ?.loadLabel(mContext.packageManager)
            } ?: return
            preference.intent = intent
            preference.title = title
            preference.summary = null
        } else {
            preference.title = resourcesForSub.getString(R.string.wifi_calling_settings_title)
            preference.summary = withContext(Dispatchers.Default) { getSummaryForWfcMode() }
        }
    }

    private fun getSummaryForWfcMode(): String {
        val resId = when (wifiCallingRepositoryFactory(mSubId).getWiFiCallingMode()) {
            ImsMmTelManager.WIFI_MODE_WIFI_ONLY ->
                com.android.internal.R.string.wfc_mode_wifi_only_summary

            ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED ->
                com.android.internal.R.string.wfc_mode_cellular_preferred_summary

            ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED ->
                com.android.internal.R.string.wfc_mode_wifi_preferred_summary

            else -> com.android.internal.R.string.wifi_calling_off_summary
        }
        return resourcesForSub.getString(resId)
    }
}
