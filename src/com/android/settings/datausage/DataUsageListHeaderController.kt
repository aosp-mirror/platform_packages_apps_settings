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

import android.net.NetworkTemplate
import android.os.Bundle
import android.util.Range
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.AdapterView
import android.widget.Spinner
import androidx.annotation.OpenForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.datausage.CycleAdapter.SpinnerInterface
import com.android.settings.datausage.lib.INetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkCycleDataRepository
import com.android.settings.datausage.lib.NetworkUsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OpenForTesting
open class DataUsageListHeaderController(
    header: View,
    template: NetworkTemplate,
    sourceMetricsCategory: Int,
    viewLifecycleOwner: LifecycleOwner,
    private val onCyclesLoad: (usageDataList: List<NetworkUsageData>) -> Unit,
    private val updateSelectedCycle: (usageData: NetworkUsageData) -> Unit,
    private val repository: INetworkCycleDataRepository =
        NetworkCycleDataRepository(header.context, template),
) {
    private val context = header.context

    private val configureButton: View = header.requireViewById(R.id.filter_settings)
    private val cycleSpinner: Spinner = header.requireViewById(R.id.filter_spinner)

    private val cycleListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            setSelectedCycle(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // ignored
        }
    }

    private val cycleAdapter = CycleAdapter(context, object : SpinnerInterface {
        override fun setAdapter(cycleAdapter: CycleAdapter) {
            cycleSpinner.adapter = cycleAdapter
        }

        override fun getSelectedItem() = cycleSpinner.selectedItem

        override fun setSelection(position: Int) {
            cycleSpinner.setSelection(position)
            if (cycleSpinner.onItemSelectedListener == null) {
                cycleSpinner.onItemSelectedListener = cycleListener
            } else {
                setSelectedCycle(position)
            }
        }
    })

    private var cycles: List<NetworkUsageData> = emptyList()

    init {
        configureButton.setOnClickListener {
            val args = Bundle().apply {
                putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, template)
            }
            SubSettingLauncher(context).apply {
                setDestination(BillingCycleSettings::class.java.name)
                setTitleRes(R.string.billing_cycle)
                setSourceMetricsCategory(sourceMetricsCategory)
                setArguments(args)
            }.launch()
        }
        cycleSpinner.visibility = View.GONE
        cycleSpinner.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                // Ignore TYPE_VIEW_SELECTED or TalkBack will speak for it at onResume.
                if (eventType == AccessibilityEvent.TYPE_VIEW_SELECTED) return
                super.sendAccessibilityEvent(host, eventType)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cycles = withContext(Dispatchers.Default) {
                    repository.loadCycles()
                }
                updateCycleData()
            }
        }
    }

    open fun setConfigButtonVisible(visible: Boolean) {
        configureButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun updateCycleData() {
        cycleAdapter.updateCycleList(cycles.map { Range(it.startTime, it.endTime) })
        cycleSpinner.visibility = View.VISIBLE
        onCyclesLoad(cycles)
    }

    private fun setSelectedCycle(position: Int) {
        cycles.getOrNull(position)?.let(updateSelectedCycle)
    }
}
