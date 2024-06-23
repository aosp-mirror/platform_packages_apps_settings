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

import android.telephony.data.ApnSetting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.network.apn.ApnTypes.toApnType
import com.android.settingslib.spa.widget.editor.SettingsDropdownCheckBox

@Composable
fun ApnTypeCheckBox(
    apnData: ApnData,
    onTypeChanged: (String) -> Unit,
    onMmsSelectedChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val apnTypeOptions = remember {
        ApnTypes.getOptions(context, apnData.apnType, apnData.customizedConfig.readOnlyApnTypes)
    }

    fun updateMmsSelected() {
        val apnTypeOptionMms = apnTypeOptions.single { it.text == ApnSetting.TYPE_MMS_STRING }
        onMmsSelectedChanged(apnTypeOptionMms.selected.value)
    }
    LaunchedEffect(Unit) { updateMmsSelected() }
    SettingsDropdownCheckBox(
        label = stringResource(R.string.apn_type),
        options = apnTypeOptions,
        enabled = apnData.apnTypeEnabled,
    ) {
        onTypeChanged(apnTypeOptions.toApnType())
        updateMmsSelected()
    }
}
