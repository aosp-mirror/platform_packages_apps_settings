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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NrAdvancedCallingPreferenceControllerTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    private val callStateRepository =
        mock<CallStateRepository> { on { isInCallFlow() } doReturn flowOf(false) }

    private val voNrRepository =
        mock<VoNrRepository> { on { isVoNrEnabledFlow(SUB_ID) } doReturn flowOf(true) }

    private val controller =
        NrAdvancedCallingPreferenceController(
                context = context,
                key = TEST_KEY,
                voNrRepository = voNrRepository,
                callStateRepository = callStateRepository,
            )
            .apply { init(SUB_ID) }

    @Test
    fun isChecked_voNrEnabled_on() {
        voNrRepository.stub { on { isVoNrEnabledFlow(SUB_ID) } doReturn flowOf(true) }

        composeTestRule.setContent { controller.Content() }

        composeTestRule
            .onNodeWithText(context.getString(R.string.nr_advanced_calling_title))
            .assertIsOn()
    }

    @Test
    fun isChecked_voNrDisabled_off() {
        voNrRepository.stub { on { isVoNrEnabledFlow(SUB_ID) } doReturn flowOf(false) }

        composeTestRule.setContent { controller.Content() }

        composeTestRule
            .onNodeWithText(context.getString(R.string.nr_advanced_calling_title))
            .assertIsOff()
    }

    @Test
    fun isChangeable_notInCall_changeable() {
        callStateRepository.stub { on { isInCallFlow() } doReturn flowOf(false) }

        composeTestRule.setContent { controller.Content() }

        composeTestRule
            .onNodeWithText(context.getString(R.string.nr_advanced_calling_title))
            .assertIsEnabled()
    }

    @Test
    fun isChangeable_inCall_notChangeable() {
        callStateRepository.stub { on { isInCallFlow() } doReturn flowOf(true) }

        composeTestRule.setContent { controller.Content() }

        composeTestRule
            .onNodeWithText(context.getString(R.string.nr_advanced_calling_title))
            .assertIsNotEnabled()
    }

    @Test
    fun onClick_setVoNrEnabled(): Unit = runBlocking {
        voNrRepository.stub { on { isVoNrEnabledFlow(SUB_ID) } doReturn flowOf(false) }

        composeTestRule.setContent { controller.Content() }
        composeTestRule.onRoot().performClick()

        verify(voNrRepository).setVoNrEnabled(SUB_ID, true)
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 2
    }
}
