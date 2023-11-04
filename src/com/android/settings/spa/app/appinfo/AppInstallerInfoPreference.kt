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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.applications.AppStoreUtil
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppInstallerInfoPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val presenter = remember { AppInstallerInfoPresenter(context, app, coroutineScope) }
    if (!presenter.isAvailableFlow.collectAsStateWithLifecycle(initialValue = false).value) return

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    val enabled by presenter.enabledFlow.collectAsStateWithLifecycle(initialValue = false)
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.app_install_details_title)
        override val summary = { summary }
        override val enabled = { enabled }
        override val onClick = presenter::startActivity
    })
}

private class AppInstallerInfoPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
    private val coroutineScope: CoroutineScope,
) {
    private val userContext = context.asUser(app.userHandle)
    private val packageManager = userContext.packageManager

    private val installerPackageFlow = flow {
        emit(withContext(Dispatchers.IO) {
            AppStoreUtil.getInstallerPackageName(userContext, app.packageName)
        })
    }.sharedFlow()

    private val installerLabelFlow = installerPackageFlow.map { installerPackage ->
        installerPackage ?: return@map null
        withContext(Dispatchers.IO) {
            Utils.getApplicationLabel(context, installerPackage)
        }
    }.sharedFlow()

    val isAvailableFlow = installerLabelFlow.map { installerLabel ->
        withContext(Dispatchers.IO) {
            !AppUtils.isMainlineModule(packageManager, app.packageName) &&
                    installerLabel != null
        }
    }

    val summaryFlow = installerLabelFlow.map { installerLabel ->
        val detailsStringId = when {
            app.isInstantApp -> R.string.instant_app_details_summary
            else -> R.string.app_install_details_summary
        }
        context.getString(detailsStringId, installerLabel)
    }

    private val intentFlow = installerPackageFlow.map { installerPackage ->
        withContext(Dispatchers.IO) {
            AppStoreUtil.getAppStoreLink(context, installerPackage, app.packageName)
        }
    }.sharedFlow()

    val enabledFlow = intentFlow.map { it != null }

    fun startActivity() {
        coroutineScope.launch {
            intentFlow.collect { intent ->
                if (intent != null) {
                    context.startActivityAsUser(intent, app.userHandle)
                }
            }
        }
    }

    private fun <T> Flow<T>.sharedFlow() =
        shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
}
