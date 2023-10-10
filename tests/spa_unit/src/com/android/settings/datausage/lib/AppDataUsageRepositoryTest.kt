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
import android.content.pm.UserInfo
import android.content.res.Resources
import android.net.NetworkPolicyManager
import android.net.NetworkTemplate
import android.os.UserHandle
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.datausage.lib.NetworkStatsRepository.Companion.Bucket
import com.android.settingslib.AppItem
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class AppDataUsageRepositoryTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    private val mockUserManager = mock<UserManager> {
        on { userProfiles } doReturn listOf(UserHandle.of(USER_ID))
        on { getUserInfo(USER_ID) } doReturn UserInfo(USER_ID, "", 0)
    }

    private val mockNetworkPolicyManager = mock<NetworkPolicyManager> {
        on { getUidsWithPolicy(NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND) } doReturn
            intArrayOf()
    }

    private val mockResources = mock<Resources> {
        on { getIntArray(R.array.datausage_hiding_carrier_service_carrier_id) } doReturn
            intArrayOf(HIDING_CARRIER_ID)

        on { getStringArray(R.array.datausage_hiding_carrier_service_package_names) } doReturn
            arrayOf(HIDING_PACKAGE_NAME)
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { userManager } doReturn mockUserManager
        on { getSystemService(NetworkPolicyManager::class.java) } doReturn mockNetworkPolicyManager
        on { resources } doReturn mockResources
    }

    @Test
    fun getAppPercent_noAppToHide() {
        val repository = AppDataUsageRepository(
            context = context,
            currentUserId = USER_ID,
            template = Template,
            getPackageName = { null },
        )
        val buckets = listOf(
            Bucket(uid = APP_ID_1, bytes = 1),
            Bucket(uid = APP_ID_2, bytes = 2),
        )

        val appPercentList = repository.getAppPercent(null, buckets)

        assertThat(appPercentList).hasSize(2)
        appPercentList[0].first.apply {
            assertThat(key).isEqualTo(APP_ID_2)
            assertThat(category).isEqualTo(AppItem.CATEGORY_APP)
            assertThat(total).isEqualTo(2)
        }
        assertThat(appPercentList[0].second).isEqualTo(100)
        appPercentList[1].first.apply {
            assertThat(key).isEqualTo(APP_ID_1)
            assertThat(category).isEqualTo(AppItem.CATEGORY_APP)
            assertThat(total).isEqualTo(1)
        }
        assertThat(appPercentList[1].second).isEqualTo(50)
    }

    @Test
    fun getAppPercent_hasAppToHide() {
        val repository = AppDataUsageRepository(
            context = context,
            currentUserId = USER_ID,
            template = Template,
            getPackageName = { if (it.key == APP_ID_1) HIDING_PACKAGE_NAME else null },
        )
        val buckets = listOf(
            Bucket(uid = APP_ID_1, bytes = 1),
            Bucket(uid = APP_ID_2, bytes = 2),
        )

        val appPercentList = repository.getAppPercent(HIDING_CARRIER_ID, buckets)

        assertThat(appPercentList).hasSize(1)
        appPercentList[0].first.apply {
            assertThat(key).isEqualTo(APP_ID_2)
            assertThat(category).isEqualTo(AppItem.CATEGORY_APP)
            assertThat(total).isEqualTo(2)
        }
        assertThat(appPercentList[0].second).isEqualTo(100)
    }

    private companion object {
        const val USER_ID = 1
        const val APP_ID_1 = 110001
        const val APP_ID_2 = 110002
        const val HIDING_CARRIER_ID = 4
        const val HIDING_PACKAGE_NAME = "hiding.package.name"

        val Template: NetworkTemplate = mock<NetworkTemplate>()
    }
}
