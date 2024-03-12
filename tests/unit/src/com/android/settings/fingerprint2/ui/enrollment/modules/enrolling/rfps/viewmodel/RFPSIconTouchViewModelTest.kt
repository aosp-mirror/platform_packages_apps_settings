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

package com.android.settings.fingerprint2.ui.enrollment.modules.enrolling.rfps.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel.RFPSIconTouchViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RFPSIconTouchViewModelTest {
  @JvmField @Rule var rule = MockitoJUnit.rule()

  @get:Rule val instantTaskRule = InstantTaskExecutorRule()

  private var backgroundDispatcher = StandardTestDispatcher()
  private var testScope = TestScope(backgroundDispatcher)
  private lateinit var rfpsIconTouchViewModel: RFPSIconTouchViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(backgroundDispatcher)
    testScope = TestScope(backgroundDispatcher)
    rfpsIconTouchViewModel =
      RFPSIconTouchViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initShouldNotShowDialog() =
    testScope.runTest {
      var shouldShowDialog = false

      val job = launch { rfpsIconTouchViewModel.shouldShowDialog.collect { shouldShowDialog = it } }

      runCurrent()

      assertThat(shouldShowDialog).isFalse()
      job.cancel()
    }

  @Test
  fun shouldShowDialogTest() =
    testScope.runTest {
      var shouldShowDialog = false

      val job = launch { rfpsIconTouchViewModel.shouldShowDialog.collect { shouldShowDialog = it } }

      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      rfpsIconTouchViewModel.userTouchedFingerprintIcon()

      runCurrent()

      assertThat(shouldShowDialog).isTrue()
      job.cancel()
    }

  @Test
  fun stateShouldBeFalseAfterReset() =
    testScope.runTest {
      var shouldShowDialog = false

      val job = launch { rfpsIconTouchViewModel.shouldShowDialog.collect { shouldShowDialog = it } }

      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      rfpsIconTouchViewModel.userTouchedFingerprintIcon()

      runCurrent()

      assertThat(shouldShowDialog).isTrue()

      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      runCurrent()

      assertThat(shouldShowDialog).isFalse()

      job.cancel()
    }

  @Test
  fun toggleMultipleTimes() =
    testScope.runTest {
      var shouldShowDialog = false

      val job = launch { rfpsIconTouchViewModel.shouldShowDialog.collect { shouldShowDialog = it } }

      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      rfpsIconTouchViewModel.userTouchedFingerprintIcon()

      runCurrent()

      assertThat(shouldShowDialog).isTrue()

      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      runCurrent()

      assertThat(shouldShowDialog).isFalse()

      rfpsIconTouchViewModel.userTouchedFingerprintIcon()
      rfpsIconTouchViewModel.userTouchedFingerprintIcon()

      runCurrent()
      assertThat(shouldShowDialog).isTrue()

      job.cancel()
    }
}
