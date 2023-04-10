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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Launch
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.model.app.userHandle

class AppLaunchButton(packageInfoPresenter: PackageInfoPresenter) {
    private val context = packageInfoPresenter.context
    private val userPackageManager = packageInfoPresenter.userPackageManager

    fun getActionButton(app: ApplicationInfo): ActionButton? =
        userPackageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
            launchButton(intent, app)
        }

    private fun launchButton(intent: Intent, app: ApplicationInfo) = ActionButton(
        text = context.getString(R.string.launch_instant_app),
        imageVector = Icons.Outlined.Launch,
    ) {
        try {
            context.startActivityAsUser(intent, app.userHandle)
        } catch (_: ActivityNotFoundException) {
            // Only happens after package changes like uninstall, and before page auto refresh or
            // close, so ignore this exception is safe.
        }
    }
}
