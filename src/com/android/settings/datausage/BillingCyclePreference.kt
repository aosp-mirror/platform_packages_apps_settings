/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage

import android.app.settings.SettingsEnums
import android.content.Context
import android.net.NetworkTemplate
import android.os.Bundle
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.datausage.lib.BillingCycleRepository
import com.android.settings.network.mobileDataEnabledFlow
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import kotlinx.coroutines.flow.map

/**
 * Preference which displays billing cycle of subscription
 *
 * @param context Context of preference
 * @param attrs   The attributes of the XML tag that is inflating the preference
 */
class BillingCyclePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    private val repository: BillingCycleRepository = BillingCycleRepository(context),
) : ComposePreference(context, attrs), TemplatePreference {

    override fun setTemplate(template: NetworkTemplate, subId: Int) {
        setContent {
            val isModifiable by remember {
                context.mobileDataEnabledFlow(subId).map { repository.isModifiable(subId) }
            }.collectAsStateWithLifecycle(initialValue = false)

            Preference(object : PreferenceModel {
                override val title = stringResource(R.string.billing_cycle)
                override val enabled = { isModifiable }
                override val onClick = { launchBillingCycleSettings(template) }
            })
        }
    }

    private fun launchBillingCycleSettings(template: NetworkTemplate) {
        val args = Bundle().apply {
            putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, template)
        }
        SubSettingLauncher(context).apply {
            setDestination(BillingCycleSettings::class.java.name)
            setArguments(args)
            setTitleRes(R.string.billing_cycle)
            setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
        }.launch()
    }
}
