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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.app.settings.SettingsEnums
import android.companion.AssociationRequest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.media.flags.Flags
import com.android.settings.R
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.spaprivileged.model.app.IAppOpsController
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class MediaRoutingControlTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule();

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var listModel: MediaRoutingControlAppsListModel

    @Mock
    private lateinit var mockRoleManager: RoleManager

    private val fakeFeatureFactory = FakeFeatureFactory()
    private val metricsFeatureProvider = fakeFeatureFactory.metricsFeatureProvider

    @Before
    fun setUp() {
        whenever(context.getSystemService(RoleManager::class.java))
                .thenReturn(mockRoleManager)
        listModel = MediaRoutingControlAppsListModel(context)
    }

    @Test
    fun modelResourceIdAndProperties() {
        assertThat(listModel.pageTitleResId).isEqualTo(R.string.media_routing_control_title)
        assertThat(listModel.switchTitleResId).isEqualTo(R.string.allow_media_routing_control)
        assertThat(listModel.footerResId).isEqualTo(R.string.allow_media_routing_description)
        assertThat(listModel.appOp).isEqualTo(AppOpsManager.OP_MEDIA_ROUTING_CONTROL)
        assertThat(listModel.permission).isEqualTo(Manifest.permission.MEDIA_ROUTING_CONTROL)
        assertThat(listModel.setModeByUid).isTrue()
    }

    @Test
    fun setAllowed_callWithNewStatusAsTrue_shouldChangeAppControllerModeToAllowed() {
        val fakeAppOpController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT)
        val permissionRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = true,
                        hasRequestBroaderPermission = false,
                        appOpsController = fakeAppOpController,
                )

        listModel.setAllowed(permissionRequestedRecord, true)

        assertThat(fakeAppOpController.getMode()).isEqualTo(AppOpsManager.MODE_ALLOWED)
    }

    @Test
    fun setAllowed_callWithNewStatusAsTrue_shouldLogPermissionToggleActionAsAllowed() {
        val fakeAppOpController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT)
        val permissionRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = true,
                        hasRequestBroaderPermission = false,
                        appOpsController = fakeAppOpController,
                )

        listModel.setAllowed(permissionRequestedRecord, true)

        verify(metricsFeatureProvider)
                .action(context, SettingsEnums.MEDIA_ROUTING_CONTROL, VALUE_LOGGING_ALLOWED)
    }

    @Test
    fun setAllowed_callWithNewStatusAsFalse_shouldChangeAppControllerModeToErrored() {
        val fakeAppOpController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT)
        val permissionRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = true,
                        hasRequestBroaderPermission = false,
                        appOpsController = fakeAppOpController,
                )

        listModel.setAllowed(permissionRequestedRecord, false)

        assertThat(fakeAppOpController.getMode()).isEqualTo(AppOpsManager.MODE_ERRORED)
    }

    @Test
    fun setAllowed_callWithNewStatusAsFalse_shouldLogPermissionToggleActionAsDenied() {
        val fakeAppOpController = FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT)
        val permissionRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = true,
                        hasRequestBroaderPermission = false,
                        appOpsController = fakeAppOpController,
                )

        listModel.setAllowed(permissionRequestedRecord, false)

        verify(metricsFeatureProvider)
                .action(context, SettingsEnums.MEDIA_ROUTING_CONTROL, VALUE_LOGGING_DENIED)
    }

    @Test
    fun isChangeable_permissionRequestedByAppAndWatchCompanionRoleAssigned_shouldReturnTrue() {
        setFlagsRule.enableFlags(Flags.FLAG_ENABLE_PRIVILEGED_ROUTING_FOR_MEDIA_ROUTING_CONTROL)
        val permissionRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = true,
                        hasRequestBroaderPermission = false,
                        appOpsController =
                            FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
                )
        whenever(mockRoleManager.getRoleHolders(AssociationRequest.DEVICE_PROFILE_WATCH))
                .thenReturn(listOf(PACKAGE_NAME))

        val isSpecialAccessChangeable = listModel.isChangeable(permissionRequestedRecord)

        assertThat(isSpecialAccessChangeable).isTrue()
    }

    @Test
    fun isChangeable_permissionNotRequestedByAppButWatchCompanionRoleAssigned_shouldReturnFalse() {
        setFlagsRule.enableFlags(Flags.FLAG_ENABLE_PRIVILEGED_ROUTING_FOR_MEDIA_ROUTING_CONTROL)
        val permissionNotRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = false,
                        hasRequestBroaderPermission = false,
                        appOpsController =
                            FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
                )
        whenever(mockRoleManager.getRoleHolders(AssociationRequest.DEVICE_PROFILE_WATCH))
                .thenReturn(listOf(PACKAGE_NAME))

        val isSpecialAccessChangeable = listModel.isChangeable(permissionNotRequestedRecord)

        assertThat(isSpecialAccessChangeable).isFalse()
    }

    @Test
    fun isChangeable_permissionRequestedByAppButWatchCompanionRoleNotAssigned_shouldReturnFalse() {
        setFlagsRule.enableFlags(Flags.FLAG_ENABLE_PRIVILEGED_ROUTING_FOR_MEDIA_ROUTING_CONTROL)
        val permissionRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = true,
                        hasRequestBroaderPermission = false,
                        appOpsController =
                            FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
                )
        whenever(mockRoleManager.getRoleHolders(AssociationRequest.DEVICE_PROFILE_WATCH))
                .thenReturn(listOf("other.package.name"))

        val isSpecialAccessChangeable = listModel.isChangeable(permissionRequestedRecord)

        assertThat(isSpecialAccessChangeable).isFalse()
    }

    @Test
    fun isChangeable_withFlagDisabled_shouldReturnFalse() {
        setFlagsRule.disableFlags(Flags.FLAG_ENABLE_PRIVILEGED_ROUTING_FOR_MEDIA_ROUTING_CONTROL)
        val permissionRequestedRecord =
                AppOpPermissionRecord(
                        app = ApplicationInfo().apply { packageName = PACKAGE_NAME },
                        hasRequestPermission = true,
                        hasRequestBroaderPermission = false,
                        appOpsController =
                        FakeAppOpsController(fakeMode = AppOpsManager.MODE_DEFAULT),
                )
        whenever(mockRoleManager.getRoleHolders(AssociationRequest.DEVICE_PROFILE_WATCH))
                .thenReturn(listOf(PACKAGE_NAME))

        val isSpecialAccessChangeable = listModel.isChangeable(permissionRequestedRecord)

        assertThat(isSpecialAccessChangeable).isFalse()
    }

    private class FakeAppOpsController(fakeMode: Int) : IAppOpsController {

        override val mode = MutableStateFlow(fakeMode)

        override fun setAllowed(allowed: Boolean) {
            mode.value = if (allowed) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_ERRORED
        }

        override fun getMode(): Int = mode.value
    }

    companion object {
        const val PACKAGE_NAME = "test.package.name"
        const val VALUE_LOGGING_ALLOWED = 1
        const val VALUE_LOGGING_DENIED = 0
    }
}