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
package com.android.settings.network.telephony

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.core.AbstractPreferenceController
import com.google.common.collect.Sets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Session for controlling the status of TelephonyPreferenceController(s).
 *
 * Within this session, result of [BasePreferenceController.getAvailabilityStatus]
 * would be under control.
 */
class TelephonyStatusControlSession(
    private val controllers: Collection<AbstractPreferenceController>,
    lifecycle: Lifecycle,
) : AutoCloseable {
    private var job: Job? = null
    private val controllerSet = Sets.newConcurrentHashSet<TelephonyAvailabilityHandler>()

    init {
        job = lifecycle.coroutineScope.launch(Dispatchers.Default) {
            for (controller in controllers) {
                launch {
                    setupAvailabilityStatus(controller)
                }
            }
        }
    }

    /**
     * Close the session.
     *
     * No longer control the status.
     */
    override fun close() {
        job?.cancel()
        unsetAvailabilityStatus()
    }

    private suspend fun setupAvailabilityStatus(controller: AbstractPreferenceController): Boolean =
        try {
            if (controller is TelephonyAvailabilityHandler) {
                val status = (controller as BasePreferenceController).availabilityStatus
                yield() // prompt cancellation guarantee
                if (controllerSet.add(controller)) {
                    controller.setAvailabilityStatus(status)
                }
            }
            true
        } catch (exception: Exception) {
            Log.e(LOG_TAG, "Setup availability status failed!", exception)
            false
        }

    private fun unsetAvailabilityStatus() {
        for (controller in controllerSet) {
            controller.unsetAvailabilityStatus()
        }
    }

    companion object {
        private const val LOG_TAG = "TelephonyStatusControlSS"
    }
}
