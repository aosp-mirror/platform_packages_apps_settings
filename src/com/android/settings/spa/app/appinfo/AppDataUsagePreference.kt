/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.NetworkStats
import android.net.NetworkTemplate
import android.os.Process
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.datausage.AppDataUsage
import com.android.settings.datausage.DataUsageUtils
import com.android.settingslib.net.NetworkCycleDataForUid
import com.android.settingslib.net.NetworkCycleDataForUidLoader
import com.android.settingslib.spa.framework.compose.toState
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.hasFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

@Composable
fun AppDataUsagePreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember { AppDataUsagePresenter(context, app) }
    if (!presenter.isAvailableFlow.collectAsStateWithLifecycle(initialValue = false).value) return

    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.data_usage_app_summary_title)
        override val summary = presenter.summaryFlow.collectAsStateWithLifecycle(
            initialValue = stringResource(R.string.computing_size),
        )
        override val enabled = presenter.isEnabled().toState()
        override val onClick = presenter::startActivity
    })
}

private class AppDataUsagePresenter(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    val isAvailableFlow = flow { emit(isAvailable()) }

    private suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        Utils.isBandwidthControlEnabled()
    }

    fun isEnabled() = app.hasFlag(ApplicationInfo.FLAG_INSTALLED)

    val summaryFlow = flow { emit(getSummary()) }

    private suspend fun getSummary() = withContext(Dispatchers.IO) {
        val appUsageData = getAppUsageData()
        val totalBytes = appUsageData.sumOf { it.totalUsage }
        if (totalBytes == 0L) {
            context.getString(R.string.no_data_usage)
        } else {
            val startTime = appUsageData.minOfOrNull { it.startTime } ?: System.currentTimeMillis()
            context.getString(
                R.string.data_summary_format,
                Formatter.formatFileSize(context, totalBytes, Formatter.FLAG_IEC_UNITS),
                DateUtils.formatDateTime(context, startTime, DATE_FORMAT),
            )
        }
    }

    private suspend fun getAppUsageData(): List<NetworkCycleDataForUid> =
        withContext(Dispatchers.IO) {
            createLoader().loadInBackground() ?: emptyList()
        }

    private fun createLoader(): NetworkCycleDataForUidLoader =
        NetworkCycleDataForUidLoader.builder(context).apply {
            setRetrieveDetail(false)
            setNetworkTemplate(getTemplate())
            addUid(app.uid)
            if (Process.isApplicationUid(app.uid)) {
                // Also add in network usage for the app's SDK sandbox
                addUid(Process.toSdkSandboxUid(app.uid))
            }
        }.build()

    private fun getTemplate(): NetworkTemplate = when {
        DataUsageUtils.hasReadyMobileRadio(context) -> {
            NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE)
                .setMeteredness(NetworkStats.METERED_YES)
                .build()
        }
        DataUsageUtils.hasWifiRadio(context) -> {
            NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build()
        }
        else -> NetworkTemplate.Builder(NetworkTemplate.MATCH_ETHERNET).build()
    }

    fun startActivity() {
        AppInfoDashboardFragment.startAppInfoFragment(
            AppDataUsage::class.java,
            app,
            context,
            AppInfoSettingsProvider.METRICS_CATEGORY,
        )
    }

    private companion object {
        const val DATE_FORMAT = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    }
}
