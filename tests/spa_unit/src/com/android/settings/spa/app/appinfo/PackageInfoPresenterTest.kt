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
import android.app.KeyguardManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.Utils
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.mockAsUser
import com.android.settingslib.spaprivileged.framework.common.activityManager
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class PackageInfoPresenterTest {

    private val mockPackageManager = mock<PackageManager>()

    private val mockActivityManager = mock<ActivityManager>()

    private val mockKeyguardManager = mock<KeyguardManager>()

    private lateinit var mockSession: MockitoSession

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn mockPackageManager
        on { activityManager } doReturn mockActivityManager
        on { getSystemService(Context.KEYGUARD_SERVICE) } doReturn mockKeyguardManager
        doNothing().whenever(mock).startActivityAsUser(any(), any())
        mock.mockAsUser()
    }

    private val packageManagers = mock<IPackageManagers>()

    private val fakeFeatureFactory = FakeFeatureFactory()
    private val metricsFeatureProvider = fakeFeatureFactory.metricsFeatureProvider

    private val packageInfoPresenter =
        PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, TestScope(), packageManagers)

    private var isUserAuthenticated: Boolean = false

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(Utils::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(Utils.isProtectedPackage(context, PACKAGE_NAME)).thenReturn(false)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
        isUserAuthenticated = false
    }

    @Test
    fun isInterestedAppChange_packageChanged_isInterested() {
        val intent = Intent(Intent.ACTION_PACKAGE_CHANGED).apply {
            data = Uri.parse("package:$PACKAGE_NAME")
        }

        val isInterestedAppChange = packageInfoPresenter.isInterestedAppChange(intent)

        assertThat(isInterestedAppChange).isTrue()
    }

    @Test
    fun isInterestedAppChange_fullyRemoved_notInterested() {
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$PACKAGE_NAME")
            putExtra(Intent.EXTRA_DATA_REMOVED, true)
        }

        val isInterestedAppChange = packageInfoPresenter.isInterestedAppChange(intent)

        assertThat(isInterestedAppChange).isFalse()
    }

    @Test
    fun isInterestedAppChange_removedBeforeReplacing_notInterested() {
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$PACKAGE_NAME")
            putExtra(Intent.EXTRA_REPLACING, true)
        }

        val isInterestedAppChange = packageInfoPresenter.isInterestedAppChange(intent)

        assertThat(isInterestedAppChange).isFalse()
    }

    @Test
    fun isInterestedAppChange_archived_interested() {
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$PACKAGE_NAME")
            putExtra(Intent.EXTRA_ARCHIVAL, true)
        }

        val isInterestedAppChange = packageInfoPresenter.isInterestedAppChange(intent)

        assertThat(isInterestedAppChange).isTrue()
    }

    @Test
    fun enable() = runBlocking {
        packageInfoPresenter.enable()
        delay(100)

        verifyAction(SettingsEnums.ACTION_SETTINGS_ENABLE_APP)
        verify(mockPackageManager).setApplicationEnabledSetting(
            PACKAGE_NAME, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0
        )
    }

    @Test
    fun disable() = runBlocking {
        packageInfoPresenter.disable()
        delay(100)

        verifyDisablePackage()
    }

    @Test
    fun disable_protectedPackage() = runBlocking {
        mockProtectedPackage()
        setAuthPassesAutomatically()

        packageInfoPresenter.disable()
        delay(100)

        verifyUserAuthenticated()
        verifyDisablePackage()
    }

    @Test
    fun startUninstallActivity() = runBlocking {
        packageInfoPresenter.startUninstallActivity()

        verifyUninstallPackage()
    }

    @Test
    fun startUninstallActivity_protectedPackage() = runBlocking {
        mockProtectedPackage()
        setAuthPassesAutomatically()

        packageInfoPresenter.startUninstallActivity()

        verifyUserAuthenticated()
        verifyUninstallPackage()
    }

    @Test
    fun clearInstantApp() = runBlocking {
        packageInfoPresenter.clearInstantApp()
        delay(100)

        verifyAction(SettingsEnums.ACTION_SETTINGS_CLEAR_INSTANT_APP)
        verify(mockPackageManager).deletePackageAsUser(PACKAGE_NAME, null, 0, USER_ID)
    }

    @Test
    fun forceStop() = runBlocking {
        packageInfoPresenter.forceStop()
        delay(100)

        verifyForceStop()
    }

    @Test
    fun forceStop_protectedPackage() = runBlocking {
        mockProtectedPackage()
        setAuthPassesAutomatically()

        packageInfoPresenter.forceStop()
        delay(100)

        verifyUserAuthenticated()
        verifyForceStop()
    }

    @Test
    fun logAction() = runBlocking {
        packageInfoPresenter.logAction(123)

        verifyAction(123)
    }

    private fun verifyAction(category: Int) {
        verify(metricsFeatureProvider).action(context, category, PACKAGE_NAME)
    }

    private fun verifyDisablePackage() {
        verifyAction(SettingsEnums.ACTION_SETTINGS_DISABLE_APP)
        verify(mockPackageManager).setApplicationEnabledSetting(
            PACKAGE_NAME, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0
        )
    }

    private fun verifyUninstallPackage() {
        verifyAction(SettingsEnums.ACTION_SETTINGS_UNINSTALL_APP)

        val intent = argumentCaptor<Intent> {
            verify(context).startActivityAsUser(capture(), any())
        }.firstValue
        with(intent) {
            assertThat(action).isEqualTo(Intent.ACTION_UNINSTALL_PACKAGE)
            assertThat(data?.schemeSpecificPart).isEqualTo(PACKAGE_NAME)
            assertThat(getBooleanExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, true)).isEqualTo(false)
        }
    }

    private fun verifyForceStop() {
        verifyAction(SettingsEnums.ACTION_APP_FORCE_STOP)
        verify(mockActivityManager).forceStopPackageAsUser(PACKAGE_NAME, USER_ID)
    }

    private fun setAuthPassesAutomatically() {
        whenever(mockKeyguardManager.isKeyguardSecure).thenReturn(mockUserAuthentication())
    }

    private fun mockUserAuthentication() : Boolean {
        isUserAuthenticated = true
        return false
    }

    private fun mockProtectedPackage() {
        whenever(Utils.isProtectedPackage(context, PACKAGE_NAME)).thenReturn(true)
    }

    private fun verifyUserAuthenticated() {
        assertThat(isUserAuthenticated).isTrue()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val USER_ID = 0
        val PACKAGE_INFO = PackageInfo()
    }
}
