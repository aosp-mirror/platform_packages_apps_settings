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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.network.SubscriptionUtil
import com.android.settings.network.telephony.MobileNetworkUtils
import com.android.settings.network.telephony.isSubscriptionEnabledFlow
import com.android.settings.network.telephony.phoneNumberFlow
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.preference.TwoTargetSwitchPreference
import com.android.settingslib.spa.widget.ui.SettingsIcon
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.template.preference.RestrictedPreference

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
        context.isSubscriptionEnabledFlow(subInfo.subscriptionId)
    }.collectAsStateWithLifecycle(initialValue = false)
    val phoneNumber = remember(subInfo) {
        context.phoneNumberFlow(subInfo)
    }.collectAsStateWithLifecycle(initialValue = null)
    //TODO: Add the Restricted TwoTargetSwitchPreference in SPA
    TwoTargetSwitchPreference(
        object : SwitchPreferenceModel {
            override val title = subInfo.displayName.toString()
            override val summary = { phoneNumber.value ?: "" }
            override val checked = { checked.value }
            override val onCheckedChange = { newChecked: Boolean ->
                SubscriptionUtil.startToggleSubscriptionDialogActivity(
                    context,
                    subInfo.subscriptionId,
                    newChecked,
                )
            }
        }
    ) {
        MobileNetworkUtils.launchMobileNetworkSettings(context, subInfo)
    }
}

@Composable
private fun AddSim() {
    val context = LocalContext.current
    if (remember { MobileNetworkUtils.showEuiccSettings(context) }) {
        RestrictedPreference(
            model = object : PreferenceModel {
                override val title = stringResource(id = R.string.mobile_network_list_add_more)
                override val icon = @Composable { SettingsIcon(Icons.Outlined.Add) }
                override val onClick = { startAddSimFlow(context) }
            },
            restrictions = Restrictions(keys = listOf(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)),
        )
    }
}

private fun startAddSimFlow(context: Context) {
    val intent = Intent(EuiccManager.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION)
    intent.putExtra(EuiccManager.EXTRA_FORCE_PROVISION, true)
    context.startActivity(intent)
}
