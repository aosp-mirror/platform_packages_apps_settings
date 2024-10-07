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

import android.content.Context
import android.text.BidiFormatter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.network.telephony.SimRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class TopLevelNetworkEntryPreferenceControllerTest {

    private val mockSimRepository = mock<SimRepository>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private var isDemoUser = false
    private var isEmbeddingActivityEnabled = false

    private var controller =
        TopLevelNetworkEntryPreferenceController(
            context = context,
            preferenceKey = TEST_KEY,
            simRepository = mockSimRepository,
            isDemoUser = { isDemoUser },
            isEmbeddingActivityEnabled = { isEmbeddingActivityEnabled },
        )

    @Test
    fun getAvailabilityStatus_demoUser_largeScreen_unsupported() {
        isDemoUser = true
        isEmbeddingActivityEnabled = true

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getAvailabilityStatus_demoUser_nonLargeScreen_unsupported() {
        isDemoUser = true
        isEmbeddingActivityEnabled = false

        val availabilityStatus = controller.availabilityStatus

        assertThat(availabilityStatus).isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    fun getSummary_hasMobile_shouldReturnMobileSummary() {
        mockSimRepository.stub { on { showMobileNetworkPageEntrance() } doReturn true }

        val summary = controller.summary

        assertThat(summary)
            .isEqualTo(
                BidiFormatter.getInstance()
                    .unicodeWrap(context.getString(R.string.network_dashboard_summary_mobile))
            )
    }

    @Test
    fun getSummary_noMobile_shouldReturnNoMobileSummary() {
        mockSimRepository.stub { on { showMobileNetworkPageEntrance() } doReturn false }

        val summary = controller.summary

        assertThat(summary)
            .isEqualTo(
                BidiFormatter.getInstance()
                    .unicodeWrap(context.getString(R.string.network_dashboard_summary_no_mobile))
            )
    }

    private companion object {
        const val TEST_KEY = "test_key"
    }
}
