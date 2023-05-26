/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.applications.specialaccess

import android.content.Context
import android.net.NetworkPolicyManager
import android.os.UserHandle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.spa.framework.util.formatString
import com.android.settingslib.spaprivileged.model.app.AppListRepository
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DataSaverController(context: Context, key: String) : BasePreferenceController(context, key) {

    private lateinit var preference: Preference

    @AvailabilityStatus
    override fun getAvailabilityStatus(): Int = when {
        mContext.resources.getBoolean(R.bool.config_show_data_saver) -> AVAILABLE
        else -> UNSUPPORTED_ON_DEVICE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    fun init(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                preference.summary = getUnrestrictedSummary(mContext)
            }
        }
    }

    companion object {
        @VisibleForTesting
        suspend fun getUnrestrictedSummary(
            context: Context,
            appListRepository: AppListRepository =
                AppListRepositoryImpl(context.applicationContext),
        ) = context.formatString(
            R.string.data_saver_unrestricted_summary,
            "count" to getAllowCount(context.applicationContext, appListRepository),
        )

        private suspend fun getAllowCount(context: Context, appListRepository: AppListRepository) =
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val appsDeferred = async {
                        appListRepository.loadAndFilterApps(
                            userId = UserHandle.myUserId(),
                            isSystemApp = false,
                        )
                    }
                    val uidsAllowed = NetworkPolicyManager.from(context)
                        .getUidsWithPolicy(NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND)
                    appsDeferred.await().count { app -> app.uid in uidsAllowed }
                }
            }
    }
}