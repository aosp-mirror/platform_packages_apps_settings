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
import com.android.settings.biometrics2.ui.viewmodel.FingerprintErrorDialogSetResultAction.FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testNewDialog() = runTest {
        backgroundScope.launch {
            mutableListOf<Any>().let { list ->
                viewModel.newDialogFlow.toList(list)
                assertThat(list.size).isEqualTo(0)
            }

            mutableListOf<FingerprintErrorDialogSetResultAction>().let { list ->
                val testErrorMsgId = 3456
                viewModel.newDialog(testErrorMsgId)

                assertThat(viewModel.isDialogShown).isTrue()
                viewModel.setResultFlow.toList(list)
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0]).isEqualTo(testErrorMsgId)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTriggerRetry() = runTest {
        backgroundScope.launch {
            // triggerRetryFlow shall be empty on begin
            mutableListOf<Any>().let { list ->
                viewModel.triggerRetryFlow.toList(list)
                assertThat(list.size).isEqualTo(0)
            }

            // emit newDialog
            mutableListOf<FingerprintErrorDialogSetResultAction>().let { list ->
                viewModel.newDialog(0)
                viewModel.triggerRetry()

                assertThat(viewModel.isDialogShown).isFalse()
                viewModel.setResultFlow.toList(list)
                assertThat(list.size).isEqualTo(1)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSetResultFinish() = runTest {
        backgroundScope.launch {
            // setResultFlow shall be empty on begin
            mutableListOf<FingerprintErrorDialogSetResultAction>().let { list ->
                viewModel.setResultFlow.toList(list)
                assertThat(list.size).isEqualTo(0)
            }

            // emit FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH
            viewModel = FingerprintEnrollErrorDialogViewModel(application, false)
            mutableListOf<FingerprintErrorDialogSetResultAction>().let { list ->
                viewModel.newDialog(0)
                viewModel.setResultAndFinish(FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH)

                assertThat(viewModel.isDialogShown).isFalse()
                viewModel.setResultFlow.toList(list)
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0]).isEqualTo(FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH)
            }

            // emit FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT
            viewModel = FingerprintEnrollErrorDialogViewModel(application, false)
            mutableListOf<FingerprintErrorDialogSetResultAction>().let { list ->
                viewModel.newDialog(0)
                viewModel.setResultAndFinish(FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_TIMEOUT)

                assertThat(viewModel.isDialogShown).isFalse()
                viewModel.setResultFlow.toList(list)
                assertThat(list.size).isEqualTo(1)
                assertThat(list[0]).isEqualTo(FINGERPRINT_ERROR_DIALOG_ACTION_SET_RESULT_FINISH)
            }
        }
    }
}
