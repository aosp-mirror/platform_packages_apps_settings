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

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.Flags
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.platform.test.annotations.RequiresFlagsEnabled
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class AppButtonRepositoryTest {

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var appButtonRepository: AppButtonRepository

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)

        appButtonRepository = AppButtonRepository(context)
    }

    private fun mockGetHomeActivities(
        homeActivities: List<ResolveInfo>,
        currentDefaultHome: ComponentName? = null,
    ) {
        whenever(packageManager.getHomeActivities(any())).then {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as ArrayList<ResolveInfo>).addAll(homeActivities)
            currentDefaultHome
        }
    }

    @Test
    fun getHomePackageInfo_empty() {
        mockGetHomeActivities(homeActivities = emptyList())

        val homePackageInfo = appButtonRepository.getHomePackageInfo()

        assertThat(homePackageInfo.homePackages).isEmpty()
        assertThat(homePackageInfo.currentDefaultHome).isNull()
    }

    @Test
    fun getHomePackageInfo_noActivityInfo() {
        mockGetHomeActivities(homeActivities = listOf(ResolveInfo()))

        val homePackageInfo = appButtonRepository.getHomePackageInfo()

        assertThat(homePackageInfo.homePackages).isEmpty()
        assertThat(homePackageInfo.currentDefaultHome).isNull()
    }

    @Test
    fun getHomePackageInfo_oneHome() {
        mockGetHomeActivities(
            homeActivities = listOf(RESOLVE_INFO),
            currentDefaultHome = COMPONENT_NAME,
        )

        val homePackageInfo = appButtonRepository.getHomePackageInfo()

        assertThat(homePackageInfo.homePackages).containsExactly(PACKAGE_NAME)
        assertThat(homePackageInfo.currentDefaultHome).isSameInstanceAs(COMPONENT_NAME)
    }

    @Test
    fun getHomePackageInfo_homeAlternateSignatureMatch() {
        mockGetHomeActivities(homeActivities = listOf(RESOLVE_INFO_WITH_ALTERNATE))
        whenever(packageManager.checkSignatures(PACKAGE_NAME_ALTERNATE, PACKAGE_NAME))
            .thenReturn(PackageManager.SIGNATURE_MATCH)

        val homePackageInfo = appButtonRepository.getHomePackageInfo()

        assertThat(homePackageInfo.homePackages).containsExactly(
            PACKAGE_NAME, PACKAGE_NAME_ALTERNATE
        )
    }

    @Test
    fun getHomePackageInfo_homeAlternateSignatureNoMatch() {
        mockGetHomeActivities(homeActivities = listOf(RESOLVE_INFO_WITH_ALTERNATE))
        whenever(packageManager.checkSignatures(PACKAGE_NAME_ALTERNATE, PACKAGE_NAME))
            .thenReturn(PackageManager.SIGNATURE_NO_MATCH)

        val homePackageInfo = appButtonRepository.getHomePackageInfo()

        assertThat(homePackageInfo.homePackages).containsExactly(PACKAGE_NAME)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IMPROVE_HOME_APP_BEHAVIOR)
    fun uninstallDisallowedDueToHomeApp_isNotSystemAndIsCurrentHomeAndHasOnlyOneHomeApp() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        mockGetHomeActivities(
            homeActivities = listOf(RESOLVE_INFO),
            currentDefaultHome = COMPONENT_NAME,
        )

        val value = appButtonRepository.uninstallDisallowedDueToHomeApp(app)

        assertThat(value).isTrue()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IMPROVE_HOME_APP_BEHAVIOR)
    fun uninstallDisallowedDueToHomeApp_isNotSystemAndIsCurrentHomeAndHasOtherHomeApps() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
        }

        mockGetHomeActivities(
            homeActivities = listOf(RESOLVE_INFO, RESOLVE_INFO_FAKE),
            currentDefaultHome = COMPONENT_NAME,
        )

        val value = appButtonRepository.uninstallDisallowedDueToHomeApp(app)

        assertThat(value).isFalse()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IMPROVE_HOME_APP_BEHAVIOR)
    fun uninstallDisallowedDueToHomeApp_isSystemAndIsCurrentHomeAndHasOtherHomeApps() {
        val app = ApplicationInfo().apply {
            packageName = PACKAGE_NAME
            flags = ApplicationInfo.FLAG_SYSTEM
        }

        mockGetHomeActivities(
            homeActivities = listOf(RESOLVE_INFO, RESOLVE_INFO_FAKE),
            currentDefaultHome = COMPONENT_NAME,
        )

        val value = appButtonRepository.uninstallDisallowedDueToHomeApp(app)

        assertThat(value).isTrue()
    }

    private companion object {
        const val PACKAGE_NAME = "packageName"
        const val PACKAGE_NAME_ALTERNATE = "packageName.alternate"
        const val PACKAGE_NAME_FAKE = "packageName.fake"
        const val ACTIVITY_NAME = "activityName"
        val COMPONENT_NAME = ComponentName(PACKAGE_NAME, ACTIVITY_NAME)
        val RESOLVE_INFO = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = PACKAGE_NAME
            }
        }
        val RESOLVE_INFO_FAKE = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = PACKAGE_NAME_FAKE
            }
        }
        val RESOLVE_INFO_WITH_ALTERNATE = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = PACKAGE_NAME
                metaData = bundleOf(
                    ActivityManager.META_HOME_ALTERNATE to PACKAGE_NAME_ALTERNATE,
                )
            }
        }
    }
}
