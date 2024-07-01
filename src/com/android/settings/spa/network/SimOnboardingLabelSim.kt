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

import android.telephony.SubscriptionInfo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.network.SimOnboardingService
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spa.widget.editor.SettingsOutlinedTextField

import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.BottomAppBarButton
import com.android.settingslib.spa.widget.scaffold.SuwScaffold

/**
 * the sim onboarding label compose
 */
@Composable
fun SimOnboardingLabelSimImpl(
    nextAction: () -> Unit,
    cancelAction: () -> Unit,
    onboardingService: SimOnboardingService
) {
    SuwScaffold(
        imageVector = Icons.Outlined.SignalCellularAlt,
        title = stringResource(R.string.sim_onboarding_label_sim_title),
        actionButton = BottomAppBarButton(
            text = stringResource(R.string.sim_onboarding_next),
            onClick = nextAction
        ),
        dismissButton = BottomAppBarButton(
            text = stringResource(R.string.cancel),
            onClick = cancelAction
        ),
    ) {
        LabelSimBody(onboardingService)
    }
}

@Composable
private fun LabelSimBody(onboardingService: SimOnboardingService) {
    SimOnboardingMessage(stringResource(R.string.sim_onboarding_label_sim_msg))

    for (subInfo in onboardingService.getSelectableSubscriptionInfoList()) {
        LabelSimPreference(onboardingService, subInfo)
    }
}

@Composable
private fun LabelSimPreference(
    onboardingService: SimOnboardingService,
    subInfo: SubscriptionInfo,
) {
    val currentSimName = onboardingService.getSubscriptionInfoDisplayName(subInfo)
    var prefTitle by remember {
        mutableStateOf(currentSimName)
    }
    var dialogInputContent by remember {
        mutableStateOf(currentSimName)
    }
    val phoneNumber = phoneNumber(subInfo)
    val alertDialogPresenter = rememberAlertDialogPresenter(
        confirmButton = AlertDialogButton(
            stringResource(R.string.mobile_network_sim_name_rename),
            dialogInputContent.isNotBlank()
        ) {
            onboardingService.addItemForRenaming(
                subInfo, dialogInputContent
            )
            prefTitle = dialogInputContent
        },
        dismissButton = AlertDialogButton(
            stringResource(R.string.cancel),
        ) {
            // Do nothing
        },
        title = stringResource(R.string.sim_onboarding_label_sim_dialog_title),
        text = {
            Text(
                phoneNumber.value ?: "",
                modifier = Modifier.padding(bottom = SettingsDimension.itemPaddingVertical)
            )
            SettingsOutlinedTextField(
                value = dialogInputContent,
                label = stringResource(R.string.sim_onboarding_label_sim_dialog_label),
                placeholder = {Text(text = subInfo.displayName.toString())},
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contentInput")
            ) {
                dialogInputContent = it
            }
        },
    )
    Preference(object : PreferenceModel {
        override val title = prefTitle
        override val summary = { phoneNumber.value ?: "" }
        override val onClick = alertDialogPresenter::open
    })
}
