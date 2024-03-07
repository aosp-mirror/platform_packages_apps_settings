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
import android.content.pm.PackageManager.ResolveInfoFlags
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.applications.AppLocaleUtil
import com.android.settings.applications.appinfo.AppLocaleDetails
import com.android.settings.localepicker.AppLocalePickerActivity
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

@Composable
fun AppLocalePreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember { AppLocalePresenter(context, app) }
    if (!presenter.isAvailableFlow.collectAsStateWithLifecycle(initialValue = false).value) return

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.app_locale_preference_title)
        override val summary = { summary }
        override val onClick = presenter::startActivity
    })
}

private class AppLocalePresenter(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    private val packageManager = context.packageManager

    val isAvailableFlow = flow { emit(isAvailable()) }

    private suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val resolveInfos = packageManager.queryIntentActivitiesAsUser(
            AppLocaleUtil.LAUNCHER_ENTRY_INTENT,
            ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            app.userId,
        )
        AppLocaleUtil.canDisplayLocaleUi(context, app, resolveInfos)
    }

    val summaryFlow = flow { emit(getSummary()) }

    private suspend fun getSummary() = withContext(Dispatchers.IO) {
        AppLocaleDetails.getSummary(context, app).toString()
    }

    fun startActivity() {
        val intent = Intent(context, AppLocalePickerActivity::class.java).apply {
            data = Uri.parse("package:" + app.packageName)
            putExtra(AppInfoBase.ARG_PACKAGE_UID, app.uid)
        }
        context.startActivityAsUser(intent, app.userHandle)
    }
}
