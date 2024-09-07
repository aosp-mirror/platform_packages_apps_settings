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

package com.android.settings.spa.app.appinfo

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppForceStopRepositoryTest {

    private val mockDevicePolicyManager = mock<DevicePolicyManager>()

    private var resultCode = Activity.RESULT_CANCELED

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { devicePolicyManager } doReturn mockDevicePolicyManager
        onGeneric {
            sendOrderedBroadcastAsUser(
                argThat { action == Intent.ACTION_QUERY_PACKAGE_RESTART },
                eq(UserHandle.CURRENT),
                eq(Manifest.permission.HANDLE_QUERY_PACKAGE_RESTART),
                any(),
                isNull(),
                eq(Activity.RESULT_CANCELED),
                isNull(),
                isNull(),
            )
        } doAnswer {
            val broadcastReceiver = spy(it.arguments[3] as BroadcastReceiver) {
                on { resultCode } doReturn resultCode
            }
            broadcastReceiver.onReceive(mock, it.arguments[0] as Intent)
        }
    }

    private val packageInfoPresenter = mock<PackageInfoPresenter> {
        on { context } doReturn context
    }

    private val repository = AppForceStopRepository(packageInfoPresenter)

    @Test
    fun getActionButton_isActiveAdmin_returnFalse() = runBlocking {
        val app = mockApp {}
        mockDevicePolicyManager.stub {
            on { packageHasActiveAdmins(PACKAGE_NAME, app.userId) } doReturn true
        }

        val canForceStop = repository.canForceStopFlow().firstWithTimeoutOrNull()

        assertThat(canForceStop).isFalse()
    }

    @Test
    fun getActionButton_isUninstallInQueue_returnFalse() = runBlocking {
        mockApp {}
        mockDevicePolicyManager.stub {
            on { isUninstallInQueue(PACKAGE_NAME) } doReturn true
        }

        val canForceStop = repository.canForceStopFlow().firstWithTimeoutOrNull()

        assertThat(canForceStop).isFalse()
    }

    @Test
    fun canForceStopFlow_notStopped_returnTrue() = runBlocking {
        mockApp { flags = 0 }

        val canForceStop = repository.canForceStopFlow().firstWithTimeoutOrNull()

        assertThat(canForceStop).isTrue()
    }

    @Test
    fun canForceStopFlow_isStoppedAndQueryReturnCancel_returnFalse() = runBlocking {
        mockApp {
            flags = ApplicationInfo.FLAG_STOPPED
        }
        resultCode = Activity.RESULT_CANCELED

        val canForceStop = repository.canForceStopFlow().firstWithTimeoutOrNull()

        assertThat(canForceStop).isFalse()
    }

    @Test
    fun canForceStopFlow_isStoppedAndQueryReturnOk_returnTrue() = runBlocking {
        mockApp {
            flags = ApplicationInfo.FLAG_STOPPED
        }
        resultCode = Activity.RESULT_OK

        val canForceStop = repository.canForceStopFlow().firstWithTimeoutOrNull()

        assertThat(canForceStop).isTrue()
    }

    private fun mockApp(builder: ApplicationInfo.() -> Unit = {}) = packageInfoPresenter.stub {
        on { flow } doReturn MutableStateFlow(PackageInfo().apply {
            applicationInfo = createApp(builder)
        })
    }

    private fun createApp(builder: ApplicationInfo.() -> Unit = {}) =
        ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            uid = UID
            enabled = true
        }.apply(builder)

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val UID = 10000
    }
}
