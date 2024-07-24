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

package com.android.settings.system

import android.content.Context
import android.os.Build
import android.os.SystemUpdateManager
import android.os.UserManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlinx.coroutines.launch

open class SystemUpdatePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {
    private val userManager: UserManager = context.userManager
    private val systemUpdateRepository = SystemUpdateRepository(context)
    private val clientInitiatedActionRepository = ClientInitiatedActionRepository(context)
    private lateinit var preference: Preference

    override fun getAvailabilityStatus() =
        if (mContext.resources.getBoolean(R.bool.config_show_system_update_settings) &&
            userManager.isAdminUser
        ) AVAILABLE else UNSUPPORTED_ON_DEVICE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
        if (isAvailable) {
            val intent = systemUpdateRepository.getSystemUpdateIntent()
            if (intent != null) {  // Replace the intent with this specific activity
                preference.intent = intent
            } else { // Did not find a matching activity, so remove the preference
                screen.removePreference(preference)
            }
        }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey == preference.key) {
            clientInitiatedActionRepository.onSystemUpdate()
        }
        // always return false here because this handler does not want to block other handlers.
        return false
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                preference.summary = calculateSummary()
            }
        }
    }

    private suspend fun calculateSummary(): String {
        val updateInfo = mContext.getSystemUpdateInfo() ?: return getReleaseVersionSummary()

        val status = updateInfo.getInt(SystemUpdateManager.KEY_STATUS)
        if (status == SystemUpdateManager.STATUS_UNKNOWN) {
            Log.d(TAG, "Update statue unknown")
        }
        when (status) {
            SystemUpdateManager.STATUS_WAITING_DOWNLOAD,
            SystemUpdateManager.STATUS_IN_PROGRESS,
            SystemUpdateManager.STATUS_WAITING_INSTALL,
            SystemUpdateManager.STATUS_WAITING_REBOOT -> {
                return mContext.getString(R.string.android_version_pending_update_summary)
            }

            SystemUpdateManager.STATUS_IDLE,
            SystemUpdateManager.STATUS_UNKNOWN -> {
                val version = updateInfo.getString(SystemUpdateManager.KEY_TITLE)
                if (!version.isNullOrEmpty()) {
                    return mContext.getString(R.string.android_version_summary, version)
                }
            }
        }
        return getReleaseVersionSummary()
    }

    private fun getReleaseVersionSummary(): String = mContext.getString(
        R.string.android_version_summary,
        Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY,
    )

    companion object {
        private const val TAG = "SysUpdatePrefContr"
    }
}
