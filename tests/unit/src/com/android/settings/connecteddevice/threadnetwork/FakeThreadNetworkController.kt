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

import android.net.thread.ThreadNetworkController
import android.net.thread.ThreadNetworkException
import android.os.OutcomeReceiver
import java.util.concurrent.Executor

/** A fake implementation of [BaseThreadNetworkController] for unit tests. */
class FakeThreadNetworkController : BaseThreadNetworkController {
    var isEnabled = false
        private set
    var registeredStateCallback: ThreadNetworkController.StateCallback? = null
        private set

    override fun setEnabled(
        enabled: Boolean,
        executor: Executor,
        receiver: OutcomeReceiver<Void?, ThreadNetworkException>
    ) {
        isEnabled = enabled
        if (registeredStateCallback != null) {
            if (!isEnabled) {
                executor.execute {
                    registeredStateCallback!!.onThreadEnableStateChanged(
                        ThreadNetworkController.STATE_DISABLING
                    )
                }
                executor.execute {
                    registeredStateCallback!!.onThreadEnableStateChanged(
                        ThreadNetworkController.STATE_DISABLED
                    )
                }
            } else {
                executor.execute {
                    registeredStateCallback!!.onThreadEnableStateChanged(
                        ThreadNetworkController.STATE_ENABLED
                    )
                }
            }
        }
        executor.execute { receiver.onResult(null) }
    }

    override fun registerStateCallback(
        executor: Executor,
        callback: ThreadNetworkController.StateCallback
    ) {
        require(callback !== registeredStateCallback) { "callback is already registered" }
        registeredStateCallback = callback
        val enabledState =
            if (isEnabled) ThreadNetworkController.STATE_ENABLED else ThreadNetworkController.STATE_DISABLED
        executor.execute { registeredStateCallback!!.onThreadEnableStateChanged(enabledState) }
    }

    override fun unregisterStateCallback(callback: ThreadNetworkController.StateCallback) {
        requireNotNull(registeredStateCallback) { "callback is already unregistered" }
        registeredStateCallback = null
    }
}
