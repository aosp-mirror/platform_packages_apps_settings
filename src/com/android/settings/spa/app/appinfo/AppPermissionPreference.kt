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

package com.android.settings.spa.app.appinfo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.compose.placeholder
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlinx.coroutines.flow.Flow

private const val TAG = "AppPermissionPreference"
private const val EXTRA_HIDE_INFO_BUTTON = "hideInfoButton"

@Composable
fun AppPermissionPreference(
    app: ApplicationInfo,
    summaryFlow: Flow<AppPermissionSummaryState> = rememberAppPermissionSummary(app),
) {
    val context = LocalContext.current
    val summaryState = summaryFlow.collectAsStateWithLifecycle(
        initialValue = AppPermissionSummaryState(summary = placeholder(), enabled = false),
    )
    Preference(
        model = remember {
            object : PreferenceModel {
                override val title = context.getString(R.string.permissions_label)
                override val summary = { summaryState.value.summary }
                override val enabled = { summaryState.value.enabled }
                override val onClick = { startManagePermissionsActivity(context, app) }
            }
        },
        singleLineSummary = true,
    )
}

/** Starts new activity to manage app permissions */
private fun startManagePermissionsActivity(context: Context, app: ApplicationInfo) {
    val intent = Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS).apply {
        putExtra(Intent.EXTRA_PACKAGE_NAME, app.packageName)
        putExtra(EXTRA_HIDE_INFO_BUTTON, true)
    }
    try {
        context.startActivityAsUser(intent, app.userHandle)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS")
    }
}
