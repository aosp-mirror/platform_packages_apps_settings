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

import android.telephony.SubscriptionManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.network.SimOnboardingService
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.scaffold.BottomAppBarButton
import com.android.settingslib.spa.widget.scaffold.SuwScaffold
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spa.widget.ui.SettingsIcon

/**
 * the sim onboarding primary sim compose
 */
@Composable
fun SimOnboardingPrimarySimImpl(
    nextAction: () -> Unit,
    cancelAction: () -> Unit,
    onboardingService: SimOnboardingService
) {
    SuwScaffold(
        imageVector = Icons.Outlined.SignalCellularAlt,
        title = stringResource(id = R.string.sim_onboarding_primary_sim_title),
        actionButton = BottomAppBarButton(
            stringResource(id = R.string.done),
            nextAction
        ),
        dismissButton = BottomAppBarButton(
            stringResource(id = R.string.cancel),
            cancelAction
        ),
    ) {
        val callsSelectedId = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
        val textsSelectedId = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
        val mobileDataSelectedId = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
        val nonDdsRemember = rememberSaveable {
            mutableIntStateOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }

        Column(Modifier.padding(SettingsDimension.itemPadding)) {
            SettingsBody(stringResource(id = R.string.sim_onboarding_primary_sim_msg))
        }

        var selectedSubscriptionInfoList =
                onboardingService.getSelectedSubscriptionInfoListWithRenaming()
        callsSelectedId.intValue = onboardingService.targetPrimarySimCalls
        textsSelectedId.intValue = onboardingService.targetPrimarySimTexts
        mobileDataSelectedId.intValue = onboardingService.targetPrimarySimMobileData
        PrimarySimImpl(
            subscriptionInfoList = selectedSubscriptionInfoList,
            callsSelectedId = callsSelectedId,
            textsSelectedId = textsSelectedId,
            mobileDataSelectedId = mobileDataSelectedId,
            nonDds = nonDdsRemember,
            actionSetCalls = {
                callsSelectedId.intValue = it
                onboardingService.targetPrimarySimCalls = it},
            actionSetTexts = {
                textsSelectedId.intValue = it
                onboardingService.targetPrimarySimTexts = it},
            actionSetMobileData = {
                mobileDataSelectedId.intValue = it
                onboardingService.targetPrimarySimMobileData = it},
            actionSetAutoDataSwitch = {
                onboardingService.targetPrimarySimAutoDataSwitch = it},
        )
    }
}

@Composable
fun CreatePrimarySimListPreference(
        title: String,
        list: List<ListPreferenceOption>,
        selectedId: MutableIntState,
        icon: ImageVector,
        onIdSelected: (id: Int) -> Unit
) = ListPreference(
    object : ListPreferenceModel {
        override val title = title
        override val options = list
        override val selectedId = selectedId
        override val onIdSelected = onIdSelected
        override val icon = @Composable {
            SettingsIcon(icon)
        }
})