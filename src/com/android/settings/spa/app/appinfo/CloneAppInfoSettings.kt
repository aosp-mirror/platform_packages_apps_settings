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
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spaprivileged.model.app.toRoute
import com.android.settingslib.spaprivileged.template.app.AppInfoProvider

private const val PACKAGE_NAME = "packageName"
private const val USER_ID = "userId"

object CloneAppInfoSettingsProvider : SettingsPageProvider {
    override val name = "CloneAppInfoSettingsProvider"

    override val parameter = listOf(
            navArgument(PACKAGE_NAME) { type = NavType.StringType },
            navArgument(USER_ID) { type = NavType.IntType },
    )

    @Composable
    override fun Page(arguments: Bundle?) {
        val packageName = arguments!!.getString(PACKAGE_NAME)!!
        val userId = arguments.getInt(USER_ID)
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val packageInfoPresenter = remember {
            PackageInfoPresenter(context, packageName, userId, coroutineScope)
        }
        CloneAppInfoSettings(packageInfoPresenter)
        packageInfoPresenter.PackageFullyRemovedEffect()
    }

    @Composable
    fun navigator(app: ApplicationInfo) = com.android.settingslib.spa.framework.compose.navigator(route = "$name/${app.toRoute()}")

    /**
     * Gets the route to the App Info Settings page.
     *
     * Expose route to enable enter from non-SPA pages.
     */
    fun getRoute(packageName: String, userId: Int): String = "$name/$packageName/$userId"
}

@Composable
private fun CloneAppInfoSettings(packageInfoPresenter: PackageInfoPresenter) {
    val packageInfo = packageInfoPresenter.flow.collectAsStateWithLifecycle().value ?: return
    RegularScaffold(
            title = stringResource(R.string.application_info_label),
    ) {
        val appInfoProvider = remember { AppInfoProvider(packageInfo) }

        appInfoProvider.AppInfo(isClonedAppPage = true)
        ClonePageAppButtons(packageInfoPresenter)
    }
}
