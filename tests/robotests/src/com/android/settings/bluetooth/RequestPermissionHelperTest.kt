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

package com.android.settings.bluetooth

import androidx.activity.ComponentActivity
import com.android.settings.R
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as whenever

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class RequestPermissionHelperTest {
    private lateinit var activityController: ActivityController<ComponentActivity>

    @Before
    fun setUp() {
        activityController =
            ActivityController.of(ComponentActivity()).create().start().postCreate(null).resume()
    }

    @After
    fun tearDown() {
        activityController.pause().stop().destroy()
    }

    @Test
    fun requestEnable_withAppLabelAndNoTimeout_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = "App Label",
            timeout = -1,
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs("App Label wants to turn on Bluetooth")
    }

    @Test
    fun requestEnable_withAppLabelAndZeroTimeout_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = "App Label",
            timeout = 0,
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs(
            "App Label wants to turn on Bluetooth and make your phone visible to other devices. " +
                "You can change this later in Bluetooth settings."
        )
    }

    @Test
    fun requestEnable_withAppLabelAndNormalTimeout_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = "App Label",
            timeout = 120,
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs(
            "App Label wants to turn on Bluetooth and make your phone visible to other devices " +
                "for 120 seconds."
        )
    }

    @Test
    fun requestEnable_withNoAppLabelAndNoTimeout_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = null,
            timeout = -1,
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs("An app wants to turn on Bluetooth")
    }

    @Test
    fun requestEnable_withNoAppLabelAndZeroTimeout_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = null,
            timeout = 0,
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs(
            "An app wants to turn on Bluetooth and make your phone visible to other devices. " +
                "You can change this later in Bluetooth settings."
        )
    }

    @Test
    fun requestEnable_withNoAppLabelAndNormalTimeout_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = null,
            timeout = 120,
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs(
            "An app wants to turn on Bluetooth and make your phone visible to other devices for " +
                "120 seconds."
        )
    }

    @Test
    fun requestEnable_whenAutoConfirm_onAllowIsCalled() {
        val activity = getActivityWith(autoConfirm = true)
        var onAllowCalled = false

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = null,
            timeout = -1,
            onAllow = { onAllowCalled = true },
            onDeny = {},
        )

        assertThat(onAllowCalled).isTrue()
    }

    @Test
    fun requestEnable_whenNotAutoConfirm_onAllowIsNotCalledWhenRequest() {
        val activity = getActivityWith(autoConfirm = false)
        var onAllowCalled = false

        RequestPermissionHelper.requestEnable(
            context = activity,
            appLabel = null,
            timeout = -1,
            onAllow = { onAllowCalled = true },
            onDeny = {},
        )

        assertThat(onAllowCalled).isFalse()
    }

    @Test
    fun requestDisable_withAppLabel_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestDisable(
            context = activity,
            appLabel = "App Label",
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs("App Label wants to turn off Bluetooth")
    }

    @Test
    fun requestDisable_withNoAppLabel_hasCorrectMessage() {
        val activity = activityController.get()

        RequestPermissionHelper.requestDisable(
            context = activity,
            appLabel = null,
            onAllow = {},
            onDeny = {},
        )!!.show()

        assertLatestMessageIs("An app wants to turn off Bluetooth")
    }

    @Test
    fun requestDisable_whenAutoConfirm_onAllowIsCalled() {
        val activity = getActivityWith(autoConfirm = true)
        var onAllowCalled = false

        RequestPermissionHelper.requestDisable(
            context = activity,
            appLabel = null,
            onAllow = { onAllowCalled = true },
            onDeny = {},
        )

        assertThat(onAllowCalled).isTrue()
    }

    @Test
    fun requestDisable_whenNotAutoConfirm_onAllowIsNotCalledWhenRequest() {
        val activity = getActivityWith(autoConfirm = false)
        var onAllowCalled = false

        RequestPermissionHelper.requestDisable(
            context = activity,
            appLabel = null,
            onAllow = { onAllowCalled = true },
            onDeny = {},
        )

        assertThat(onAllowCalled).isFalse()
    }

    private fun getActivityWith(autoConfirm: Boolean): ComponentActivity {
        val activity = spy(activityController.get())
        val spyResources = spy(activity.resources)
        whenever(activity.resources).thenReturn(spyResources)
        whenever(spyResources.getBoolean(R.bool.auto_confirm_bluetooth_activation_dialog))
            .thenReturn(autoConfirm)
        return activity
    }

    private fun assertLatestMessageIs(message: String) {
        val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
        val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
        assertThat(shadowDialog.message.toString()).isEqualTo(message)
    }
}
