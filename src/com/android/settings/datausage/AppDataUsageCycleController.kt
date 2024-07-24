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

import android.content.Context
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settings.datausage.lib.IAppDataUsageDetailsRepository
import com.android.settings.datausage.lib.NetworkUsageDetailsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDataUsageCycleController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private lateinit var repository: IAppDataUsageDetailsRepository
    private var onUsageDataUpdated: (NetworkUsageDetailsData) -> Unit = {}
    private lateinit var preference: SpinnerPreference
    private var cycleAdapter: CycleAdapter? = null

    private var usageDetailsDataList: List<NetworkUsageDetailsData> = emptyList()

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
        if (cycleAdapter == null) {
            cycleAdapter = CycleAdapter(mContext, preference)
        }
    }

    fun init(
        repository: IAppDataUsageDetailsRepository,
        onUsageDataUpdated: (NetworkUsageDetailsData) -> Unit,
    ) {
        this.repository = repository
        this.onUsageDataUpdated = onUsageDataUpdated
    }

    /**
     * Sets the initial cycles.
     *
     * If coming from a page like DataUsageList where already has a selected cycle, display that
     * before loading to reduce flicker.
     */
    fun setInitialCycles(initialCycles: List<Long>, initialSelectedEndTime: Long) {
        if (initialCycles.isNotEmpty()) {
            cycleAdapter?.setInitialCycleList(initialCycles, initialSelectedEndTime)
            preference.setHasCycles(true)
        }
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                update()
            }
        }
    }

    private suspend fun update() {
        usageDetailsDataList = withContext(Dispatchers.Default) {
            repository.queryDetailsForCycles()
        }
        if (usageDetailsDataList.isEmpty()) {
            preference.setHasCycles(false)
            onUsageDataUpdated(NetworkUsageDetailsData.AllZero)
            return
        }

        preference.setHasCycles(true)
        cycleAdapter?.updateCycleList(usageDetailsDataList.map { it.range })
        preference.setOnItemSelectedListener(cycleListener)
    }

    private val cycleListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            usageDetailsDataList.getOrNull(position)?.let(onUsageDataUpdated)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // ignored
        }
    }
}
