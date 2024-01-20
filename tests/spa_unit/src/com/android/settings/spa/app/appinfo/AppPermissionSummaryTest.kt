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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.settings.R
import com.android.settings.testutils.mockAsUser
import com.android.settingslib.applications.PermissionsSummaryHelper
import com.android.settingslib.applications.PermissionsSummaryHelper.PermissionsResultCallback
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class AppPermissionSummaryTest {

    private lateinit var mockSession: MockitoSession

    private val mockPackageManager = mock<PackageManager>()

    private var context: Context = spy(ApplicationProvider.getApplicationContext()) {
        mock.mockAsUser()
        on { packageManager } doReturn mockPackageManager
    }

    private val summaryRepository = AppPermissionSummaryRepository(context, APP)

    @Before
    fun setUp() {
        mockSession = mockitoSession()
            .mockStatic(PermissionsSummaryHelper::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
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
    fun summary_noPermissionsRequested() = runBlocking {
        mockGetPermissionSummary(requestedPermissionCount = 0)

        val (summary, enabled) = summaryRepository.flow.first()

        assertThat(summary).isEqualTo(
            context.getString(R.string.runtime_permissions_summary_no_permissions_requested)
        )
        assertThat(enabled).isFalse()
    }

    @Test
    fun summary_noPermissionsGranted() = runBlocking {
        mockGetPermissionSummary(requestedPermissionCount = 1, grantedGroupLabels = emptyList())

        val (summary, enabled) = summaryRepository.flow.first()

        assertThat(summary).isEqualTo(
            context.getString(R.string.runtime_permissions_summary_no_permissions_granted)
        )
        assertThat(enabled).isTrue()
    }

    @Test
    fun summary_hasRuntimePermission_usePermissionAsSummary() = runBlocking {
        mockGetPermissionSummary(
            requestedPermissionCount = 1,
            grantedGroupLabels = listOf(PERMISSION),
        )

        val (summary, enabled) = summaryRepository.flow.first()

        assertThat(summary).isEqualTo(PERMISSION)
        assertThat(enabled).isTrue()
    }

    @Test
    fun summary_hasAdditionalPermission_containsAdditionalSummary() = runBlocking {
        mockGetPermissionSummary(
            requestedPermissionCount = 5,
            additionalGrantedPermissionCount = 2,
            grantedGroupLabels = listOf(PERMISSION),
        )

        val (summary, enabled) = summaryRepository.flow.first()

        assertThat(summary).isEqualTo("Storage and 2 additional permissions")
        assertThat(enabled).isTrue()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val PERMISSION = "Storage"
        val APP = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
    }
}
