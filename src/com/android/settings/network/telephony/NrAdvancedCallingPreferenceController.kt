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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Preference controller for "Voice over NR".
 */
class NrAdvancedCallingPreferenceController @JvmOverloads constructor(
    context: Context,
    key: String,
    private val callStateRepository : CallStateRepository = CallStateRepository(context),
) : ComposePreferenceController(context, key) {
    private var subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var repository: VoNrRepository? = null

    /** Initial this PreferenceController. */
    @JvmOverloads
    fun init(subId: Int, repository: VoNrRepository = VoNrRepository(mContext, subId)) {
        this.subId = subId
        this.repository = repository
    }

    override fun getAvailabilityStatus() =
        if (repository?.isVoNrAvailable() == true) AVAILABLE else CONDITIONALLY_UNAVAILABLE

    @Composable
    override fun Content() {
        val summary = stringResource(R.string.nr_advanced_calling_summary)
        val isInCall by remember { callStateRepository.isInCallFlow() }
            .collectAsStateWithLifecycle(initialValue = false)
        val isEnabled by remember {
            repository?.isVoNrEnabledFlow() ?: flowOf(false)
        }.collectAsStateWithLifecycle(initialValue = false)
        val coroutineScope = rememberCoroutineScope()
        SwitchPreference(object : SwitchPreferenceModel {
            override val title = stringResource(R.string.nr_advanced_calling_title)
            override val summary = { summary }
            override val changeable = { !isInCall }
            override val checked = { isEnabled }
            override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                coroutineScope.launch {
                    repository?.setVoNrEnabled(newChecked)
                }
            }
        })
    }
}
