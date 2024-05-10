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
import android.net.thread.ThreadNetworkController.StateCallback
import android.net.thread.ThreadNetworkException
import android.os.OutcomeReceiver
import androidx.annotation.VisibleForTesting
import java.util.concurrent.Executor

/**
 * A testable interface for [ThreadNetworkController] which is `final`.
 *
 * We are in a awkward situation that Android API guideline suggest `final` for API classes
 * while Robolectric test is being deprecated for platform testing (See
 * tests/robotests/new_tests_hook.sh). This force us to use "mockito-target-extended" but it's
 * conflicting with the default "mockito-target" which is somehow indirectly depended by the
 * `SettingsUnitTests` target.
 */
@VisibleForTesting
interface BaseThreadNetworkController {
    fun setEnabled(
        enabled: Boolean,
        executor: Executor,
        receiver: OutcomeReceiver<Void?, ThreadNetworkException>
    )

    fun registerStateCallback(executor: Executor, callback: StateCallback)

    fun unregisterStateCallback(callback: StateCallback)
}