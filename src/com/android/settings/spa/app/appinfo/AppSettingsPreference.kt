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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.ResolveInfoFlags
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.spa.framework.compose.collectAsStateWithLifecycle
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppSettingsPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val presenter = remember { AppSettingsPresenter(context, app, coroutineScope) }
    if (!presenter.isAvailableFlow.collectAsStateWithLifecycle(initialValue = false).value) return

    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.app_settings_link)
        override val onClick = presenter::startActivity
    })
}

private class AppSettingsPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
    private val coroutineScope: CoroutineScope,
) {
    private val packageManager = context.packageManager

    private val intentFlow = flow {
        emit(resolveIntent())
    }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)

    val isAvailableFlow = intentFlow.map { it != null }

    fun startActivity() {
        coroutineScope.launch {
            intentFlow.collect { intent ->
                if (intent != null) {
                    FeatureFactory.getFactory(context).metricsFeatureProvider
                        .action(
                            SettingsEnums.PAGE_UNKNOWN,
                            SettingsEnums.ACTION_OPEN_APP_SETTING,
                            AppInfoSettingsProvider.METRICS_CATEGORY,
                            null,
                            0,
                        )
                    context.startActivityAsUser(intent, app.userHandle)
                }
            }
        }
    }

    private suspend fun resolveIntent(): Intent? = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_APPLICATION_PREFERENCES).apply {
            `package` = app.packageName
        }
        packageManager.resolveActivityAsUser(intent, ResolveInfoFlags.of(0), app.userId)
            ?.activityInfo
            ?.let { activityInfo ->
                Intent(intent.action).apply {
                    setClassName(activityInfo.packageName, activityInfo.name)
                }
            }
    }
}
