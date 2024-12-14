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

import android.provider.Telephony
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel

@Composable
fun ApnEditCarrierEnabled(apnData: ApnData, onCarrierEnabledChanged: (Boolean) -> Unit) {
    SwitchPreference(
        object : SwitchPreferenceModel {
            override val title = stringResource(R.string.carrier_enabled)
            val allowEdit = booleanResource(R.bool.config_allow_edit_carrier_enabled)
            override val changeable = {
                allowEdit && apnData.isFieldEnabled(Telephony.Carriers.CARRIER_ENABLED)
            }
            override val checked = { apnData.carrierEnabled }
            override val onCheckedChange = onCarrierEnabledChanged
        }
    )
}
