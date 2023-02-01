/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.settings.R
import com.android.settings.testutils.mockAsUser
import com.android.settingslib.applications.PermissionsSummaryHelper
import com.android.settingslib.applications.PermissionsSummaryHelper.PermissionsResultCallback
import com.android.settingslib.spa.testutils.getOrAwaitValue
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppPermissionSummaryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockSession: MockitoSession

    @Spy
    private var context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var summaryLiveData: AppPermissionSummaryLiveData

    @Before
    fun setUp() {
        mockSession = mockitoSession()
            .initMocks(this)
            .mockStatic(PermissionsSummaryHelper::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        context.mockAsUser()
        whenever(context.packageManager).thenReturn(packageManager)

        summaryLiveData = AppPermissionSummaryLiveData(context, APP)
    }

    private fun mockGetPermissionSummary(
        requestedPermissionCount: Int = 0,
        additionalGrantedPermissionCount: Int = 0,
        grantedGroupLabels: List<CharSequence> = emptyList(),
    ) {
        whenever(PermissionsSummaryHelper.getPermissionSummary(any(), eq(PACKAGE_NAME), any()))
            .thenAnswer {
                val callback = it.arguments[2] as PermissionsResultCallback
                callback.onPermissionSummaryResult(
                    requestedPermissionCount,
                    additionalGrantedPermissionCount,
                    grantedGroupLabels,
                )
            }
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun permissionsChangeListener() {
        mockGetPermissionSummary()

        summaryLiveData.getOrAwaitValue {
            verify(packageManager).addOnPermissionsChangeListener(any())
            verify(packageManager, never()).removeOnPermissionsChangeListener(any())
        }

        verify(packageManager).removeOnPermissionsChangeListener(any())
    }

    @Test
    fun summary_noPermissionsRequested() {
        mockGetPermissionSummary(requestedPermissionCount = 0)

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo(
            context.getString(R.string.runtime_permissions_summary_no_permissions_requested)
        )
        assertThat(enabled).isFalse()
    }

    @Test
    fun summary_noPermissionsGranted() {
        mockGetPermissionSummary(requestedPermissionCount = 1, grantedGroupLabels = emptyList())

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo(
            context.getString(R.string.runtime_permissions_summary_no_permissions_granted)
        )
        assertThat(enabled).isTrue()
    }

    @Test
    fun onPermissionSummaryResult_hasRuntimePermission_shouldSetPermissionAsSummary() {
        mockGetPermissionSummary(
            requestedPermissionCount = 1,
            grantedGroupLabels = listOf(PERMISSION),
        )

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo(PERMISSION)
        assertThat(enabled).isTrue()
    }

    @Test
    fun onPermissionSummaryResult_hasAdditionalPermission_shouldSetAdditionalSummary() {
        mockGetPermissionSummary(
            requestedPermissionCount = 5,
            additionalGrantedPermissionCount = 2,
            grantedGroupLabels = listOf(PERMISSION),
        )

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo("Storage and 2 additional permissions")
        assertThat(enabled).isTrue()
    }

    private companion object {
        const val PACKAGE_NAME = "packageName"
        const val PERMISSION = "Storage"
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
    }
}
