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

package com.android.settings.spa.app.appinfo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.userHandle

@Composable
fun TopBarAppLaunchButton(packageInfoPresenter: PackageInfoPresenter, app: ApplicationInfo) {
    val intent = packageInfoPresenter.launchIntent(app = app) ?: return
    IconButton({ launchButtonAction(intent, app, packageInfoPresenter) }) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Launch,
            contentDescription = stringResource(R.string.launch_instant_app),
        )
    }
}

private fun PackageInfoPresenter.launchIntent(
    app: ApplicationInfo
): Intent? {
    return userPackageManager.getLaunchIntentForPackage(app.packageName)
}

private fun launchButtonAction(
    intent: Intent,
    app: ApplicationInfo,
    packageInfoPresenter: PackageInfoPresenter
) {
    try {
        packageInfoPresenter.context.startActivityAsUser(intent, app.userHandle)
    } catch (_: ActivityNotFoundException) {
        // Only happens after package changes like uninstall, and before page auto refresh or
        // close, so ignore this exception is safe.
    }
}