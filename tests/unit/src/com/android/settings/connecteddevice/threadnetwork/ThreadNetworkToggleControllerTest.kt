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
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE
import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import java.util.concurrent.Executor
import org.junit.Ignore

/** Unit tests for [ThreadNetworkToggleController].  */
@RunWith(AndroidJUnit4::class)
class ThreadNetworkToggleControllerTest {
    @get:Rule
    val mSetFlagsRule = SetFlagsRule()
    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var controller: ThreadNetworkToggleController
    private lateinit var fakeThreadNetworkController: FakeThreadNetworkController
    private lateinit var preference: SwitchPreference

    @Before
    fun setUp() {
        mSetFlagsRule.enableFlags(Flags.FLAG_THREAD_SETTINGS_ENABLED)
        context = spy(ApplicationProvider.getApplicationContext<Context>())
        executor = MoreExecutors.directExecutor()
        fakeThreadNetworkController = FakeThreadNetworkController()
        controller = newControllerWithThreadFeatureSupported(true)
        val preferenceManager = PreferenceManager(context)
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preference = SwitchPreference(context)
        preference.key = "toggle_thread_network"
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    private fun newControllerWithThreadFeatureSupported(
        present: Boolean
    ): ThreadNetworkToggleController {
        return ThreadNetworkToggleController(
            context,
            "toggle_thread_network" /* key */,
            executor,
            if (present) fakeThreadNetworkController else null
        )
    }

    @Test
    fun availabilityStatus_flagDisabled_returnsConditionallyUnavailable() {
        mSetFlagsRule.disableFlags(Flags.FLAG_THREAD_SETTINGS_ENABLED)
        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun availabilityStatus_threadFeatureNotSupported_returnsUnsupported() {
        controller = newControllerWithThreadFeatureSupported(false)
        startController(controller)

        assertThat(fakeThreadNetworkController.registeredStateCallback).isNull()
        assertThat(controller.availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE)
    }

    @Test
    @Ignore("b/339767488")
    fun isChecked_threadSetEnabled_returnsTrue() {
        fakeThreadNetworkController.setEnabled(true, executor) { }
        startController(controller)

        assertThat(controller.isChecked).isTrue()
    }

    @Test
    fun isChecked_threadSetDisabled_returnsFalse() {
        fakeThreadNetworkController.setEnabled(false, executor) { }
        startController(controller)

        assertThat(controller.isChecked).isFalse()
    }

    @Test
    fun setChecked_setChecked_threadIsEnabled() {
        startController(controller)

        controller.setChecked(true)

        assertThat(fakeThreadNetworkController.isEnabled).isTrue()
    }

    @Test
    @Ignore("b/339767488")
    fun setChecked_setUnchecked_threadIsDisabled() {
        startController(controller)

        controller.setChecked(false)

        assertThat(fakeThreadNetworkController.isEnabled).isFalse()
    }

    private fun startController(controller: ThreadNetworkToggleController) {
        controller.onStateChanged(mock(LifecycleOwner::class.java), Lifecycle.Event.ON_START)
    }
}
