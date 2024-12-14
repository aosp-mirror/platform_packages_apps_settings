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

package com.android.settings.network

import android.telephony.SubscriptionInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimOnboardingServiceTest {

    @Test
    fun addItemForRenaming_addItemWithNewName_findItem() {
        val simOnboardingService = SimOnboardingService()
        val newName = "NewName"

        simOnboardingService.addItemForRenaming(SUB_INFO_1, newName)

        assertThat(simOnboardingService.renameMutableMap)
            .containsEntry(SUB_INFO_1.subscriptionId, newName)
    }

    @Test
    fun addItemForRenaming_sameNameAndItemNotInList_removeItem() {
        val simOnboardingService = SimOnboardingService()

        simOnboardingService.addItemForRenaming(SUB_INFO_1, DISPLAY_NAME_1)

        assertThat(simOnboardingService.renameMutableMap)
            .doesNotContainKey(SUB_INFO_1.subscriptionId)
    }

    @Test
    fun addItemForRenaming_sameNameAndItemInList_removeItem() {
        val simOnboardingService = SimOnboardingService()
        simOnboardingService.renameMutableMap[SUB_INFO_1.subscriptionId] = "NewName"

        simOnboardingService.addItemForRenaming(SUB_INFO_1, DISPLAY_NAME_1)

        assertThat(simOnboardingService.renameMutableMap)
            .doesNotContainKey(SUB_INFO_1.subscriptionId)
    }

    private companion object {
        const val SUB_ID_1 = 1
        const val DISPLAY_NAME_1 = "Sub 1"

        val SUB_INFO_1: SubscriptionInfo = SubscriptionInfo.Builder().apply {
            setId(SUB_ID_1)
            setDisplayName(DISPLAY_NAME_1)
        }.build()
    }
}