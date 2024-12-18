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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.network.SimOnboardingService
import com.android.settingslib.spa.widget.preference.CheckboxPreference
import com.android.settingslib.spa.widget.preference.CheckboxPreferenceModel
import com.android.settingslib.spa.widget.scaffold.BottomAppBarButton
import com.android.settingslib.spa.widget.scaffold.SuwScaffold

/**
 * the sim onboarding select sim compose
 */
@Composable
fun SimOnboardingSelectSimImpl(
    nextAction: () -> Unit,
    cancelAction: () -> Unit,
    onboardingService: SimOnboardingService
) {
    var actionButtonController = rememberSaveable { mutableStateOf(false) }

    SuwScaffold(
        imageVector = Icons.Outlined.SignalCellularAlt,
        title = stringResource(id = R.string.sim_onboarding_select_sim_title),
        actionButton = BottomAppBarButton(
            text = stringResource(id = R.string.sim_onboarding_next),
            enabled = actionButtonController.value,
            onClick = nextAction
        ),
        dismissButton = BottomAppBarButton(
            text = stringResource(id = R.string.cancel),
            onClick = cancelAction
        ),
    ) {
        SelectSimBody(onboardingService, actionButtonController)
    }
}

@Composable
private fun SelectSimBody(
    onboardingService: SimOnboardingService,
    isFinished: MutableState<Boolean>
) {
    SimOnboardingMessage(stringResource(id = R.string.sim_onboarding_select_sim_msg))

    isFinished.value = onboardingService.isSimSelectionFinished
    for (subInfo in onboardingService.getSelectableSubscriptionInfoList()) {
        var title = onboardingService.getSubscriptionInfoDisplayName(subInfo)
        val phoneNumber = phoneNumber(subInfo)
        var checked = rememberSaveable {
            mutableStateOf(
                onboardingService.getSelectedSubscriptionInfoList().contains(subInfo)
            )
        }
        CheckboxPreference(remember {
            object : CheckboxPreferenceModel {
                override val title = title
                override val summary: () -> String
                    get() = { phoneNumber.value ?: "" }
                override val checked = { checked.value }
                override val onCheckedChange = { newChecked: Boolean ->
                    checked.value = newChecked
                    if (newChecked) {
                        onboardingService.addItemForSelectedSim(subInfo)
                    } else {
                        onboardingService.removeItemForSelectedSim(subInfo)
                    }
                    isFinished.value = onboardingService.isSimSelectionFinished
                }
                override val changeable = {
                    subInfo.isActive
                        && (!isFinished.value || (isFinished.value && checked.value))
                }
            }
        })
    }
}
