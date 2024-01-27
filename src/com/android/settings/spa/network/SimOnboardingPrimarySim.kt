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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.android.settings.R
import com.android.settings.network.SimOnboardingService
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
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
        primarySimBody(onboardingService)
    }
}

@Composable
private fun primarySimBody(onboardingService: SimOnboardingService) {
    //TODO: Load the status from the frameworks
    var callsSelectedId = rememberSaveable { mutableIntStateOf(1) }
    var textsSelectedId = rememberSaveable { mutableIntStateOf(1) }
    var mobileDataSelectedId = rememberSaveable { mutableIntStateOf(1) }
    var automaticDataChecked by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.padding(SettingsDimension.itemPadding)) {
        SettingsBody(stringResource(id = R.string.sim_onboarding_primary_sim_msg))
    }
    var selectableSubscriptionInfo = onboardingService.getSelectableSubscriptionInfo()
    var list = listOf(ListPreferenceOption(id = -1, text = "Loading"))
    if (selectableSubscriptionInfo.size >= 2) {
        list = listOf(
            ListPreferenceOption(
                id = selectableSubscriptionInfo[0].subscriptionId,
                text = "${selectableSubscriptionInfo[0].displayName}"
            ),
            ListPreferenceOption(
                id = selectableSubscriptionInfo[1].subscriptionId,
                text = "${selectableSubscriptionInfo[1].displayName}"
            ),
            ListPreferenceOption(
                id = -1,
                text = stringResource(id = R.string.sim_calls_ask_first_prefs_title)
            ),
        )
    } else {
        // set all of primary sim items' enable as false and showing that sim.
    }
    createPrimarySimListPreference(
        stringResource(id = R.string.primary_sim_calls_title),
        list,
        callsSelectedId,
        ImageVector.vectorResource(R.drawable.ic_phone),
        onIdSelected = { callsSelectedId.intValue = it }
    )
    createPrimarySimListPreference(
        stringResource(id = R.string.primary_sim_texts_title),
        list,
        textsSelectedId,
        Icons.AutoMirrored.Outlined.Message,
        onIdSelected = { textsSelectedId.intValue = it }
    )

    createPrimarySimListPreference(
            stringResource(id = R.string.mobile_data_settings_title),
            list,
            mobileDataSelectedId,
        Icons.Outlined.DataUsage,
            onIdSelected = { mobileDataSelectedId.intValue = it }
    )

    val autoDataTitle = stringResource(id = R.string.primary_sim_automatic_data_title)
    val autoDataSummary = stringResource(id = R.string.primary_sim_automatic_data_msg)
    SwitchPreference(remember {
        object : SwitchPreferenceModel {
            override val title = autoDataTitle
            override val summary = { autoDataSummary }
            override val checked = { automaticDataChecked }
            override val onCheckedChange =
                { newChecked: Boolean -> automaticDataChecked = newChecked }
        }
    })
}

@Composable
fun createPrimarySimListPreference(
        title: String,
        list: List<ListPreferenceOption>,
        selectedId: MutableIntState,
        icon: ImageVector,
        enable: Boolean = true,
        onIdSelected: (id: Int) -> Unit
) = ListPreference(remember {
    object : ListPreferenceModel {
        override val title = title
        override val options = list
        override val selectedId = selectedId
        override val onIdSelected = onIdSelected
        override val icon = @Composable {
            SettingsIcon(icon)
        }
        override val enabled: () -> Boolean
            get() = { enable }
    }
})