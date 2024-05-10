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

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.liveData
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.app.userHandle
import com.android.settingslib.spaprivileged.model.app.userId
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class DefaultAppShortcut(
    val roleName: String,
    @StringRes val titleResId: Int,
)

@Composable
fun DefaultAppShortcutPreference(shortcut: DefaultAppShortcut, app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember(shortcut.roleName, app) {
        DefaultAppShortcutPresenter(context, shortcut.roleName, app)
    }
    if (remember(presenter) { !presenter.isAvailable() }) return
    if (presenter.isVisible().observeAsState().value != true) return

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(shortcut.titleResId)
        override val summary = { summary }
        override val onClick = presenter::startActivity
    })
}

private class DefaultAppShortcutPresenter(
    private val context: Context,
    private val roleName: String,
    private val app: ApplicationInfo,
) {
    private val roleManager = context.getSystemService(RoleManager::class.java)!!
    private val executor = Dispatchers.IO.asExecutor()

    fun isAvailable() = !context.userManager.isManagedProfile(app.userId)

    fun isVisible() = liveData {
        coroutineScope {
            val roleVisible = async { isRoleVisible() }
            val applicationVisibleForRole = async { isApplicationVisibleForRole() }
            emit(roleVisible.await() && applicationVisibleForRole.await())
        }
    }

    private suspend fun isRoleVisible(): Boolean {
        return suspendCoroutine { continuation ->
            roleManager.isRoleVisible(roleName, executor) {
                continuation.resume(it)
            }
        }
    }

    private suspend fun isApplicationVisibleForRole() = suspendCoroutine { continuation ->
        roleManager.isApplicationVisibleForRole(roleName, app.packageName, executor) {
            continuation.resume(it)
        }
    }

    val summaryFlow = flow { emit(getSummary()) }.flowOn(Dispatchers.IO)

    private fun getSummary(): String {
        val defaultApp = roleManager.getRoleHoldersAsUser(roleName, app.userHandle).firstOrNull()
        return context.getString(
            if (defaultApp == app.packageName) R.string.yes else R.string.no
        )
    }

    fun startActivity() {
        val intent = Intent(Intent.ACTION_MANAGE_DEFAULT_APP).apply {
            putExtra(Intent.EXTRA_ROLE_NAME, roleName)
        }
        context.startActivityAsUser(intent, app.userHandle)
    }
}
