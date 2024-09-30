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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.format.DateFormat
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.utils.getLocale
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

// LINT.IfChange
class MainlineModuleVersionPreference :
    PreferenceMetadata,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceBinding {

    private var moduleVersion: String? = null

    override val key: String
        get() = "module_version"

    override val title: Int
        get() = R.string.module_version

    override fun getSummary(context: Context): CharSequence? {
        val version = getModuleVersion(context)
        if (version.isEmpty()) return null

        val locale = context.getLocale()
        fun parseDate(pattern: String): Date? {
            val simpleDateFormat = SimpleDateFormat(pattern, locale)
            simpleDateFormat.timeZone = TimeZone.getDefault()
            return try {
                simpleDateFormat.parse(version)
            } catch (e: ParseException) {
                null
            }
        }

        val date = parseDate("yyyy-MM-dd") ?: parseDate("yyyy-MM")
        return if (date == null) {
            Log.w(TAG, "Cannot parse mainline versionName ($version) as date")
            version
        } else {
            DateFormat.format(DateFormat.getBestDateTimePattern(locale, "dMMMMyyyy"), date)
        }
    }

    override fun intent(context: Context): Intent? {
        val packageManager = context.packageManager
        val intentPackage =
            if (Flags.mainlineModuleExplicitIntent()) {
                context.getString(R.string.config_mainline_module_update_package)
            } else {
                null
            }
        fun String.resolveIntent() =
            Intent(this).let {
                if (intentPackage != null) it.setPackage(intentPackage)
                if (packageManager.resolveActivity(it, 0) != null) it else null
            }

        return MODULE_UPDATE_ACTION_V2.resolveIntent() ?: MODULE_UPDATE_ACTION.resolveIntent()
    }

    override fun isAvailable(context: Context) = getModuleVersion(context).isNotEmpty()

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // This seems unnecessary, just follow existing behavior to pass test
        if (preference.intent == null) preference.setSummary(R.string.summary_placeholder)
        preference.isCopyingEnabled = true
    }

    private fun getModuleVersion(context: Context): String =
        moduleVersion ?: context.getVersion().also { moduleVersion = it }

    private fun Context.getVersion(): String {
        val moduleProvider =
            getString(com.android.internal.R.string.config_defaultModuleMetadataProvider)
        if (moduleProvider.isEmpty()) return ""
        return try {
            packageManager.getPackageInfo(moduleProvider, 0)?.versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to get mainline version.", e)
            ""
        }
    }

    companion object {
        private const val TAG = "MainlineModulePreference"
        const val MODULE_UPDATE_ACTION = "android.settings.MODULE_UPDATE_SETTINGS"
        const val MODULE_UPDATE_ACTION_V2 = "android.settings.MODULE_UPDATE_VERSIONS"
    }
}
// LINT.ThenChange(MainlineModuleVersionPreferenceController.java)
