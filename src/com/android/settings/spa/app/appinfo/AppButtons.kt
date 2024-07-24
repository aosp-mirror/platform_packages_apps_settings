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

import android.content.pm.ApplicationInfo
import android.content.pm.FeatureFlags as PmFeatureFlags
import android.content.pm.FeatureFlagsImpl as PmFeatureFlagsImpl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.button.ActionButtons
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
/**
 * @param featureFlags can be overridden in tests
 */
fun AppButtons(
    packageInfoPresenter: PackageInfoPresenter,
    isHibernationSwitchEnabledStateFlow: MutableStateFlow<Boolean>,
    featureFlags: PmFeatureFlags = PmFeatureFlagsImpl()
) {
    if (remember(packageInfoPresenter) { packageInfoPresenter.isMainlineModule() }) return
    val presenter = remember {
        AppButtonsPresenter(
            packageInfoPresenter,
            isHibernationSwitchEnabledStateFlow,
            featureFlags
        )
    }
    ActionButtons(actionButtons = presenter.getActionButtons())
}

private fun PackageInfoPresenter.isMainlineModule(): Boolean =
    AppUtils.isMainlineModule(userPackageManager, packageName)

private class AppButtonsPresenter(
    private val packageInfoPresenter: PackageInfoPresenter,
    isHibernationSwitchEnabledStateFlow: MutableStateFlow<Boolean>,
    private val featureFlags: PmFeatureFlags
) {
    private val appLaunchButton = AppLaunchButton(packageInfoPresenter)
    private val appInstallButton = AppInstallButton(packageInfoPresenter)
    private val appDisableButton = AppDisableButton(packageInfoPresenter)
    private val appUninstallButton = AppUninstallButton(packageInfoPresenter)
    private val appClearButton = AppClearButton(packageInfoPresenter)
    private val appForceStopButton = AppForceStopButton(packageInfoPresenter)
    private val appArchiveButton =
        AppArchiveButton(packageInfoPresenter, isHibernationSwitchEnabledStateFlow)
    private val appRestoreButton = AppRestoreButton(packageInfoPresenter)

    @Composable
    fun getActionButtons() =
        packageInfoPresenter.flow.collectAsStateWithLifecycle(initialValue = null).value?.let {
            getActionButtons(checkNotNull(it.applicationInfo))
        } ?: emptyList()

    @Composable
    private fun getActionButtons(app: ApplicationInfo): List<ActionButton> = listOfNotNull(
        if (isArchivingEnabled(featureFlags)) {
            if (app.isArchived) {
                appRestoreButton.getActionButton(app)
            } else {
                appArchiveButton.getActionButton(app)
            }
        } else {
            appLaunchButton.getActionButton(app)
        },
        appInstallButton.getActionButton(app),
        appDisableButton.getActionButton(app),
        appUninstallButton.getActionButton(app),
        appClearButton.getActionButton(app),
        appForceStopButton.getActionButton(app),
    )
}
