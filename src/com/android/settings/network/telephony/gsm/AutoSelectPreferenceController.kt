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

package com.android.settings.network.telephony.gsm

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.Settings.NetworkSelectActivity
import com.android.settings.network.CarrierConfigCache
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.network.telephony.allowedNetworkTypesFlow
import com.android.settings.network.telephony.serviceStateFlow
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlin.properties.Delegates.notNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preference controller for "Auto Select Network"
 */
class AutoSelectPreferenceController @JvmOverloads constructor(
    context: Context,
    key: String,
    private val allowedNetworkTypesFlowFactory: (subId: Int) -> Flow<Long> =
        context::allowedNetworkTypesFlow,
    private val serviceStateFlowFactory: (subId: Int) -> Flow<ServiceState> =
        context::serviceStateFlow,
    private val getConfigForSubId: (subId: Int) -> PersistableBundle = { subId ->
        CarrierConfigCache.getInstance(context).getConfigForSubId(subId)
    },
) : ComposePreferenceController(context, key) {

    private lateinit var telephonyManager: TelephonyManager
    private val listeners = mutableListOf<OnNetworkSelectModeListener>()

    @VisibleForTesting
    var progressDialog: ProgressDialog? = null

    private var subId by notNull<Int>()

    /**
     * Initialization based on given subscription id.
     */
    fun init(subId: Int): AutoSelectPreferenceController {
        this.subId = subId
        telephonyManager = mContext.getSystemService(TelephonyManager::class.java)!!
            .createForSubscriptionId(subId)

        return this
    }

    override fun getAvailabilityStatus() =
        if (MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, subId)) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    @Composable
    override fun Content() {
        val coroutineScope = rememberCoroutineScope()
        val serviceStateFlow = remember {
            serviceStateFlowFactory(subId)
                .stateIn(coroutineScope, SharingStarted.Lazily, null)
                .filterNotNull()
        }
        val isAutoOverridableFlow = remember {
            OverridableFlow(serviceStateFlow.map { !it.isManualSelection })
        }
        val isAuto by isAutoOverridableFlow.flow
            .onEach(::updateListenerValue)
            .collectAsStateWithLifecycle(initialValue = null)
        val disallowedSummary by serviceStateFlow.map(::getDisallowedSummary)
            .collectAsStateWithLifecycle(initialValue = "")
        SwitchPreference(object : SwitchPreferenceModel {
            override val title = stringResource(R.string.select_automatically)
            override val summary = { disallowedSummary }
            override val changeable = { disallowedSummary.isEmpty() }
            override val checked = { isAuto }
            override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                if (newChecked) {
                    coroutineScope.launch { setAutomaticSelectionMode(isAutoOverridableFlow) }
                } else {
                    mContext.startActivity(Intent().apply {
                        setClass(mContext, NetworkSelectActivity::class.java)
                        putExtra(Settings.EXTRA_SUB_ID, subId)
                    })
                }
            }
        })
    }

    private suspend fun getDisallowedSummary(serviceState: ServiceState): String =
        withContext(Dispatchers.Default) {
            if (!serviceState.roaming && onlyAutoSelectInHome()) {
                mContext.getString(
                    R.string.manual_mode_disallowed_summary,
                    telephonyManager.simOperatorName
                )
            } else ""
        }

    private fun onlyAutoSelectInHome(): Boolean =
        getConfigForSubId(subId)
            .getBoolean(CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL)

    private suspend fun setAutomaticSelectionMode(overrideChannel: OverridableFlow<Boolean>) {
        showAutoSelectProgressBar()

        withContext(Dispatchers.Default) {
            val minimumDialogTimeDeferred = async { delay(MINIMUM_DIALOG_TIME) }
            telephonyManager.setNetworkSelectionModeAutomatic()
            minimumDialogTimeDeferred.await()
        }
        overrideChannel.override(true)

        dismissProgressBar()
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        allowedNetworkTypesFlowFactory(subId).collectLatestWithLifecycle(viewLifecycleOwner) {
            preference.isVisible = withContext(Dispatchers.Default) {
                MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, subId)
            }
        }
    }

    fun addListener(listener: OnNetworkSelectModeListener): AutoSelectPreferenceController {
        listeners.add(listener)
        return this
    }

    private fun updateListenerValue(isAuto: Boolean) {
        for (listener in listeners) {
            listener.onNetworkSelectModeUpdated(
                if (isAuto) TelephonyManager.NETWORK_SELECTION_MODE_AUTO
                else TelephonyManager.NETWORK_SELECTION_MODE_MANUAL
            )
        }
    }

    private fun showAutoSelectProgressBar() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(mContext).apply {
                setMessage(mContext.resources.getString(R.string.register_automatically))
                setCanceledOnTouchOutside(false)
                setCancelable(false)
                isIndeterminate = true
            }
        }
        progressDialog?.show()
    }

    private fun dismissProgressBar() {
        if (progressDialog?.isShowing == true) {
            try {
                progressDialog?.dismiss()
            } catch (e: IllegalArgumentException) {
                // Ignore exception since the dialog will be gone anyway.
            }
        }
    }

    /**
     * Callback when network select mode might get updated
     *
     * @see TelephonyManager.getNetworkSelectionMode
     */
    interface OnNetworkSelectModeListener {
        fun onNetworkSelectModeUpdated(mode: Int)
    }

    companion object {
        private val MINIMUM_DIALOG_TIME = 1.seconds
    }
}
