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

import android.app.Activity
import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.core.SubSettingLauncher
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.password.PasswordUtils
import com.android.settings.spa.SpaDestination

class SpaSearchLandingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val keyString = intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)
        if (!keyString.isNullOrEmpty() && isValidCall()) {
            tryLaunch(this, keyString)
        }
        finish()
    }

    private fun isValidCall(): Boolean {
        val callingAppPackageName = PasswordUtils.getCallingAppPackageName(activityToken)
        if (callingAppPackageName == packageName) {
            // SettingsIntelligence sometimes starts SearchResultTrampoline first, in this case,
            // SearchResultTrampoline checks if the call is valid, then SearchResultTrampoline will
            // start this activity, allow this use case.
            return true
        }
        return callingAppPackageName ==
            featureFactory.searchFeatureProvider.getSettingsIntelligencePkgName(this)
    }

    companion object {
        @VisibleForTesting
        fun tryLaunch(context: Context, keyString: String) {
            val key = decodeToSpaSearchLandingKey(keyString) ?: return
            if (key.hasSpaPage()) {
                val destination = key.spaPage.destination
                if (destination.isNotEmpty()) {
                    Log.d(TAG, "Launch SPA search result: ${key.spaPage}")
                    SpaDestination(destination = destination, highlightMenuKey = null)
                        .startFromExportedActivity(context)
                }
            }
            if (key.hasFragment()) {
                Log.d(TAG, "Launch fragment search result: ${key.fragment}")
                val arguments =
                    Bundle().apply {
                        key.fragment.argumentsMap.forEach { (k, v) ->
                            if (v.hasIntValue()) putInt(k, v.intValue)
                        }
                        putString(EXTRA_FRAGMENT_ARG_KEY, key.fragment.preferenceKey)
                    }
                SubSettingLauncher(context)
                    .setDestination(key.fragment.fragmentName)
                    .setArguments(arguments)
                    .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                    .launch()
            }
        }

        private const val TAG = "SpaSearchLandingActivity"
    }
}
