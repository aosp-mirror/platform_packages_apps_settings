/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.UserHandle
import android.util.Log
import com.android.settingslib.spaprivileged.model.app.hasFlag
import com.android.settingslib.spaprivileged.model.app.isActiveAdmin
import com.android.settingslib.spaprivileged.model.app.userId
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine

class AppForceStopRepository(
    private val packageInfoPresenter: PackageInfoPresenter,
    private val appButtonRepository: AppButtonRepository =
        AppButtonRepository(packageInfoPresenter.context),
) {
    private val context = packageInfoPresenter.context

    /**
     * Flow of whether a package can be force stopped.
     */
    fun canForceStopFlow(): Flow<Boolean> = packageInfoPresenter.flow
        .map { packageInfo ->
            val app = packageInfo?.applicationInfo ?: return@map false
            canForceStop(app)
        }
        .conflate()
        .onEach { Log.d(TAG, "canForceStopFlow: $it") }
        .flowOn(Dispatchers.Default)

    /**
     * Gets whether a package can be force stopped.
     */
    private suspend fun canForceStop(app: ApplicationInfo): Boolean = when {
        // User can't force stop device admin.
        app.isActiveAdmin(context) -> false

        appButtonRepository.isDisallowControl(app) -> false

        // If the app isn't explicitly stopped, then always show the force stop button.
        !app.hasFlag(ApplicationInfo.FLAG_STOPPED) -> true

        else -> queryAppRestart(app)
    }

    /**
     * Queries if app has restarted.
     *
     * @return true means app can be force stop again.
     */
    private suspend fun queryAppRestart(app: ApplicationInfo): Boolean {
        val packageName = app.packageName
        val intent = Intent(
            Intent.ACTION_QUERY_PACKAGE_RESTART,
            Uri.fromParts("package", packageName, null)
        ).apply {
            putExtra(Intent.EXTRA_PACKAGES, arrayOf(packageName))
            putExtra(Intent.EXTRA_UID, app.uid)
            putExtra(Intent.EXTRA_USER_HANDLE, app.userId)
        }
        Log.d(TAG, "Sending broadcast to query restart status for $packageName")

        return suspendCancellableCoroutine { continuation ->
            val receiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val enabled = resultCode != Activity.RESULT_CANCELED
                    Log.d(TAG, "Got broadcast response: Restart status for $packageName $enabled")
                    continuation.resume(enabled)
                }
            }
            context.sendOrderedBroadcastAsUser(
                intent,
                UserHandle.CURRENT,
                Manifest.permission.HANDLE_QUERY_PACKAGE_RESTART,
                receiver,
                null,
                Activity.RESULT_CANCELED,
                null,
                null,
            )
        }
    }

    private companion object {
        private const val TAG = "AppForceStopRepository"
    }
}
