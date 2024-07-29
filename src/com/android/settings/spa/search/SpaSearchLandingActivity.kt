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
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingKey
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException

class SpaSearchLandingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val keyString = intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)
        if (!keyString.isNullOrEmpty() && isValidCall()) {
            tryLaunch(this, keyString)
        }
        finish()
    }

    private fun isValidCall() =
        PasswordUtils.getCallingAppPackageName(activityToken) ==
            featureFactory.searchFeatureProvider.getSettingsIntelligencePkgName(this)

    companion object {
        @VisibleForTesting
        fun tryLaunch(context: Context, keyString: String) {
            val key =
                try {
                    SpaSearchLandingKey.parseFrom(ByteString.copyFromUtf8(keyString))
                } catch (e: InvalidProtocolBufferException) {
                    Log.w(TAG, "arg key ($keyString) invalid", e)
                    return
                }

            if (key.hasSpaPage()) {
                val destination = key.spaPage.destination
                if (destination.isNotEmpty()) {
                    SpaDestination(destination = destination, highlightMenuKey = null)
                        .startFromExportedActivity(context)
                }
            }
            if (key.hasFragment()) {
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
