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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.button.ActionButtons

@Composable
fun ClonePageAppButtons(packageInfoPresenter: PackageInfoPresenter) {
    val presenter = remember { CloneAppButtonsPresenter(packageInfoPresenter) }
    ActionButtons(actionButtons = presenter.getActionButtons())
}

private class CloneAppButtonsPresenter(private val packageInfoPresenter: PackageInfoPresenter) {
    private val appLaunchButton = FakeAppLaunchButton(packageInfoPresenter)
    private val appCreateButton = AppCreateButton(packageInfoPresenter)
    private val appForceStopButton = FakeAppForceStopButton(packageInfoPresenter)

    @Composable
    fun getActionButtons() =
        packageInfoPresenter.flow.collectAsStateWithLifecycle(initialValue = null).value?.let {
            getActionButtons(checkNotNull(it.applicationInfo))
        } ?: emptyList()

    @Composable
    private fun getActionButtons(app: ApplicationInfo): List<ActionButton> = listOfNotNull(
            appLaunchButton.getActionButton(),
            appCreateButton.getActionButton(app),
            appForceStopButton.getActionButton(),
    )
}

class FakeAppForceStopButton(packageInfoPresenter: PackageInfoPresenter) {
    private val context = packageInfoPresenter.context

    fun getActionButton(): ActionButton {
        return ActionButton(
                text = context.getString(R.string.force_stop),
                imageVector = Icons.Outlined.WarningAmber,
                enabled = false,
        ) {
            // Unclickable
        }
    }
}

class FakeAppLaunchButton(packageInfoPresenter: PackageInfoPresenter) {
    private val context = packageInfoPresenter.context

    @Composable
    fun getActionButton(): ActionButton {
        return ActionButton(
                text = context.getString(R.string.launch_instant_app),
                imageVector = Icons.Outlined.Launch,
                enabled = false
        ) {
            // Unclickable
        }
    }
}
