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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.resolveActionForApp
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

@Composable
fun AppAllServicesPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val presenter = remember { AppAllServicesPresenter(context, app, coroutineScope) }
    if (!presenter.isAvailableFlow.collectAsStateWithLifecycle(initialValue = false).value) return

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.app_info_all_services_label)
        override val summary = { summary }
        override val onClick = presenter::startActivity
    })
}

private class AppAllServicesPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
    private val coroutineScope: CoroutineScope,
) {
    private val packageManager = context.packageManager

    private val activityInfoFlow = flow {
        emit(packageManager.resolveActionForApp(
            app = app,
            action = Intent.ACTION_VIEW_APP_FEATURES,
            flags = PackageManager.GET_META_DATA,
        ))
    }.shareIn(coroutineScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), 1)

    val isAvailableFlow = activityInfoFlow.map { it != null }

    val summaryFlow = activityInfoFlow.map { activityInfo ->
        activityInfo?.metaData?.getSummary() ?: ""
    }.flowOn(Dispatchers.IO)

    private fun Bundle.getSummary(): String {
        val resources = try {
            packageManager.getResourcesForApplication(app)
        } catch (exception: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Name not found for the application.")
            return ""
        }

        return try {
            resources.getString(getInt(SUMMARY_METADATA_KEY))
        } catch (exception: Resources.NotFoundException) {
            Log.d(TAG, "Resource not found for summary string.")
            ""
        }
    }

    fun startActivity() {
        coroutineScope.launch {
            activityInfoFlow.firstOrNull()?.let { activityInfo ->
                val intent = Intent(Intent.ACTION_VIEW_APP_FEATURES).apply {
                    component = activityInfo.componentName
                }
                context.startActivityAsUser(intent, app.userHandle)
            }
        }
    }

    companion object {
        private const val TAG = "AppAllServicesPresenter"
        private const val SUMMARY_METADATA_KEY = "app_features_preference_summary"
    }
}
