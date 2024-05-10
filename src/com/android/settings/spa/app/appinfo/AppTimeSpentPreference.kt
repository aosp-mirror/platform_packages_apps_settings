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
import android.content.pm.PackageManager.ResolveInfoFlags
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.liveData
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.Dispatchers

@Composable
fun AppTimeSpentPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember(app) { AppTimeSpentPresenter(context, app) }
    if (!presenter.isAvailable()) return

    val summary by presenter.summaryLiveData.observeAsState(
        initial = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.time_spent_in_app_pref_title)
        override val summary = { summary }
        override val enabled = { presenter.isEnabled() }
        override val onClick = presenter::startActivity
    })
}

private class AppTimeSpentPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    private val intent = Intent(Settings.ACTION_APP_USAGE_SETTINGS).apply {
        putExtra(Intent.EXTRA_PACKAGE_NAME, app.packageName)
    }
    private val appFeatureProvider = featureFactory.applicationFeatureProvider

    fun isAvailable() = context.packageManager.queryIntentActivitiesAsUser(
        intent, ResolveInfoFlags.of(0), app.userId
    ).any { resolveInfo ->
        resolveInfo?.activityInfo?.applicationInfo?.isSystemApp == true
    }

    fun isEnabled() = app.hasFlag(ApplicationInfo.FLAG_INSTALLED)

    val summaryLiveData = liveData(Dispatchers.IO) {
        emit(appFeatureProvider.getTimeSpentInApp(app.packageName).toString())
    }

    fun startActivity() {
        context.startActivityAsUser(intent, app.userHandle)
    }
}
