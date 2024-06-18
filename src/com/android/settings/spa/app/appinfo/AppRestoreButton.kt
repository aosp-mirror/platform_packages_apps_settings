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
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spaprivileged.framework.compose.DisposableBroadcastReceiverAsUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppRestoreButton(packageInfoPresenter: PackageInfoPresenter) {
    private companion object {
        private const val LOG_TAG = "AppRestoreButton"
        private const val INTENT_ACTION = "com.android.settings.unarchive.action"
    }

    private val context = packageInfoPresenter.context
    private val userPackageManager = packageInfoPresenter.userPackageManager
    private val packageInstaller = userPackageManager.packageInstaller
    private val packageName = packageInfoPresenter.packageName
    private val userHandle = UserHandle.of(packageInfoPresenter.userId)
    private var broadcastReceiverIsCreated = false
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var updateButtonTextJob: Job
    private val buttonTexts = intArrayOf(
        R.string.restore,
        R.string.restoring_step_one,
        R.string.restoring_step_two,
        R.string.restoring_step_three,
        R.string.restoring_step_four,
    )
    private var buttonTextIndexStateFlow = MutableStateFlow(0)

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton {
        if (!broadcastReceiverIsCreated) {
            val intentFilter = IntentFilter(INTENT_ACTION)
            DisposableBroadcastReceiverAsUser(intentFilter, userHandle) { intent ->
                if (app.packageName == intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)) {
                    onReceive(intent, app)
                }
            }
            broadcastReceiverIsCreated = true
        }
        coroutineScope = rememberCoroutineScope()
        if (app.isArchived && ::updateButtonTextJob.isInitialized && !updateButtonTextJob.isActive) {
            buttonTextIndexStateFlow.value = 0
        }
        return ActionButton(
            text = context.getString(
                buttonTexts[
                    buttonTextIndexStateFlow.asStateFlow().collectAsStateWithLifecycle(0).value]
            ),
            imageVector = Icons.Outlined.CloudDownload,
            enabled = app.isArchived && (!::updateButtonTextJob.isInitialized || !updateButtonTextJob.isActive)
        ) { onRestoreClicked(app) }
    }

    private fun onRestoreClicked(app: ApplicationInfo) {
        val intent = Intent(INTENT_ACTION)
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcastAsUser(
            context, 0, intent,
            // FLAG_MUTABLE is required by PackageInstaller#requestUnarchive
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE,
            userHandle
        )
        try {
            packageInstaller.requestUnarchive(app.packageName, pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Request unarchive failed", e)
            Toast.makeText(
                context,
                context.getString(R.string.restoring_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onReceive(intent: Intent, app: ApplicationInfo) {
        when (val unarchiveStatus =
            intent.getIntExtra(PackageInstaller.EXTRA_UNARCHIVE_STATUS, Int.MIN_VALUE)) {
            PackageInstaller.UNARCHIVAL_OK -> {
                // updateButtonTextJob will be canceled automatically once
                // AppButtonsPresenter#getActionButtons is triggered
                updateButtonTextJob = coroutineScope.launch {
                    while (isActive) {
                        var index = buttonTextIndexStateFlow.value
                        index = (index + 1) % buttonTexts.size
                        // The initial state shouldn't be used here
                        if (index == 0) index++
                        buttonTextIndexStateFlow.emit(index)
                        delay(1000)
                    }
                }
                val appLabel = userPackageManager.getApplicationLabel(app)
                Toast.makeText(
                    context,
                    context.getString(R.string.restoring_in_progress, appLabel),
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                Log.e(
                    LOG_TAG,
                    "Request unarchiving failed for $packageName with code $unarchiveStatus"
                )
                val errorDialogIntent =
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                if (errorDialogIntent != null) {
                    context.startActivityAsUser(errorDialogIntent, userHandle)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.restoring_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
