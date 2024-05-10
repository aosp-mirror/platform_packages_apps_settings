/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.applications.AppStorageSettings
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.template.app.getStorageSize

@Composable
fun AppStoragePreference(app: ApplicationInfo) {
    if (!app.hasFlag(ApplicationInfo.FLAG_INSTALLED)) return
    val context = LocalContext.current
    Preference(
        model = object : PreferenceModel {
            override val title = stringResource(R.string.storage_settings_for_app)
            override val summary = getSummary(context, app)
            override val onClick = { startStorageSettingsActivity(context, app) }
        },
        singleLineSummary = true,
    )
}

@Composable
private fun getSummary(context: Context, app: ApplicationInfo): () -> String {
    val sizeState = app.getStorageSize()
    return {
        val size = sizeState.value
        if (size.isBlank()) {
            context.getString(R.string.computing_size)
        } else {
            val storageType = context.getString(
                when (app.hasFlag(ApplicationInfo.FLAG_EXTERNAL_STORAGE)) {
                    true -> R.string.storage_type_external
                    false -> R.string.storage_type_internal
                }
            )
            context.getString(R.string.storage_summary_format, size, storageType)
        }
    }
}

private fun startStorageSettingsActivity(context: Context, app: ApplicationInfo) {
    AppInfoDashboardFragment.startAppInfoFragment(
        AppStorageSettings::class.java,
        app,
        context,
        AppInfoSettingsProvider.METRICS_CATEGORY,
    )
}
