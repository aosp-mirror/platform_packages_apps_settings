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
import android.net.thread.ThreadNetworkController
import android.net.thread.ThreadNetworkController.StateCallback
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags
import java.util.concurrent.Executor

/**
 * The fragment controller for Thread settings in
 * "Connected devices > Connection preferences > Thread".
 */
class ThreadNetworkFragmentController @VisibleForTesting constructor(
    context: Context,
    preferenceKey: String,
    private val executor: Executor,
    private val threadController: BaseThreadNetworkController?
) : BasePreferenceController(context, preferenceKey), LifecycleEventObserver {
    private val stateCallback: StateCallback
    private var threadEnabled = false
    private var preference: Preference? = null

    constructor(context: Context, preferenceKey: String) : this(
        context,
        preferenceKey,
        ContextCompat.getMainExecutor(context),
        ThreadNetworkUtils.getThreadNetworkController(context)
    )

    init {
        stateCallback = newStateCallback()
    }

    override fun getAvailabilityStatus(): Int {
        return if (!Flags.threadSettingsEnabled()) {
            CONDITIONALLY_UNAVAILABLE
        } else if (threadController == null) {
            UNSUPPORTED_ON_DEVICE
        } else {
            AVAILABLE
        }
    }

    override fun getSummary(): CharSequence {
        return if (threadEnabled) {
            mContext.getText(R.string.switch_on_text)
        } else {
            mContext.getText(R.string.switch_off_text)
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (threadController == null) {
            return
        }

        when (event) {
            Lifecycle.Event.ON_START ->
                threadController.registerStateCallback(executor, stateCallback)

            Lifecycle.Event.ON_STOP ->
                threadController.unregisterStateCallback(stateCallback)

            else -> {}
        }
    }

    private fun newStateCallback(): StateCallback {
        return object : StateCallback {
            override fun onThreadEnableStateChanged(enabledState: Int) {
                threadEnabled = enabledState == ThreadNetworkController.STATE_ENABLED
                preference?.let { preference -> refreshSummary(preference) }
            }

            override fun onDeviceRoleChanged(role: Int) {}
        }
    }
}
