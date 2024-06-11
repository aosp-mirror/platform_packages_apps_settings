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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.MainSwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.launch

class MobileNetworkSwitchController @JvmOverloads constructor(
    context: Context,
    preferenceKey: String,
    private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
    private val subscriptionActivationRepository: SubscriptionActivationRepository =
        SubscriptionActivationRepository(context),
) : ComposePreferenceController(context, preferenceKey) {

    private var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    override fun getAvailabilityStatus() = AVAILABLE_UNSEARCHABLE

    fun init(subId: Int) {
        this.subId = subId
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        if (remember { !context.isVisible() }) return
        val checked by remember {
            subscriptionRepository.isSubscriptionEnabledFlow(subId)
        }.collectAsStateWithLifecycle(initialValue = null)
        val changeable by remember {
            subscriptionActivationRepository.isActivationChangeableFlow()
        }.collectAsStateWithLifecycle(initialValue = true)
        val coroutineScope = rememberCoroutineScope()
        MainSwitchPreference(model = object : SwitchPreferenceModel {
            override val title = stringResource(R.string.mobile_network_use_sim_on)
            override val changeable = { changeable }
            override val checked = { checked }
            override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                coroutineScope.launch {
                    subscriptionActivationRepository.setActive(subId, newChecked)
                }
            }
        })
    }

    private fun Context.isVisible(): Boolean {
        val subInfo = subscriptionRepository.getSelectableSubscriptionInfoList()
            .firstOrNull { it.subscriptionId == subId }
            ?: return false
        // For eSIM, we always want the toggle. If telephony stack support disabling a pSIM
        // directly, we show the toggle.
        return subInfo.isEmbedded || requireSubscriptionManager().canDisablePhysicalSubscription()
    }
}
