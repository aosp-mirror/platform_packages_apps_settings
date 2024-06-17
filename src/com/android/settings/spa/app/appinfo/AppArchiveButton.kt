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

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.framework.compose.DisposableBroadcastReceiverAsUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AppArchiveButton(
    packageInfoPresenter: PackageInfoPresenter,
    private val isHibernationSwitchEnabledStateFlow: MutableStateFlow<Boolean>,
) {
    private companion object {
        private const val LOG_TAG = "AppArchiveButton"
        private const val INTENT_ACTION = "com.android.settings.archive.action"
    }

    private val context = packageInfoPresenter.context
    private val appButtonRepository = AppButtonRepository(context)
    private val userPackageManager = packageInfoPresenter.userPackageManager
    private val packageInstaller = userPackageManager.packageInstaller
    private val packageName = packageInfoPresenter.packageName
    private val userHandle = UserHandle.of(packageInfoPresenter.userId)
    private var broadcastReceiverIsCreated = false
    private lateinit var appLabel: CharSequence

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton {
        if (!broadcastReceiverIsCreated) {
            val intentFilter = IntentFilter(INTENT_ACTION)
            DisposableBroadcastReceiverAsUser(intentFilter, userHandle) { intent ->
                if (app.packageName == intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)) {
                    onReceive(intent)
                }
            }
            broadcastReceiverIsCreated = true
        }
        appLabel = userPackageManager.getApplicationLabel(app)
        return ActionButton(
            text = context.getString(R.string.archive),
            imageVector = Icons.Outlined.CloudUpload,
            enabled = remember(app) {
                isHibernationSwitchEnabledStateFlow.asStateFlow().map {
                    it && isActionButtonEnabledForApp(app)
                }.flowOn(Dispatchers.Default)
            }.collectAsStateWithLifecycle(false).value
        ) { onArchiveClicked(app) }
    }

    private fun isActionButtonEnabledForApp(app: ApplicationInfo): Boolean {
        return app.isActionButtonEnabled() && appButtonRepository.isAllowUninstallOrArchive(
            context,
            app
        )
    }

    private fun ApplicationInfo.isActionButtonEnabled(): Boolean {
        return !isArchived
            && userPackageManager.isAppArchivable(packageName)
            // We apply the same device policy for both the uninstallation and archive
            // button.
            && !appButtonRepository.isUninstallBlockedByAdmin(this)
    }

    private fun onArchiveClicked(app: ApplicationInfo) {
        val intent = Intent(INTENT_ACTION)
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcastAsUser(
            context, 0, intent,
            // FLAG_MUTABLE is required by PackageInstaller#requestArchive
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE,
            userHandle
        )
        try {
            packageInstaller.requestArchive(app.packageName, pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Request archive failed", e)
            Toast.makeText(
                context,
                context.getString(R.string.archiving_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onReceive(intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)) {
            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.archiving_succeeded, appLabel),
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                Log.e(LOG_TAG, "Request archiving failed for $packageName with code $status")
                Toast.makeText(
                    context,
                    context.getString(R.string.archiving_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
