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
package com.android.settings.biometrics2.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics2.ui.viewmodel.FingerprintErrorDialogSetResultAction.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FingerprintEnrollErrorDialogViewModelTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private var viewModel: FingerprintEnrollErrorDialogViewModel =
        FingerprintEnrollErrorDialogViewModel(application, false)

    @Before
    fun setUp() {
        // Make sure viewModel is new for each test
        viewModel = FingerprintEnrollErrorDialogViewModel(application, false)
    }

    @Test
    fun testIsDialogNotShownDefaultFalse() {
        assertThat(viewModel.isDialogShown).isFalse()
    }

    @Test
    fun testIsSuw() {
        assertThat(FingerprintEnrollErrorDialogViewModel(application, false).isSuw).isFalse()
        assertThat(FingerprintEnrollErrorDialogViewModel(application, true).isSuw).isTrue()
    }

    @Test
    fun testNewDialog() = runTest {
        val newDialogs: List<Int> = mutableListOf<Int>().also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.newDialogFlow.toList(it)
            }
        }

        runCurrent()

        // Default values
        assertThat(viewModel.isDialogShown).isFalse()
        assertThat(newDialogs.size).isEqualTo(0)

        val testErrorMsgId = 3456
        viewModel.newDialog(testErrorMsgId)
        runCurrent()

        // verify after emit
        assertThat(viewModel.isDialogShown).isTrue()
        assertThat(newDialogs.size).isEqualTo(1)
        assertThat(newDialogs[0]).isEqualTo(testErrorMsgId)
    }

    @Test
    fun testTriggerRetry() = runTest {
        val triggerRetries: List<Any> = mutableListOf<Any>().also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.triggerRetryFlow.toList(it)
            }
        }

        runCurrent()

        // Default values
        assertThat(triggerRetries.size).isEqualTo(0)

        viewModel.triggerRetry()
        runCurrent()

        // verify after emit
        assertThat(triggerRetries.size).isEqualTo(1)
    }

    @Test
    fun testSetResultFinish() = runTest {
        val setResults: List<FingerprintErrorDialogSetResultAction> =
            mutableListOf<FingerprintErrorDialogSetResultAction>().also {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.setResultFlow.toList(it)
                }
            }

        runCurrent()

        // Default values
        assertThat(setResults.size).isEqualTo(0)

        viewModel.setResultAndFinish(FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH)
        runCurrent()

        // verify after emit
        assertThat(setResults.size).isEqualTo(1)
        assertThat(setResults[0]).isEqualTo(FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH)
    }
}
