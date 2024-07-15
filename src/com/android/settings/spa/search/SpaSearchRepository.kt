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

package com.android.settings.spa.search

import android.content.Context
import android.provider.SearchIndexableResource
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settingslib.search.Indexable
import com.android.settingslib.search.SearchIndexableData
import com.android.settingslib.search.SearchIndexableRaw
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironment
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory

class SpaSearchRepository(
    private val spaEnvironment: SpaEnvironment = SpaEnvironmentFactory.instance,
) {
    fun getSearchIndexableDataList(): List<SearchIndexableData> {
        Log.d(TAG, "getSearchIndexableDataList")
        return spaEnvironment.pageProviderRepository.value.getAllProviders().mapNotNull { page ->
            if (page is SearchablePage) {
                page.createSearchIndexableData(page::getSearchableTitles)
            } else null
        }
    }

    companion object {
        private const val TAG = "SpaSearchRepository"

        @VisibleForTesting
        fun SettingsPageProvider.createSearchIndexableData(
            titlesProvider: (context: Context) -> List<String>,
        ): SearchIndexableData {
            val searchIndexProvider =
                object : Indexable.SearchIndexProvider {
                    override fun getXmlResourcesToIndex(
                        context: Context,
                        enabled: Boolean,
                    ): List<SearchIndexableResource> = emptyList()

                    override fun getRawDataToIndex(
                        context: Context,
                        enabled: Boolean,
                    ): List<SearchIndexableRaw> = emptyList()

                    override fun getDynamicRawDataToIndex(
                        context: Context,
                        enabled: Boolean,
                    ): List<SearchIndexableRaw> =
                        titlesProvider(context).map { title ->
                            createSearchIndexableRaw(context, title)
                        }

                    override fun getNonIndexableKeys(context: Context): List<String> = emptyList()
                }
            return SearchIndexableData(this::class.java, searchIndexProvider)
        }

        private fun SettingsPageProvider.createSearchIndexableRaw(context: Context, title: String) =
            SearchIndexableRaw(context).apply {
                key = name
                this.title = title
                intentAction = SEARCH_LANDING_ACTION
                packageName = context.packageName
                className = SpaSearchLandingActivity::class.qualifiedName
            }

        private const val SEARCH_LANDING_ACTION = "android.settings.SPA_SEARCH_LANDING"
    }
}
