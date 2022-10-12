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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settingslib.applications.PermissionsSummaryHelper
import com.android.settingslib.applications.PermissionsSummaryHelper.PermissionsResultCallback
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
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
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.mockito.Mockito.`when` as whenever

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowPermissionsSummaryHelper::class])
class AppPermissionSummaryTest {

    @JvmField
    @Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private var context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var summaryLiveData: AppPermissionSummaryLiveData

    @Before
    fun setUp() {
        doReturn(context).`when`(context).createContextAsUser(any(), eq(0))
        whenever(context.packageManager).thenReturn(packageManager)

        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }
        summaryLiveData = AppPermissionSummaryLiveData(context, app)
    }

    @Test
    fun permissionsChangeListener() {
        summaryLiveData.getOrAwaitValue {
            verify(packageManager).addOnPermissionsChangeListener(any())
            verify(packageManager, never()).removeOnPermissionsChangeListener(any())
        }
        verify(packageManager).removeOnPermissionsChangeListener(any())
    }

    @Test
    fun summary_noPermissionsRequested() {
        ShadowPermissionsSummaryHelper.requestedPermissionCount = 0

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo(
            context.getString(R.string.runtime_permissions_summary_no_permissions_requested)
        )
        assertThat(enabled).isFalse()
    }

    @Test
    fun summary_noPermissionsGranted() {
        ShadowPermissionsSummaryHelper.requestedPermissionCount = 1
        ShadowPermissionsSummaryHelper.grantedGroupLabels = emptyList()

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo(
            context.getString(R.string.runtime_permissions_summary_no_permissions_granted)
        )
        assertThat(enabled).isTrue()
    }

    @Test
    fun onPermissionSummaryResult_hasRuntimePermission_shouldSetPermissionAsSummary() {
        ShadowPermissionsSummaryHelper.requestedPermissionCount = 1
        ShadowPermissionsSummaryHelper.grantedGroupLabels = listOf(PERMISSION)

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo(PERMISSION)
        assertThat(enabled).isTrue()
    }

    @Test
    fun onPermissionSummaryResult_hasAdditionalPermission_shouldSetAdditionalSummary() {
        ShadowPermissionsSummaryHelper.requestedPermissionCount = 5
        ShadowPermissionsSummaryHelper.additionalGrantedPermissionCount = 2
        ShadowPermissionsSummaryHelper.grantedGroupLabels = listOf(PERMISSION)

        val (summary, enabled) = summaryLiveData.getOrAwaitValue()!!

        assertThat(summary).isEqualTo("Storage and 2 additional permissions")
        assertThat(enabled).isTrue()
    }

    companion object {
        private const val PACKAGE_NAME = "packageName"
        private const val PERMISSION = "Storage"
    }
}

@Implements(PermissionsSummaryHelper::class)
private object ShadowPermissionsSummaryHelper {
    var requestedPermissionCount = 0
    var additionalGrantedPermissionCount = 0
    var grantedGroupLabels: List<CharSequence> = emptyList()

    @Implementation
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getPermissionSummary(context: Context, pkg: String, callback: PermissionsResultCallback) {
        callback.onPermissionSummaryResult(
            requestedPermissionCount,
            additionalGrantedPermissionCount,
            grantedGroupLabels,
        )
    }
}

private fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    afterObserve: () -> Unit = {},
): T? {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = Observer<T> { o ->
        data = o
        latch.countDown()
    }
    this.observeForever(observer)

    afterObserve()

    try {
        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(time, timeUnit)) {
            throw TimeoutException("LiveData value was never set.")
        }

    } finally {
        this.removeObserver(observer)
    }

    return data
}
