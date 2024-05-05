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

package com.android.settings.connecteddevice.threadnetwork

import android.app.settings.SettingsEnums
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

/** The fragment for Thread settings in "Connected devices > Connection preferences > Thread". */
@SearchIndexable(forTarget = SearchIndexable.ALL and SearchIndexable.ARC.inv())
class ThreadNetworkFragment : DashboardFragment() {
    override fun getPreferenceScreenResId() = R.xml.thread_network_settings

    override fun getLogTag() = "ThreadNetworkFragment"

    override fun getMetricsCategory() = SettingsEnums.CONNECTED_DEVICE_PREFERENCES_THREAD

    companion object {
        /** For Search. */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.thread_network_settings)
    }
}
