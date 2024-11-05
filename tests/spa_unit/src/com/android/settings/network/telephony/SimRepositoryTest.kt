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

package com.android.settings.network.telephony

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SimRepositoryTest {

    private val mockUserManager = mock<UserManager>()

    private val mockPackageManager = mock<PackageManager>()

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { userManager } doReturn mockUserManager
            on { packageManager } doReturn mockPackageManager
        }

    private val repository = SimRepository(context)

    @Test
    fun showMobileNetworkPageEntrance_adminUserAndHasTelephony_returnTrue() {
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn true
        }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isTrue()
    }

    @Test
    fun showMobileNetworkPageEntrance_notAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn true
        }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isFalse()
    }

    @Test
    fun showMobileNetworkPageEntrance_noTelephony_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn false
        }

        val showMobileNetworkPage = repository.showMobileNetworkPageEntrance()

        assertThat(showMobileNetworkPage).isFalse()
    }

    @Test
    fun canEnterMobileNetworkPage_allowConfigMobileNetwork_returnTrue() {
        mockUserManager.stub {
            on { isAdminUser } doReturn true
            on { hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS) } doReturn false
        }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn true
        }

        val showMobileNetworkPage = repository.canEnterMobileNetworkPage()

        assertThat(showMobileNetworkPage).isTrue()
    }

    @Test
    fun canEnterMobileNetworkPage_disallowConfigMobileNetwork_returnFalse() {
        mockUserManager.stub {
            on { isAdminUser } doReturn true
            on { hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS) } doReturn true
        }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn true
        }

        val showMobileNetworkPage = repository.canEnterMobileNetworkPage()

        assertThat(showMobileNetworkPage).isFalse()
    }
}
