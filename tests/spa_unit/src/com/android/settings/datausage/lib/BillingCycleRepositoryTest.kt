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

package com.android.settings.datausage.lib

import android.content.Context
import android.os.INetworkManagementService
import android.os.UserManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BillingCycleRepositoryTest {

    private val mockNetworkManagementService = mock<INetworkManagementService> {
        on { isBandwidthControlEnabled } doReturn true
    }

    private val mockUserManager = mock<UserManager> {
        on { isAdminUser } doReturn true
    }

    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
        on { isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER) } doReturn false
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { userManager } doReturn mockUserManager
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val repository = BillingCycleRepository(context, mockNetworkManagementService)

    @Test
    fun isModifiable_bandwidthControlDisabled_returnFalse() {
        whenever(mockNetworkManagementService.isBandwidthControlEnabled).thenReturn(false)

        val modifiable = repository.isModifiable(SUB_ID)

        assertThat(modifiable).isFalse()
    }

    @Test
    fun isModifiable_notAdminUser_returnFalse() {
        whenever(mockUserManager.isAdminUser).thenReturn(false)

        val modifiable = repository.isModifiable(SUB_ID)

        assertThat(modifiable).isFalse()
    }

    @Test
    fun isModifiable_dataDisabled_returnFalse() {
        whenever(
            mockTelephonyManager.isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER)
        ).thenReturn(false)

        val modifiable = repository.isModifiable(SUB_ID)

        assertThat(modifiable).isFalse()
    }

    @Test
    fun isModifiable_meetAllRequirements_returnTrue() {
        whenever(mockNetworkManagementService.isBandwidthControlEnabled).thenReturn(true)
        whenever(mockUserManager.isAdminUser).thenReturn(true)
        whenever(
            mockTelephonyManager.isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER)
        ).thenReturn(true)

        val modifiable = repository.isModifiable(SUB_ID)

        assertThat(modifiable).isTrue()
    }

    @Test
    fun isBandwidthControlEnabled_bandwidthControlDisabled_returnFalse() {
        whenever(mockNetworkManagementService.isBandwidthControlEnabled).thenReturn(false)

        val enabled = repository.isBandwidthControlEnabled()

        assertThat(enabled).isFalse()
    }

    @Test
    fun isBandwidthControlEnabled_bandwidthControlEnabled_returnTrue() {
        whenever(mockNetworkManagementService.isBandwidthControlEnabled).thenReturn(true)

        val enabled = repository.isBandwidthControlEnabled()

        assertThat(enabled).isTrue()
    }

    private companion object {
        const val SUB_ID = 1
    }
}
