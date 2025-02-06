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
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.Utils
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.mockAsUser
import com.android.settingslib.spaprivileged.framework.common.activityManager
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoSession
import org.mockito.Mockito.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PackageInfoPresenterTest {
    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var packageManager: PackageManager

    @Mock
    private lateinit var activityManager: ActivityManager

    @Mock
    private lateinit var packageManagers: IPackageManagers

    @Mock
    private lateinit var keyguardManager: KeyguardManager

    private lateinit var mockSession: MockitoSession

    private val fakeFeatureFactory = FakeFeatureFactory()
    private val metricsFeatureProvider = fakeFeatureFactory.metricsFeatureProvider

    private var isUserAuthenticated: Boolean = false

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(Utils::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()

        context.mockAsUser()
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.activityManager).thenReturn(activityManager)
        whenever(context.getSystemService(KeyguardManager::class.java)).thenReturn(keyguardManager)
        whenever(Utils.isProtectedPackage(context, PACKAGE_NAME)).thenReturn(false)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
        isUserAuthenticated = false
    }


    @Test
    fun enable() = runTest {
        coroutineScope {
            val packageInfoPresenter =
                PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

            packageInfoPresenter.enable()
        }

        verifyAction(SettingsEnums.ACTION_SETTINGS_ENABLE_APP)
        verify(packageManager).setApplicationEnabledSetting(
            PACKAGE_NAME, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0
        )
    }

    @Test
    fun disable() = runTest {
        coroutineScope {
            val packageInfoPresenter =
                PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

            packageInfoPresenter.disable()
        }

        verifyDisablePackage()
    }

    @Test
    fun disable_protectedPackage() = runTest {
        mockProtectedPackage()
        setAuthPassesAutomatically()

        coroutineScope {
            val packageInfoPresenter =
                PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

            packageInfoPresenter.disable()
        }

        verifyUserAuthenticated()
        verifyDisablePackage()
    }

    @Test
    fun startUninstallActivity() = runTest {
        doNothing().`when`(context).startActivityAsUser(any(), any())
        val packageInfoPresenter =
            PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

        packageInfoPresenter.startUninstallActivity()

        verifyUninstallPackage()
    }

    @Test
    fun startUninstallActivity_protectedPackage() = runTest {
        mockProtectedPackage()
        setAuthPassesAutomatically()

        doNothing().`when`(context).startActivityAsUser(any(), any())
        val packageInfoPresenter =
            PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

        packageInfoPresenter.startUninstallActivity()

        verifyUserAuthenticated()
        verifyUninstallPackage()
    }

    @Test
    fun clearInstantApp() = runTest {
        coroutineScope {
            val packageInfoPresenter =
                PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

            packageInfoPresenter.clearInstantApp()
        }

        verifyAction(SettingsEnums.ACTION_SETTINGS_CLEAR_INSTANT_APP)
        verify(packageManager).deletePackageAsUser(PACKAGE_NAME, null, 0, USER_ID)
    }

    @Test
    fun forceStop() = runTest {
        coroutineScope {
            val packageInfoPresenter =
                PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

            packageInfoPresenter.forceStop()
        }

        verifyForceStop()
    }

    @Test
    fun forceStop_protectedPackage() = runTest {
        mockProtectedPackage()
        setAuthPassesAutomatically()

        coroutineScope {
            val packageInfoPresenter =
                PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

            packageInfoPresenter.forceStop()
        }

        verifyUserAuthenticated()
        verifyForceStop()
    }

    @Test
    fun logAction() = runTest {
        val packageInfoPresenter =
            PackageInfoPresenter(context, PACKAGE_NAME, USER_ID, this, packageManagers)

        packageInfoPresenter.logAction(123)

        verifyAction(123)
    }

    private fun verifyAction(category: Int) {
        verify(metricsFeatureProvider).action(context, category, PACKAGE_NAME)
    }

    private fun verifyDisablePackage() {
        verifyAction(SettingsEnums.ACTION_SETTINGS_DISABLE_APP)
        verify(packageManager).setApplicationEnabledSetting(
            PACKAGE_NAME, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0
        )
    }

    private fun verifyUninstallPackage() {
        verifyAction(SettingsEnums.ACTION_SETTINGS_UNINSTALL_APP)
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivityAsUser(intentCaptor.capture(), any())
        with(intentCaptor.value) {
            assertThat(action).isEqualTo(Intent.ACTION_UNINSTALL_PACKAGE)
            assertThat(data?.schemeSpecificPart).isEqualTo(PACKAGE_NAME)
            assertThat(getBooleanExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, true)).isEqualTo(false)
        }
    }

    private fun verifyForceStop() {
        verifyAction(SettingsEnums.ACTION_APP_FORCE_STOP)
        verify(activityManager).forceStopPackageAsUser(PACKAGE_NAME, USER_ID)
    }

    private fun setAuthPassesAutomatically() {
        whenever(keyguardManager.isKeyguardSecure).thenReturn(mockUserAuthentication())
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
    }
}
