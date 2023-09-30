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

package com.android.settings.notification.app

import android.Manifest.permission.USE_FULL_SCREEN_INTENT
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.MODE_ERRORED
import android.app.AppOpsManager.OP_USE_FULL_SCREEN_INTENT
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager.IMPORTANCE_UNSPECIFIED
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SET
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.NameNotFoundException
import android.os.UserHandle
import android.permission.PermissionManager.PERMISSION_GRANTED
import android.permission.PermissionManager.PERMISSION_HARD_DENIED
import android.permission.PermissionManager.PERMISSION_SOFT_DENIED
import android.permission.PermissionManager.PermissionResult
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import com.android.settings.notification.NotificationBackend
import com.android.settings.notification.NotificationBackend.AppRow
import com.android.settings.notification.app.FullScreenIntentPermissionPreferenceController.Companion.KEY_FSI_PERMISSION
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin
import com.android.settingslib.RestrictedSwitchPreference
import com.android.settingslib.testutils.shadow.ShadowPermissionChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplicationPackageManager
import org.mockito.Mockito.`when` as whenever

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowApplicationPackageManager::class,
    ShadowPermissionChecker::class,
])
class FullScreenIntentPermissionPreferenceControllerTest {
    @JvmField
    @Rule
    val mockitoRule = MockitoJUnit.rule()!!

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var appOpsManager: AppOpsManager

    private lateinit var preference: RestrictedSwitchPreference

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var screen: PreferenceScreen

    private lateinit var controller: FullScreenIntentPermissionPreferenceController

    @Before
    fun setUp() {
        val context = spy(ApplicationProvider.getApplicationContext<Context>())

        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.getSystemService(AppOpsManager::class.java)).thenReturn(appOpsManager)

        preference = RestrictedSwitchPreference(context).apply { key = KEY_FSI_PERMISSION }

        whenever(screen.findPreference<Preference>(KEY_FSI_PERMISSION)).thenReturn(preference)

        controller = FullScreenIntentPermissionPreferenceController(
            context,
            mock(NotificationBackend::class.java)
        )
    }

    @Test
    fun testIsAvailable_notWhenPermissionNotRequested() {
        setPermissionRequestedInManifest(requested = false)
        initController()

        assertFalse(controller.isAvailable)
    }

    @Test
    fun testIsAvailable_notWhenOnChannelScreen() {
        setPermissionRequestedInManifest()
        initController(channel = makeTestChannel())

        assertFalse(controller.isAvailable)
    }

    @Test
    fun testIsAvailable_notWhenOnGroupScreen() {
        setPermissionRequestedInManifest()
        initController(group = makeTestGroup())

        assertFalse(controller.isAvailable)
    }

    @Test
    fun testIsAvailable_onAppScreenWhenRequested() {
        setPermissionRequestedInManifest()
        initController()

        assertTrue(controller.isAvailable)
    }

    @Test
    fun testIsAvailable_notWhenPackageNotFound() {
        setPackageInfoNotFound()
        initController()

        assertFalse(controller.isAvailable)
    }

    @Test
    fun testIsEnabled_notWhenDisabledByAdmin() {
        setPermissionRequestedInManifest()
        initController(admin = makeTestAdmin())

        controller.updateState(preference)

        assertFalse(preference.isEnabled)
    }

    @Test
    fun testIsEnabled_whenNotDisabledByAdmin() {
        setPermissionRequestedInManifest()
        initController()

        controller.updateState(preference)

        assertTrue(preference.isEnabled)
    }

    @Test
    fun testIsChecked_notWhenHardDenied() {
        setPermissionRequestedInManifest()
        setPermissionResult(PERMISSION_HARD_DENIED)
        initController()

        controller.updateState(preference)

        assertFalse(preference.isChecked)
    }

    @Test
    fun testIsChecked_notWhenSoftDenied() {
        setPermissionRequestedInManifest()
        setPermissionResult(PERMISSION_SOFT_DENIED)
        initController()

        controller.updateState(preference)

        assertFalse(preference.isChecked)
    }

    @Test
    fun testIsChecked_whenGranted() {
        setPermissionRequestedInManifest()
        setPermissionResult(PERMISSION_GRANTED)
        initController()

        controller.updateState(preference)

        assertTrue(preference.isChecked)
    }

    @Test
    fun testOnPreferenceChange_whenHardDenied() {
        setPermissionRequestedInManifest()
        setPermissionResult(PERMISSION_HARD_DENIED)
        initController()
        controller.displayPreference(screen)
        controller.updateState(preference)
        assertFalse(preference.isChecked)

        setPreferenceChecked(true)

        verifySetAppOpMode(MODE_ALLOWED)
        verifySetPermissionUserSetFlag()
    }

    @Test
    fun testOnPreferenceChange_whenSoftDenied() {
        setPermissionRequestedInManifest()
        setPermissionResult(PERMISSION_SOFT_DENIED)
        initController()
        controller.displayPreference(screen)
        controller.updateState(preference)
        assertFalse(preference.isChecked)

        setPreferenceChecked(true)

        verifySetAppOpMode(MODE_ALLOWED)
        verifySetPermissionUserSetFlag()
    }

    @Test
    fun testOnPreferenceChange_whenGranted() {
        setPermissionRequestedInManifest()
        setPermissionResult(PERMISSION_GRANTED)
        initController()
        controller.displayPreference(screen)
        controller.updateState(preference)
        assertTrue(preference.isChecked)

        setPreferenceChecked(false)

        verifySetAppOpMode(MODE_ERRORED)
        verifySetPermissionUserSetFlag()
    }

    private fun setPermissionRequestedInManifest(
        requested: Boolean = true
    ) {
        whenever(packageManager.getPackageInfo(TEST_PACKAGE, GET_PERMISSIONS)).thenReturn(
            PackageInfo().apply {
                packageName = TEST_PACKAGE
                applicationInfo = ApplicationInfo().apply { packageName = TEST_PACKAGE }
                requestedPermissions = if (requested) arrayOf(USE_FULL_SCREEN_INTENT) else arrayOf()
            })
    }

    private fun setPackageInfoNotFound() {
        whenever(packageManager.getPackageInfo(TEST_PACKAGE, GET_PERMISSIONS)).thenThrow(
            NameNotFoundException(TEST_PACKAGE)
        )
    }

    private fun setPermissionResult(@PermissionResult result: Int) {
        ShadowPermissionChecker.setResult(TEST_PACKAGE, USE_FULL_SCREEN_INTENT, result)
    }

    private fun setPreferenceChecked(checked: Boolean) {
        preference.isChecked = checked

        /* This shouldn't be necessary, but for some reason it's not called automatically when
           isChecked is changed. */
        controller.onPreferenceChange(preference, checked)
    }

    private fun verifySetAppOpMode(@AppOpsManager.Mode expectedMode: Int) {
        verify(appOpsManager).setUidMode(OP_USE_FULL_SCREEN_INTENT, TEST_UID, expectedMode)
    }

    private fun verifySetPermissionUserSetFlag() {
        verify(packageManager).updatePermissionFlags(
            USE_FULL_SCREEN_INTENT,
            TEST_PACKAGE,
            FLAG_PERMISSION_USER_SET,
            FLAG_PERMISSION_USER_SET,
            makeTestUserHandle()
        )
    }

    private fun initController(
        channel: NotificationChannel? = null,
        group: NotificationChannelGroup? = null,
        admin: EnforcedAdmin? = null
    ) {
        controller.onResume(
            makeTestAppRow(),
            channel,
            group,
            /* conversationDrawable = */null,
            /* conversationInfo = */null,
            admin,
            /* preferenceFilter = */null
        )
    }

    private fun makeTestChannel() =
        NotificationChannel("test_channel_id", "Test Channel Name", IMPORTANCE_UNSPECIFIED)

    private fun makeTestGroup() = NotificationChannelGroup("test_group_id", "Test Group Name")

    private fun makeTestAppRow() = AppRow().apply { pkg = TEST_PACKAGE; uid = TEST_UID }

    private fun makeTestUserHandle() = UserHandle.getUserHandleForUid(TEST_UID)

    private fun makeTestAdmin() = mock(EnforcedAdmin::class.java)

    private companion object {
        const val TEST_PACKAGE = "test.package.name"
        const val TEST_UID = 12345
    }
}