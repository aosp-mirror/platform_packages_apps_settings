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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.intentpicker.AppLaunchSettings
import com.android.settings.applications.intentpicker.IntentPickerUtils
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.framework.common.domainVerificationManager
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Composable
fun AppOpenByDefaultPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember(app) { AppOpenByDefaultPresenter(context, app) }
    if (remember(presenter) { !presenter.isAvailable() }) return

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.launch_by_default)
        override val summary = { summary }
        override val enabled = { presenter.isEnabled() }
        override val onClick = presenter::startActivity
    })
}

private class AppOpenByDefaultPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    private val domainVerificationManager = context.asUser(app.userHandle).domainVerificationManager

    fun isAvailable() =
        !app.isInstantApp && !AppUtils.isBrowserApp(context, app.packageName, app.userId)

    fun isEnabled() = app.hasFlag(ApplicationInfo.FLAG_INSTALLED) && app.enabled

    val summaryFlow = flow { emit(getSummary()) }.flowOn(Dispatchers.IO)

    private fun getSummary() = context.getString(
        when {
            isLinkHandlingAllowed() -> R.string.app_link_open_always
            else -> R.string.app_link_open_never
        }
    )

    private fun isLinkHandlingAllowed(): Boolean {
        val userState = IntentPickerUtils.getDomainVerificationUserState(
            domainVerificationManager, app.packageName
        )
        return userState?.isLinkHandlingAllowed ?: false
    }

    fun startActivity() {
        AppInfoDashboardFragment.startAppInfoFragment(
            AppLaunchSettings::class.java,
            app,
            context,
            AppInfoSettingsProvider.METRICS_CATEGORY,
        )
    }
}
