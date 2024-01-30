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

import android.app.settings.SettingsEnums
import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.android.settings.R
import com.android.settings.applications.manageapplications.CloneBackend
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider.getRoute
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.widget.button.ActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppCreateButton(packageInfoPresenter: PackageInfoPresenter) {
    private val context = packageInfoPresenter.context
    val enabledState = mutableStateOf(true)

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton? {
        return createButton(app)
    }

    @Composable
    private fun createButton(app: ApplicationInfo): ActionButton {
        val coroutineScope = rememberCoroutineScope()
        val navController = LocalNavController.current
        return ActionButton(
                text = context.getString(R.string.create),
                imageVector = Icons.Outlined.Add,
                enabled = enabledState.value,
        )
        {
            val cloneBackend = CloneBackend.getInstance(context)
            featureFactory.metricsFeatureProvider.action(context,
                    SettingsEnums.ACTION_CREATE_CLONE_APP)
            val appLabel = app.loadLabel(context.packageManager)
            Toast.makeText(context, context.getString(R.string.cloned_app_creation_toast_summary,
                appLabel),Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                enabledState.value = false
                val result = installCloneApp(app, cloneBackend)
                if (result == CloneBackend.SUCCESS) {
                    Toast.makeText(context,
                        context.getString(R.string.cloned_app_created_toast_summary, appLabel),
                            Toast.LENGTH_SHORT).show()
                    navController.navigate(getRoute(app.packageName, cloneBackend.cloneUserId),
                            /* popUpCurrent*/ true)
                } else {
                    enabledState.value = true
                }
            }
        }
    }

    private suspend fun installCloneApp(app: ApplicationInfo, cloneBackend: CloneBackend): Int = withContext(Dispatchers.IO) {
        cloneBackend.installCloneApp(app.packageName)
    }
}