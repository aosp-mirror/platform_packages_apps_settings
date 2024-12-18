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
 *
 */

package com.android.settings.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.android.settings.search.SearchIndexableResourcesFactory.createSearchIndexableResources
import com.android.settings.spa.search.SpaSearchRepository
import com.android.settingslib.search.SearchIndexableResources

/** FeatureProvider for the refactored search code. */
open class SearchFeatureProviderImpl : SearchFeatureProvider {
    private val lazySearchIndexableResources by lazy {
        createSearchIndexableResources().apply {
            for (searchIndexableData in SpaSearchRepository().getSearchIndexableDataList()) {
                addIndex(searchIndexableData)
            }
        }
    }

    override fun verifyLaunchSearchResultPageCaller(context: Context, callerPackage: String) {
        require(callerPackage.isNotEmpty()) {
            "ExternalSettingsTrampoline intents must be called with startActivityForResult"
        }
        val isSettingsPackage = callerPackage == context.packageName
        if (isSettingsPackage ||
            callerPackage == getSettingsIntelligencePkgName(context) ||
            isSignatureAllowlisted(context, callerPackage)) {
            return
        }
        throw SecurityException(
            "Search result intents must be called with from an allowlisted package.")
    }

    override fun getSearchIndexableResources(): SearchIndexableResources =
        lazySearchIndexableResources

    override fun buildSearchIntent(context: Context, pageId: Int): Intent =
        Intent(Settings.ACTION_APP_SEARCH_SETTINGS)
            .setPackage(getSettingsIntelligencePkgName(context))
            .putExtra(Intent.EXTRA_REFERRER, buildReferrer(context, pageId))

    protected open fun isSignatureAllowlisted(context: Context, callerPackage: String): Boolean =
        false

    companion object {
        private fun buildReferrer(context: Context, pageId: Int): Uri =
            Uri.Builder()
                .scheme("android-app")
                .authority(context.packageName)
                .path(pageId.toString())
                .build()
    }
}
