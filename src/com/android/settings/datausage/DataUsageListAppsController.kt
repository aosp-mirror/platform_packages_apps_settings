/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.app.ActivityManager
import android.content.Context
import android.net.NetworkTemplate
import android.os.Bundle
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.datausage.lib.AppDataUsageRepository
import com.android.settings.datausage.lib.NetworkUsageData
import com.android.settingslib.AppItem
import com.android.settingslib.net.UidDetailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OpenForTesting
open class DataUsageListAppsController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private val uidDetailProvider = UidDetailProvider(context)
    private lateinit var template: NetworkTemplate
    private lateinit var repository: AppDataUsageRepository
    private lateinit var preference: PreferenceGroup
    private lateinit var lifecycleScope: LifecycleCoroutineScope

    private var cycleData: List<NetworkUsageData>? = null

    open fun init(template: NetworkTemplate) {
        this.template = template
        repository = AppDataUsageRepository(
            context = mContext,
            currentUserId = ActivityManager.getCurrentUser(),
            template = template,
        ) { appItem: AppItem -> uidDetailProvider.getUidDetail(appItem.key, true).packageName }
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        lifecycleScope = viewLifecycleOwner.lifecycleScope
    }

    fun updateCycles(cycleData: List<NetworkUsageData>) {
        this.cycleData = cycleData
    }

    fun update(carrierId: Int?, startTime: Long, endTime: Long) = lifecycleScope.launch {
        val apps = withContext(Dispatchers.Default) {
            repository.getAppPercent(carrierId, startTime, endTime).map { (appItem, percent) ->
                AppDataUsagePreference(mContext, appItem, percent, uidDetailProvider).apply {
                    setOnPreferenceClickListener {
                        startAppDataUsage(appItem, endTime)
                        true
                    }
                }
            }
        }
        preference.removeAll()
        for (app in apps) {
            preference.addPreference(app)
        }
    }

    @VisibleForTesting
    fun startAppDataUsage(item: AppItem, endTime: Long) {
        val cycleData = cycleData ?: return
        val args = Bundle().apply {
            putParcelable(AppDataUsage.ARG_APP_ITEM, item)
            putParcelable(AppDataUsage.ARG_NETWORK_TEMPLATE, template)
            val cycles = ArrayList<Long>().apply {
                for (data in cycleData) {
                    if (isEmpty()) add(data.endTime)
                    add(data.startTime)
                }
            }
            putSerializable(AppDataUsage.ARG_NETWORK_CYCLES, cycles)
            putLong(AppDataUsage.ARG_SELECTED_CYCLE, endTime)
        }
        SubSettingLauncher(mContext).apply {
            setDestination(AppDataUsage::class.java.name)
            setTitleRes(R.string.data_usage_app_summary_title)
            setArguments(args)
            setSourceMetricsCategory(metricsCategory)
        }.launch()
    }
}
