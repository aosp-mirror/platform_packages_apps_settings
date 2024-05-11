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
import android.net.thread.ThreadNetworkException
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settings.flags.Flags
import java.util.concurrent.Executor

/**
 * Controller for the "Use Thread" toggle in "Connected devices > Connection preferences > Thread".
 */
class ThreadNetworkToggleController @VisibleForTesting constructor(
    context: Context,
    key: String,
    private val executor: Executor,
    private val threadController: BaseThreadNetworkController?
) : TogglePreferenceController(context, key), LifecycleEventObserver {
    private val stateCallback: StateCallback
    private var threadEnabled = false
    private var preference: Preference? = null

    constructor(context: Context, key: String) : this(
        context,
        key,
        ContextCompat.getMainExecutor(context),
        ThreadNetworkUtils.getThreadNetworkController(context)
    )

    init {
        stateCallback = newStateCallback()
    }

    val isThreadSupportedOnDevice: Boolean
        get() = threadController != null

    private fun newStateCallback(): StateCallback {
        return object : StateCallback {
            override fun onThreadEnableStateChanged(enabledState: Int) {
                threadEnabled = enabledState == ThreadNetworkController.STATE_ENABLED
                preference?.let { preference -> updateState(preference) }
            }

            override fun onDeviceRoleChanged(role: Int) {}
        }
    }

    override fun getAvailabilityStatus(): Int {
        return if (!Flags.threadSettingsEnabled()) {
            CONDITIONALLY_UNAVAILABLE
        } else if (!isThreadSupportedOnDevice) {
            UNSUPPORTED_ON_DEVICE
        } else {
            AVAILABLE
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun isChecked(): Boolean {
        return threadEnabled
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        if (threadController == null) {
            return false
        }

        // Avoids dead loop of setChecked -> threadController.setEnabled() ->
        // StateCallback.onThreadEnableStateChanged -> updateState -> setChecked
        if (isChecked == isChecked()) {
            return true
        }

        val action = if (isChecked) "enable" else "disable"
        threadController.setEnabled(
            isChecked,
            executor,
            object : OutcomeReceiver<Void?, ThreadNetworkException> {
                override fun onError(e: ThreadNetworkException) {
                    // TODO(b/327549838): gracefully handle the failure by resetting the UI state
                    Log.e(TAG, "Failed to $action Thread", e)
                }

                override fun onResult(unused: Void?) {
                    Log.d(TAG, "Successfully $action Thread")
                }
            })
        return true
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (threadController == null) {
            return
        }

        when (event) {
            Lifecycle.Event.ON_START -> {
                threadController.registerStateCallback(executor, stateCallback)
            }

            Lifecycle.Event.ON_STOP -> {
                threadController.unregisterStateCallback(stateCallback)
            }

            else -> {}
        }
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_connected_devices
    }

    companion object {
        private const val TAG = "ThreadNetworkSettings"
    }
}
