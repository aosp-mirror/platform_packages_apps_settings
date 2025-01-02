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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.network.telephony.MobileDataRepository
import com.android.settings.network.telephony.subscriptionManager
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MobileDataSwitchPreference(subId: Int) {
    MobileDataSwitchPreference(
        subId = subId,
        mobileDataRepository = rememberContext(::MobileDataRepository),
        setMobileData = setMobileDataImpl(subId),
    )
}

@VisibleForTesting
@Composable
fun MobileDataSwitchPreference(
    subId: Int,
    mobileDataRepository: MobileDataRepository,
    setMobileData: (newChecked: Boolean) -> Unit,
) {
    val mobileDataSummary = stringResource(id = R.string.mobile_data_settings_summary)
    val isMobileDataEnabled by
        remember(subId) { mobileDataRepository.isMobileDataEnabledFlow(subId) }
            .collectAsStateWithLifecycle(initialValue = null)

    SwitchPreference(
        object : SwitchPreferenceModel {
            override val title = stringResource(id = R.string.mobile_data_settings_title)
            override val summary = { mobileDataSummary }
            override val checked = { isMobileDataEnabled }
            override val onCheckedChange = setMobileData
        }
    )
}

@Composable
private fun setMobileDataImpl(subId: Int): (newChecked: Boolean) -> Unit {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wifiPickerTrackerHelper = rememberWifiPickerTrackerHelper()
    return { newEnabled ->
        coroutineScope.launch(Dispatchers.Default) {
            setMobileData(
                context = context,
                subscriptionManager = context.subscriptionManager,
                wifiPickerTrackerHelper = wifiPickerTrackerHelper,
                subId = subId,
                enabled = newEnabled,
            )
        }
    }
}
