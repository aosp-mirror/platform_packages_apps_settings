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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.thread.ThreadNetworkController
import android.net.thread.ThreadNetworkController.StateCallback
import android.net.thread.ThreadNetworkException
import android.net.thread.ThreadNetworkManager
import android.os.OutcomeReceiver
import android.provider.Settings
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.net.thread.platform.flags.Flags
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import java.util.concurrent.Executor

/** Controller for the "Thread" toggle in "Connected devices > Connection preferences".  */
class ThreadNetworkPreferenceController @VisibleForTesting constructor(
    context: Context,
    key: String,
    private val executor: Executor,
    private val threadController: BaseThreadNetworkController?
) : TogglePreferenceController(context, key), LifecycleEventObserver {
    private val stateCallback: StateCallback
    private val airplaneModeReceiver: BroadcastReceiver
    private var threadEnabled = false
    private var airplaneModeOn = false
    private var preference: Preference? = null

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

    constructor(context: Context, key: String) : this(
        context,
        key,
        ContextCompat.getMainExecutor(context),
        getThreadNetworkController(context)
    )

    init {
        stateCallback = newStateCallback()
        airplaneModeReceiver = newAirPlaneModeReceiver()
    }

    val isThreadSupportedOnDevice: Boolean
        get() = threadController != null

    private fun newStateCallback(): StateCallback {
        return object : StateCallback {
            override fun onThreadEnableStateChanged(enabledState: Int) {
                threadEnabled = enabledState == ThreadNetworkController.STATE_ENABLED
            }

            override fun onDeviceRoleChanged(role: Int) {}
        }
    }

    private fun newAirPlaneModeReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                airplaneModeOn = isAirplaneModeOn(context)
                Log.i(TAG, "Airplane mode is " + if (airplaneModeOn) "ON" else "OFF")
                preference?.let { preference -> updateState(preference) }
            }
        }
    }

    override fun getAvailabilityStatus(): Int {
        return if (!Flags.threadEnabledPlatform()) {
            CONDITIONALLY_UNAVAILABLE
        } else if (!isThreadSupportedOnDevice) {
            UNSUPPORTED_ON_DEVICE
        } else if (airplaneModeOn) {
            DISABLED_DEPENDENT_SETTING
        } else {
            AVAILABLE
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun isChecked(): Boolean {
        // TODO (b/322742298):
        // Check airplane mode here because it's planned to disable Thread state in airplane mode
        // (code in the mainline module). But it's currently not implemented yet (b/322742298).
        // By design, the toggle should be unchecked in airplane mode, so explicitly check the
        // airplane mode here to acchieve the same UX.
        return !airplaneModeOn && threadEnabled
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        if (threadController == null) {
            return false
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

    override fun onStateChanged(lifecycleOwner: LifecycleOwner, event: Lifecycle.Event) {
        if (threadController == null) {
            return
        }

        when (event) {
            Lifecycle.Event.ON_START -> {
                threadController.registerStateCallback(executor, stateCallback)
                airplaneModeOn = isAirplaneModeOn(mContext)
                mContext.registerReceiver(
                    airplaneModeReceiver,
                    IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                )
                preference?.let { preference -> updateState(preference) }
            }
            Lifecycle.Event.ON_STOP -> {
                threadController.unregisterStateCallback(stateCallback)
                mContext.unregisterReceiver(airplaneModeReceiver)
            }
            else -> {}
        }
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.isEnabled = !airplaneModeOn
        refreshSummary(preference)
    }

    override fun getSummary(): CharSequence {
        val resId: Int = if (airplaneModeOn) {
            R.string.thread_network_settings_summary_airplane_mode
        } else {
            R.string.thread_network_settings_summary
        }
        return mContext.getResources().getString(resId)
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_connected_devices
    }

    companion object {
        private const val TAG = "ThreadNetworkSettings"
        private fun getThreadNetworkController(context: Context): BaseThreadNetworkController? {
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

        private fun isAirplaneModeOn(context: Context): Boolean {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1
        }
    }
}
