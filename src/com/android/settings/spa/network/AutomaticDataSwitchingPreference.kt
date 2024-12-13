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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.settings.R
import com.android.settings.network.telephony.wificalling.CrossSimCallingViewModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AutomaticDataSwitchingPreference(
    isAutoDataEnabled: () -> Boolean?,
    setAutoDataEnabled: (newEnabled: Boolean) -> Unit,
) {
    val autoDataSummary = stringResource(id = R.string.primary_sim_automatic_data_msg)
    val coroutineScope = rememberCoroutineScope()
    // CrossSimCallingViewModel is responsible for maintaining the correct cross sim calling
    // settings (backup calling).
    viewModel<CrossSimCallingViewModel>()
    SwitchPreference(
        object : SwitchPreferenceModel {
            override val title = stringResource(id = R.string.primary_sim_automatic_data_title)
            override val summary = { autoDataSummary }
            override val checked = { isAutoDataEnabled() }
            override val onCheckedChange: (Boolean) -> Unit = { newEnabled ->
                coroutineScope.launch(Dispatchers.Default) {
                    setAutoDataEnabled(newEnabled)
                }
            }
        }
    )
}
