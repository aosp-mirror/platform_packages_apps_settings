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

package com.android.settings.spa.app.appcompat

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.applications.appcompat.UserAspectRatioDetails
import com.android.settings.applications.appcompat.UserAspectRatioManager
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Composable
fun UserAspectRatioAppPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember { UserAspectRatioAppPresenter(context, app) }
    if (!presenter.isAvailableFlow.collectAsStateWithLifecycle(initialValue = false).value) return

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.aspect_ratio_experimental_title)
        override val summary = { summary }
        override val onClick = presenter::startActivity
    })
}

class UserAspectRatioAppPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    private val manager = UserAspectRatioManager(context)

    val isAvailableFlow = flow {
        emit(UserAspectRatioManager.isFeatureEnabled(context)
                && manager.canDisplayAspectRatioUi(app))
    }.flowOn(Dispatchers.IO)

    fun startActivity() =
        navigateToAppAspectRatioSettings(context, app, AppInfoSettingsProvider.METRICS_CATEGORY)

    val summaryFlow = flow {
        emit(manager.getUserMinAspectRatioEntry(app.packageName, context.userId))
    }.flowOn(Dispatchers.IO)
}

fun navigateToAppAspectRatioSettings(context: Context, app: ApplicationInfo, metricsCategory: Int) {
    AppInfoDashboardFragment.startAppInfoFragment(
        UserAspectRatioDetails::class.java,
        app,
        context,
        metricsCategory,
    )
}
