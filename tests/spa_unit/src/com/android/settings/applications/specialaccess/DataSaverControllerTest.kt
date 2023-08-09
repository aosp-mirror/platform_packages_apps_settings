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

package com.android.settings.applications.specialaccess

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.net.NetworkPolicyManager
import android.net.NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.applications.specialaccess.DataSaverController.Companion.getUnrestrictedSummary
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE
import com.android.settingslib.spaprivileged.model.app.AppListRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class DataSaverControllerTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Spy
    private val resources: Resources = context.resources

    @Mock
    private lateinit var networkPolicyManager: NetworkPolicyManager

    @Mock
    private lateinit var dataSaverController: DataSaverController

    @Before
    fun setUp() {
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.resources).thenReturn(resources)
        whenever(NetworkPolicyManager.from(context)).thenReturn(networkPolicyManager)

        dataSaverController = DataSaverController(context, "key")
    }

    @Test
    fun getAvailabilityStatus_whenConfigOn_available() {
        whenever(resources.getBoolean(R.bool.config_show_data_saver)).thenReturn(true)
        assertThat(dataSaverController.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_whenConfigOff_unsupportedOnDevice() {
        whenever(resources.getBoolean(R.bool.config_show_data_saver)).thenReturn(false)
        assertThat(dataSaverController.availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getUnrestrictedSummary_whenTwoAppsAllowed() = runTest {
        whenever(
            networkPolicyManager.getUidsWithPolicy(POLICY_ALLOW_METERED_BACKGROUND)
        ).thenReturn(intArrayOf(APP1.uid, APP2.uid))

        val summary =
            getUnrestrictedSummary(context = context, appListRepository = FakeAppListRepository)

        assertThat(summary)
            .isEqualTo("2 apps allowed to use unrestricted data when Data Saver is on")
    }

    @Test
    fun getUnrestrictedSummary_whenNoAppsAllowed() = runTest {
        whenever(
            networkPolicyManager.getUidsWithPolicy(POLICY_ALLOW_METERED_BACKGROUND)
        ).thenReturn(intArrayOf())

        val summary =
            getUnrestrictedSummary(context = context, appListRepository = FakeAppListRepository)

        assertThat(summary)
            .isEqualTo("0 apps allowed to use unrestricted data when Data Saver is on")
    }

    private companion object {
        val APP1 = ApplicationInfo().apply { uid = 10001 }
        val APP2 = ApplicationInfo().apply { uid = 10002 }
        val APP3 = ApplicationInfo().apply { uid = 10003 }

        object FakeAppListRepository : AppListRepository {
            override suspend fun loadApps(
                userId: Int,
                loadInstantApps: Boolean,
                matchAnyUserForAdmin: Boolean,
            ) = emptyList<ApplicationInfo>()

            override fun showSystemPredicate(
                userIdFlow: Flow<Int>,
                showSystemFlow: Flow<Boolean>,
            ): Flow<(app: ApplicationInfo) -> Boolean> = flowOf { false }

            override fun getSystemPackageNamesBlocking(userId: Int): Set<String> = emptySet()

            override suspend fun loadAndFilterApps(userId: Int, isSystemApp: Boolean) =
                listOf(APP1, APP2, APP3)
        }
    }
}