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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.android.settingslib.spaprivileged.model.app.IAppOpsController
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class WifiControlAppListModelTest {

    @get:Rule val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock private lateinit var packageManagers: IPackageManagers

    private lateinit var listModel: WifiControlAppListModel

    @Before
    fun setUp() {
        listModel = WifiControlAppListModel(context, packageManagers)
    }

    @Test
    fun transformItem_recordHasCorrectApp() {
        val record = listModel.transformItem(APP)

        assertThat(record.app).isSameInstanceAs(APP)
    }

    @Test
    fun transformItem_hasRequestPermission() = runTest {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(PM_CHANGE_WIFI_STATE)).thenReturn(true)
        }

        val record = listModel.transformItem(APP)

        assertThat(record.hasRequestPermission).isTrue()
    }

    @Test
    fun transformItem_notRequestPermission() = runTest {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(PM_CHANGE_WIFI_STATE)).thenReturn(false)
        }

        val record = listModel.transformItem(APP)

        assertThat(record.hasRequestPermission).isFalse()
    }

    @Test
    fun transformItem_hasRequestNetworkSettingsPermission() = runTest {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(PM_NETWORK_SETTINGS)).thenReturn(true)
        }

        val record = listModel.transformItem(APP)

        assertThat(record.hasRequestBroaderPermission).isTrue()
    }

    @Test
    fun transformItem_notRequestNetworkSettingsPermission() = runTest {
        with(packageManagers) {
            whenever(APP.hasRequestPermission(PM_NETWORK_SETTINGS)).thenReturn(false)
        }

        val record = listModel.transformItem(APP)

        assertThat(record.hasRequestBroaderPermission).isFalse()
    }

    @Test
    fun filter() = runTest {
        val appNotRequestPermissionRecord =
            AppOpPermissionRecord(
                app = APP_NOT_REQUEST_PERMISSION,
                hasRequestPermission = false,
                hasRequestBroaderPermission = false,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
            )
        val appRequestedNetworkSettingsRecord =
            AppOpPermissionRecord(
                app = APP_REQUESTED_NETWORK_SETTINGS,
                hasRequestPermission = true,
                hasRequestBroaderPermission = false,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT)
            )

        val recordListFlow =
            listModel.filter(
                flowOf(USER_ID),
                flowOf(listOf(appNotRequestPermissionRecord, appRequestedNetworkSettingsRecord))
            )

        val recordList = checkNotNull(recordListFlow.firstWithTimeoutOrNull())
        assertThat(recordList).containsExactly(appRequestedNetworkSettingsRecord)
    }

    @Test
    fun isAllowed_networkSettingsShouldTrump() {
        val record =
            AppOpPermissionRecord(
                app = APP,
                hasRequestPermission = false,
                hasRequestBroaderPermission = true,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
            )

        val isAllowed = getIsAllowed(record)

        assertThat(isAllowed).isTrue()
    }

    @Test
    fun isAllowed_grantedChangeWifiState() {
        val record =
            AppOpPermissionRecord(
                app = APP,
                hasRequestPermission = true,
                hasRequestBroaderPermission = false,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_ALLOWED),
            )

        val isAllowed = getIsAllowed(record)

        assertThat(isAllowed).isTrue()
    }

    @Test
    fun isAllowed_notAllowed() {
        val record =
            AppOpPermissionRecord(
                app = APP,
                hasRequestPermission = true,
                hasRequestBroaderPermission = false,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_IGNORED),
            )

        val isAllowed = getIsAllowed(record)

        assertThat(isAllowed).isFalse()
    }

    @Test
    fun isChangeable_noRequestedPermission() {
        val record =
            AppOpPermissionRecord(
                app = APP,
                hasRequestPermission = false,
                hasRequestBroaderPermission = false,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
            )

        val isChangeable = listModel.isChangeable(record)

        assertThat(isChangeable).isFalse()
    }

    @Test
    fun isChangeable_notChangableWhenRequestedNetworkSettingPermissions() {
        val record =
            AppOpPermissionRecord(
                app = APP,
                hasRequestPermission = false,
                hasRequestBroaderPermission = true,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
            )

        val isChangeable = listModel.isChangeable(record)

        assertThat(isChangeable).isFalse()
    }

    @Test
    fun isChangeable_changableWhenRequestedChangeWifiStatePermission() {
        val record =
            AppOpPermissionRecord(
                app = APP,
                hasRequestPermission = true,
                hasRequestBroaderPermission = false,
                appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
            )

        val isChangeable = listModel.isChangeable(record)

        assertThat(isChangeable).isTrue()
    }

    @Test
    fun setAllowed_shouldCallController() {
        val appOpsController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT)
        val record =
            AppOpPermissionRecord(
                app = APP,
                hasRequestPermission = true,
                hasRequestBroaderPermission = false,
                appOpsController = appOpsController,
            )

        listModel.setAllowed(record = record, newAllowed = true)

        assertThat(appOpsController.setAllowedCalledWith).isTrue()
    }

    private fun getIsAllowed(record: AppOpPermissionRecord): Boolean? {
        lateinit var isAllowedState: () -> Boolean?
        composeTestRule.setContent { isAllowedState = listModel.isAllowed(record) }
        return isAllowedState()
    }

    private companion object {
        const val USER_ID = 0
        const val PACKAGE_NAME = "package.name"
        const val PM_CHANGE_WIFI_STATE = Manifest.permission.CHANGE_WIFI_STATE
        const val PM_NETWORK_SETTINGS = Manifest.permission.NETWORK_SETTINGS

        val APP = ApplicationInfo().apply { packageName = PACKAGE_NAME }

        val APP_NOT_REQUEST_PERMISSION =
            ApplicationInfo().apply { packageName = "app1.package.name" }
        val APP_REQUESTED_NETWORK_SETTINGS =
            ApplicationInfo().apply { packageName = "app2.package.name" }
        val APP_REQUESTED_CHANGE_WIFI_STATE =
            ApplicationInfo().apply { packageName = "app3.package.name" }
    }
}

private class FakeAppOpsController(private val fakeMode: Int) : IAppOpsController {
    var setAllowedCalledWith: Boolean? = null

    override val mode = flowOf(fakeMode)

    override fun setAllowed(allowed: Boolean) {
        setAllowedCalledWith = allowed
    }

    override fun getMode() = fakeMode
}
