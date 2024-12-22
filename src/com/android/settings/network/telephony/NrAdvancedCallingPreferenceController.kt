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
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.launch

/** Preference controller for "Voice over NR". */
class NrAdvancedCallingPreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val voNrRepository: VoNrRepository = VoNrRepository(context),
    private val callStateRepository: CallStateRepository = CallStateRepository(context),
) : ComposePreferenceController(context, key) {
    private var subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private val searchItem = NrAdvancedCallingSearchItem(context)

    /** Initial this PreferenceController. */
    fun init(subId: Int) {
        this.subId = subId
    }

    override fun getAvailabilityStatus() =
        if (searchItem.isAvailable(subId)) AVAILABLE else CONDITIONALLY_UNAVAILABLE

    @Composable
    override fun Content() {
        val summary = stringResource(R.string.nr_advanced_calling_summary)
        val isInCall by
            remember { callStateRepository.isInCallFlow() }
                .collectAsStateWithLifecycle(initialValue = false)
        val isVoNrEnabled by
            remember { voNrRepository.isVoNrEnabledFlow(subId) }
                .collectAsStateWithLifecycle(initialValue = false)
        val coroutineScope = rememberCoroutineScope()
        SwitchPreference(
            object : SwitchPreferenceModel {
                override val title = stringResource(R.string.nr_advanced_calling_title)
                override val summary = { summary }
                override val changeable = { !isInCall }
                override val checked = { isVoNrEnabled }
                override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
                    coroutineScope.launch { voNrRepository.setVoNrEnabled(subId, newChecked) }
                }
            }
        )
    }

    companion object {
        class NrAdvancedCallingSearchItem(private val context: Context) :
            MobileNetworkSettingsSearchItem {
            private val voNrRepository = VoNrRepository(context)

            fun isAvailable(subId: Int): Boolean = voNrRepository.isVoNrAvailable(subId)

            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!isAvailable(subId)) return null
                return MobileNetworkSettingsSearchResult(
                    key = "nr_advanced_calling",
                    title = context.getString(R.string.nr_advanced_calling_title),
                    keywords = context.getString(R.string.keywords_nr_advanced_calling),
                )
            }
        }
    }
}
