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

package com.android.settings.network.apn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.editor.SettingsDropdownCheckBox

@Composable
fun ApnNetworkTypeCheckBox(apnData: ApnData, onNetworkTypeChanged: (Long) -> Unit) {
    val options = remember { ApnNetworkTypes.getNetworkTypeOptions() }
    val selectedStateMap = remember {
        ApnNetworkTypes.networkTypeToSelectedStateMap(options, apnData.networkType)
    }
    SettingsDropdownCheckBox(
        label = stringResource(R.string.network_type),
        options = options,
        emptyText = stringResource(R.string.network_type_unspecified),
        enabled = apnData.networkTypeEnabled,
    ) {
        onNetworkTypeChanged(
            ApnNetworkTypes.selectedStateMapToNetworkType(options, selectedStateMap)
        )
    }
}
