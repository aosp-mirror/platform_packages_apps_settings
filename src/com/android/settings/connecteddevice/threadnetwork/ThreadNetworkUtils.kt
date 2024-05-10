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

package com.android.settings.connecteddevice.threadnetwork

import android.content.Context
import android.content.pm.PackageManager
import android.net.thread.ThreadNetworkController
import android.net.thread.ThreadNetworkController.StateCallback
import android.net.thread.ThreadNetworkException
import android.net.thread.ThreadNetworkManager
import android.os.OutcomeReceiver
import androidx.annotation.VisibleForTesting
import java.util.concurrent.Executor

/** Common utilities for Thread settings classes. */
object ThreadNetworkUtils {
    /**
     * Retrieves the [BaseThreadNetworkController] instance that is backed by the Android
     * [ThreadNetworkController].
     */
    fun getThreadNetworkController(context: Context): BaseThreadNetworkController? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_THREAD_NETWORK)) {
            return null
        }
        val manager = context.getSystemService(ThreadNetworkManager::class.java) ?: return null
        val controller = manager.allThreadNetworkControllers[0]
        return object : BaseThreadNetworkController {
            override fun setEnabled(
                enabled: Boolean,
                executor: Executor,
                receiver: OutcomeReceiver<Void?, ThreadNetworkException>
            ) {
                controller.setEnabled(enabled, executor, receiver)
            }

            override fun registerStateCallback(executor: Executor, callback: StateCallback) {
                controller.registerStateCallback(executor, callback)
            }

            override fun unregisterStateCallback(callback: StateCallback) {
                controller.unregisterStateCallback(callback)
            }
        }
    }
}