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

package com.android.settings.datausage

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.datausage.lib.DataUsageFormatter
import com.android.settings.datausage.lib.NetworkUsageDetailsData
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.compose.getPlaceholder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class AppDataUsageSummaryController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    private val dataFlow = MutableStateFlow(NetworkUsageDetailsData.AllZero)
    private val dataUsageFormatter = DataUsageFormatter(context)
    private val emptyDataUsage =
        DataUsageFormatter.FormattedDataUsage(context.getPlaceholder(), context.getPlaceholder())

    private val totalUsageFlow = dataFlow.map {
        dataUsageFormatter.formatDataUsage(it.totalUsage)
    }

    private val foregroundUsageFlow = dataFlow.map {
        dataUsageFormatter.formatDataUsage(it.foregroundUsage)
    }

    private val backgroundUsageFlow = dataFlow.map {
        dataUsageFormatter.formatDataUsage(it.backgroundUsage)
    }

    override fun getAvailabilityStatus() = AVAILABLE

    fun update(data: NetworkUsageDetailsData) {
        dataFlow.value = data
    }

    @Composable
    override fun Content() {
        Column {
            val totalUsage by totalUsageFlow.collectAsStateWithLifecycle(emptyDataUsage)
            val foregroundUsage by foregroundUsageFlow.collectAsStateWithLifecycle(emptyDataUsage)
            val backgroundUsage by backgroundUsageFlow.collectAsStateWithLifecycle(emptyDataUsage)
            Preference(object : PreferenceModel {
                override val title = stringResource(R.string.total_size_label)
                override val summary = { totalUsage.displayText }
                override val summaryContentDescription = { totalUsage.contentDescription }
            })
            Preference(object : PreferenceModel {
                override val title = stringResource(R.string.data_usage_label_foreground)
                override val summary = { foregroundUsage.displayText }
                override val summaryContentDescription = { foregroundUsage.contentDescription }
            })
            Preference(object : PreferenceModel {
                override val title = stringResource(R.string.data_usage_label_background)
                override val summary = { backgroundUsage.displayText }
                override val summaryContentDescription = { backgroundUsage.contentDescription }
            })
        }
    }
}
