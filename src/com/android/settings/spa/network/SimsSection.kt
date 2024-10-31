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

package com.android.settings.spa.network

import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.telephony.SubscriptionInfo
import android.telephony.euicc.EuiccManager
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.SimCardDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.network.telephony.SubscriptionActivationRepository
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settings.network.telephony.euicc.EuiccRepository
import com.android.settings.network.telephony.phoneNumberFlow
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.ui.SettingsIcon
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.template.preference.RestrictedPreference
import com.android.settingslib.spaprivileged.template.preference.RestrictedTwoTargetSwitchPreference
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Composable
fun SimsSection(subscriptionInfoList: List<SubscriptionInfo>) {
    Column {
        for (subInfo in subscriptionInfoList) {
            SimPreference(subInfo)
        }

        AddSim()
    }
}

@Composable
private fun SimPreference(subInfo: SubscriptionInfo) {
    val context = LocalContext.current
    val checked = remember(subInfo.subscriptionId) {
        SubscriptionRepository(context).isSubscriptionEnabledFlow(subInfo.subscriptionId)
    }.collectAsStateWithLifecycle(initialValue = false)
    val phoneNumber = phoneNumber(subInfo)
    val isConvertedPsim by remember(subInfo) {
        flow {
            emit(SubscriptionUtil.isConvertedPsimSubscription(subInfo))
        }
    }.collectAsStateWithLifecycle(initialValue = false)
    val subscriptionActivationRepository = remember { SubscriptionActivationRepository(context) }
    val isActivationChangeable by remember {
        subscriptionActivationRepository.isActivationChangeableFlow()
    }.collectAsStateWithLifecycle(initialValue = false)
    val coroutineScope = rememberCoroutineScope()
    RestrictedTwoTargetSwitchPreference(
        model = object : SwitchPreferenceModel {
            override val title = subInfo.displayName.toString()
            override val summary = {
                if (isConvertedPsim) {
                    context.getString(R.string.sim_category_converted_sim)
                } else {
                    phoneNumber.value ?: ""
                }
            }
            override val icon = @Composable { SimIcon(subInfo.isEmbedded) }
            override val changeable = { isActivationChangeable && !isConvertedPsim }
            override val checked = { checked.value }
            override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                coroutineScope.launch {
                    subscriptionActivationRepository.setActive(subInfo.subscriptionId, newChecked)
                }
            }
        },
        restrictions = Restrictions(keys = listOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)),
        primaryEnabled = { !isConvertedPsim },
    ) {
        MobileNetworkUtils.launchMobileNetworkSettings(context, subInfo)
    }
}

@Composable
private fun SimIcon(isEmbedded: Boolean) {
    SettingsIcon(if (isEmbedded) Icons.Outlined.SimCardDownload else Icons.Outlined.SimCard)
}

@Composable
fun phoneNumber(subInfo: SubscriptionInfo): State<String?> {
    val context = LocalContext.current
    return remember(subInfo) {
        context.phoneNumberFlow(subInfo)
    }.collectAsStateWithLifecycle(initialValue = null)
}

@Composable
private fun AddSim() {
    val context = LocalContext.current
    val isShow by
        remember { EuiccRepository(context).showEuiccSettingsFlow() }
            .collectAsStateWithLifecycle(initialValue = false)
    if (isShow) {
        RestrictedPreference(
            model =
                object : PreferenceModel {
                    override val title = stringResource(id = R.string.mobile_network_list_add_more)
                    override val icon = @Composable { SettingsIcon(Icons.Outlined.Add) }
                    override val onClick = { startAddSimFlow(context) }
                },
            restrictions = Restrictions(keys = listOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)),
        )
    }
}

fun startAddSimFlow(context: Context) {
    val intent = Intent(EuiccManager.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION)
    intent.setPackage(Utils.PHONE_PACKAGE_NAME)
    intent.putExtra(EuiccManager.EXTRA_FORCE_PROVISION, true)
    context.startActivity(intent)
}
