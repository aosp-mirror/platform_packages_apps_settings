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
import android.net.thread.ThreadNetworkController.STATE_DISABLED
import android.net.thread.ThreadNetworkController.STATE_DISABLING
import android.net.thread.ThreadNetworkController.STATE_ENABLED
import android.net.thread.ThreadNetworkController.StateCallback
import android.net.thread.ThreadNetworkException
import android.os.OutcomeReceiver
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.net.thread.platform.flags.Flags
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING
import com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE
import com.android.settings.connecteddevice.threadnetwork.ThreadNetworkPreferenceController.BaseThreadNetworkController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.Executor

/** Unit tests for [ThreadNetworkPreferenceController].  */
@RunWith(AndroidJUnit4::class)
class ThreadNetworkPreferenceControllerTest {
    @get:Rule
    val mSetFlagsRule = SetFlagsRule()
    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var controller: ThreadNetworkPreferenceController
    private lateinit var fakeThreadNetworkController: FakeThreadNetworkController
    private lateinit var preference: SwitchPreference
    private val broadcastReceiverArgumentCaptor = ArgumentCaptor.forClass(
        BroadcastReceiver::class.java
    )

    @Before
    fun setUp() {
        mSetFlagsRule.enableFlags(Flags.FLAG_THREAD_ENABLED_PLATFORM)
        context = spy(ApplicationProvider.getApplicationContext<Context>())
        executor = ContextCompat.getMainExecutor(context)
        fakeThreadNetworkController = FakeThreadNetworkController(executor)
        controller = newControllerWithThreadFeatureSupported(true)
        val preferenceManager = PreferenceManager(context)
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preference = SwitchPreference(context)
        preference.key = "thread_network_settings"
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)

        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
    }

    private fun newControllerWithThreadFeatureSupported(
        present: Boolean
    ): ThreadNetworkPreferenceController {
        return ThreadNetworkPreferenceController(
            context,
            "thread_network_settings" /* key */,
            executor,
            if (present) fakeThreadNetworkController else null
        )
    }

    @Test
    fun availabilityStatus_flagDisabled_returnsConditionallyUnavailable() {
        mSetFlagsRule.disableFlags(Flags.FLAG_THREAD_ENABLED_PLATFORM)
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun availabilityStatus_airPlaneModeOn_returnsDisabledDependentSetting() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        assertThat(controller.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING)
    }

    @Test
    fun availabilityStatus_airPlaneModeOff_returnsAvailable() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE)
    }

    @Test
    fun availabilityStatus_threadFeatureNotSupported_returnsUnsupported() {
        controller = newControllerWithThreadFeatureSupported(false)
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        assertThat(fakeThreadNetworkController.registeredStateCallback).isNull()
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun isChecked_threadSetEnabled_returnsTrue() {
        fakeThreadNetworkController.setEnabled(true, executor) { }
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        assertThat(controller.isChecked).isTrue()
    }

    @Test
    fun isChecked_threadSetDisabled_returnsFalse() {
        fakeThreadNetworkController.setEnabled(false, executor) { }
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        assertThat(controller.isChecked).isFalse()
    }

    @Test
    fun setChecked_setChecked_threadIsEnabled() {
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        controller.setChecked(true)

        assertThat(fakeThreadNetworkController.isEnabled).isTrue()
    }

    @Test
    fun setChecked_setUnchecked_threadIsDisabled() {
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        controller.setChecked(false)

        assertThat(fakeThreadNetworkController.isEnabled).isFalse()
    }

    @Test
    fun updatePreference_airPlaneModeOff_preferenceEnabled() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        assertThat(preference.isEnabled).isTrue()
        assertThat(preference.summary).isEqualTo(
            context.resources.getString(R.string.thread_network_settings_summary)
        )
    }

    @Test
    fun updatePreference_airPlaneModeOn_preferenceDisabled() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)

        assertThat(preference.isEnabled).isFalse()
        assertThat(preference.summary).isEqualTo(
            context.resources.getString(R.string.thread_network_settings_summary_airplane_mode)
        )
    }

    @Test
    fun updatePreference_airPlaneModeTurnedOn_preferenceDisabled() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        startControllerAndCaptureCallbacks()

        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        broadcastReceiverArgumentCaptor.value.onReceive(context, Intent())

        assertThat(preference.isEnabled).isFalse()
        assertThat(preference.summary).isEqualTo(
            context.resources.getString(R.string.thread_network_settings_summary_airplane_mode)
        )
    }

    private fun startControllerAndCaptureCallbacks() {
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)
        verify(context)!!.registerReceiver(broadcastReceiverArgumentCaptor.capture(), any())
    }

    private class FakeThreadNetworkController(private val executor: Executor) :
        BaseThreadNetworkController {
        var isEnabled = true
            private set
        var registeredStateCallback: StateCallback? = null
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
                            STATE_DISABLING
                        )
                    }
                    executor.execute {
                        registeredStateCallback!!.onThreadEnableStateChanged(
                            STATE_DISABLED
                        )
                    }
                } else {
                    executor.execute {
                        registeredStateCallback!!.onThreadEnableStateChanged(
                            STATE_ENABLED
                        )
                    }
                }
            }
            executor.execute { receiver.onResult(null) }
        }

        override fun registerStateCallback(
            executor: Executor,
            callback: StateCallback
        ) {
            require(callback !== registeredStateCallback) { "callback is already registered" }
            registeredStateCallback = callback
            val enabledState =
                if (isEnabled) STATE_ENABLED else STATE_DISABLED
            executor.execute { registeredStateCallback!!.onThreadEnableStateChanged(enabledState) }
        }

        override fun unregisterStateCallback(callback: StateCallback) {
            requireNotNull(registeredStateCallback) { "callback is already unregistered" }
            registeredStateCallback = null
        }
    }
}
