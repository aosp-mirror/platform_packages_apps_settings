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

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import com.android.settings.R
import com.android.settings.applications.AppStoreUtil
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.model.app.userHandle

class AppInstallButton(private val packageInfoPresenter: PackageInfoPresenter) {
    private val context = packageInfoPresenter.context

    fun getActionButton(packageInfo: PackageInfo): ActionButton? {
        val app = packageInfo.applicationInfo
        if (!app.isInstantApp) return null

        return AppStoreUtil.getAppStoreLink(packageInfoPresenter.userContext, app.packageName)
            ?.let { intent -> installButton(intent, app) }
    }

    private fun installButton(intent: Intent, app: ApplicationInfo) = ActionButton(
        text = context.getString(R.string.install_text),
        imageVector = Icons.Outlined.FileDownload,
    ) {
        context.startActivityAsUser(intent, app.userHandle)
    }
}
