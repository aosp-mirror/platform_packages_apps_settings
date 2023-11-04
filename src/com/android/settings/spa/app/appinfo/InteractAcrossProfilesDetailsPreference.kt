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
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesDetails
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.common.crossProfileApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Composable
fun InteractAcrossProfilesDetailsPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember { InteractAcrossProfilesDetailsPresenter(context, app) }
    if (!presenter.isAvailableFlow.collectAsStateWithLifecycle(initialValue = false).value) return

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.interact_across_profiles_title)
        override val summary = { summary }
        override val onClick = presenter::startActivity
    })
}

private class InteractAcrossProfilesDetailsPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    private val crossProfileApps = context.crossProfileApps

    val isAvailableFlow = flow {
        emit(crossProfileApps.canUserAttemptToConfigureInteractAcrossProfiles(app.packageName))
    }.flowOn(Dispatchers.IO)

    val summaryFlow = flow {
        emit(InteractAcrossProfilesDetails.getPreferenceSummary(context, app.packageName))
    }.flowOn(Dispatchers.IO)

    fun startActivity() {
        AppInfoDashboardFragment.startAppInfoFragment(
            InteractAcrossProfilesDetails::class.java,
            app,
            context,
            AppInfoSettingsProvider.METRICS_CATEGORY,
        )
    }
}
