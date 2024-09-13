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

package com.android.settings.spa.search

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingKey
import com.android.settings.spa.SpaSearchLanding.SpaSearchLandingSpaPage
import com.android.settings.spa.search.SpaSearchRepository.Companion.createSearchIndexableData
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class SpaSearchRepositoryTest {

    @Test
    fun createSearchIndexableData() {
        val pageProvider =
            object : SettingsPageProvider {
                override val name = PAGE_NAME
            }

        val searchIndexableData =
            pageProvider.createSearchIndexableData({ PAGE_TITLE }) { listOf(ITEM_TITLE) }
        val dynamicRawDataToIndex =
            searchIndexableData.searchIndexProvider.getDynamicRawDataToIndex(mock<Context>(), true)

        assertThat(searchIndexableData.targetClass).isEqualTo(pageProvider::class.java)
        assertThat(dynamicRawDataToIndex).hasSize(1)
        val rawData = dynamicRawDataToIndex[0]
        val key = decodeToSpaSearchLandingKey(rawData.key)
        assertThat(key)
            .isEqualTo(
                SpaSearchLandingKey.newBuilder()
                    .setSpaPage(SpaSearchLandingSpaPage.newBuilder().setDestination(PAGE_NAME))
                    .build())
        assertThat(rawData.title).isEqualTo(ITEM_TITLE)
        assertThat(rawData.intentAction).isEqualTo("android.settings.SPA_SEARCH_LANDING")
        assertThat(rawData.intentTargetClass)
            .isEqualTo(SpaSearchLandingActivity::class.qualifiedName)
        assertThat(rawData.className).isEqualTo(pageProvider::class.java.name)
        assertThat(rawData.screenTitle).isEqualTo(PAGE_TITLE)
    }

    private companion object {
        const val PAGE_NAME = "PageName"
        const val PAGE_TITLE = "Page Title"
        const val ITEM_TITLE = "Item Title"
    }
}
