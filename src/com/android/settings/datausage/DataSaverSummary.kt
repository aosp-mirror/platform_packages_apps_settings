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

import android.app.Application
import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.widget.Switch
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.applications.AppStateBaseBridge
import com.android.settings.datausage.AppStateDataUsageBridge.DataUsageState
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.widget.SettingsMainSwitchBar
import com.android.settingslib.applications.ApplicationsState
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.spa.framework.util.formatString
import kotlinx.coroutines.launch

@SearchIndexable
class DataSaverSummary : SettingsPreferenceFragment() {
    private lateinit var switchBar: SettingsMainSwitchBar
    private lateinit var dataSaverBackend: DataSaverBackend
    private lateinit var unrestrictedAccess: Preference
    private var dataUsageBridge: AppStateDataUsageBridge? = null
    private var session: ApplicationsState.Session? = null

    // Flag used to avoid infinite loop due if user switch it on/off too quick.
    private var switching = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        if (!requireContext().isDataSaverVisible()) {
            finishFragment()
            return
        }

        addPreferencesFromResource(R.xml.data_saver)
        unrestrictedAccess = findPreference(KEY_UNRESTRICTED_ACCESS)!!
        dataSaverBackend = DataSaverBackend(requireContext())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        switchBar = (activity as SettingsActivity).switchBar.apply {
            setTitle(getString(R.string.data_saver_switch_title))
            show()
            addOnSwitchChangeListener { _: Switch, isChecked: Boolean ->
                onSwitchChanged(isChecked)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataSaverBackend.refreshAllowlist()
        dataSaverBackend.refreshDenylist()
        dataSaverBackend.addListener(dataSaverBackendListener)
        dataUsageBridge?.resume(/* forceLoadAllApps= */ true)
            ?: viewLifecycleOwner.lifecycleScope.launch {
                val applicationsState = ApplicationsState.getInstance(
                    requireContext().applicationContext as Application
                )
                dataUsageBridge = AppStateDataUsageBridge(
                    applicationsState, dataUsageBridgeCallbacks, dataSaverBackend
                )
                session =
                    applicationsState.newSession(applicationsStateCallbacks, settingsLifecycle)
                dataUsageBridge?.resume(/* forceLoadAllApps= */ true)
            }
    }

    override fun onPause() {
        super.onPause()
        dataSaverBackend.remListener(dataSaverBackendListener)
        dataUsageBridge?.pause()
    }

    private fun onSwitchChanged(isChecked: Boolean) {
        synchronized(this) {
            if (!switching) {
                switching = true
                dataSaverBackend.isDataSaverEnabled = isChecked
            }
        }
    }

    override fun getMetricsCategory() = SettingsEnums.DATA_SAVER_SUMMARY

    override fun getHelpResource() = R.string.help_url_data_saver

    private val dataSaverBackendListener = object : DataSaverBackend.Listener {
        override fun onDataSaverChanged(isDataSaving: Boolean) {
            synchronized(this) {
                switchBar.isChecked = isDataSaving
                switching = false
            }
        }

        override fun onAllowlistStatusChanged(uid: Int, isAllowlisted: Boolean) {}

        override fun onDenylistStatusChanged(uid: Int, isDenylisted: Boolean) {}
    }

    private val dataUsageBridgeCallbacks = AppStateBaseBridge.Callback {
        updateUnrestrictedAccessSummary()
    }

    private val applicationsStateCallbacks = object : ApplicationsState.Callbacks {
        override fun onRunningStateChanged(running: Boolean) {}

        override fun onPackageListChanged() {}

        override fun onRebuildComplete(apps: ArrayList<ApplicationsState.AppEntry>?) {}

        override fun onPackageIconChanged() {}

        override fun onPackageSizeChanged(packageName: String?) {}

        override fun onAllSizesComputed() {
            updateUnrestrictedAccessSummary()
        }

        override fun onLauncherInfoChanged() {
            updateUnrestrictedAccessSummary()
        }

        override fun onLoadEntriesCompleted() {}
    }

    private fun updateUnrestrictedAccessSummary() {
        if (!isAdded || isFinishingOrDestroyed) return
        val allApps = session?.allApps ?: return
        val count = allApps.count {
            ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(it) &&
                (it.extraInfo as? DataUsageState)?.isDataSaverAllowlisted == true
        }
        unrestrictedAccess.summary =
            resources.formatString(R.string.data_saver_unrestricted_summary, "count" to count)
    }

    companion object {
        private const val KEY_UNRESTRICTED_ACCESS = "unrestricted_access"

        private fun Context.isDataSaverVisible(): Boolean =
            resources.getBoolean(R.bool.config_show_data_saver)

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.data_saver) {
            override fun isPageSearchEnabled(context: Context): Boolean =
                context.isDataSaverVisible() &&
                    DataUsageUtils.hasMobileData(context) &&
                    (DataUsageUtils.getDefaultSubscriptionId(context) !=
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        }
    }
}