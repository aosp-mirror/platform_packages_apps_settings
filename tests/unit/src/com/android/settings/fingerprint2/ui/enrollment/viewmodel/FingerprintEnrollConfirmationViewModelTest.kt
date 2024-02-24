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

package com.android.settings.fingerprint2.ui.enrollment.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.settings.biometrics.fingerprint2.lib.model.Default
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollConfirmationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintFlowViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationViewModel
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.SensorStrength
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class FingerprintEnrollConfirmationViewModelTest {
  @JvmField @Rule var rule = MockitoJUnit.rule()

  @get:Rule val instantTaskRule = InstantTaskExecutorRule()
  private var backgroundDispatcher = StandardTestDispatcher()
  private var testScope = TestScope(backgroundDispatcher)
  val fingerprintFlowViewModel = FingerprintFlowViewModel(Default)
  val fakeFingerprintManagerInteractor = FakeFingerprintManagerInteractor()
  lateinit var navigationViewModel: FingerprintNavigationViewModel
  lateinit var underTest: FingerprintEnrollConfirmationViewModel

  @Before
  fun setup() {
    navigationViewModel =
      FingerprintNavigationViewModel(
        FingerprintNavigationStep.Confirmation,
        false,
        fingerprintFlowViewModel,
        fakeFingerprintManagerInteractor,
      )
    underTest =
      FingerprintEnrollConfirmationViewModel(navigationViewModel, fakeFingerprintManagerInteractor)
  }

  @Test
  fun testCanEnrollFingerprints() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensor(0 /* sensorId */, SensorStrength.STRONG, 5, FingerprintSensorType.REAR)
      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()
      fakeFingerprintManagerInteractor.enrollableFingerprints = 5

      var canEnrollFingerprints: Boolean = false
      val job = launch { underTest.isAddAnotherButtonVisible.collect { canEnrollFingerprints = it } }

      advanceUntilIdle()
      assertThat(canEnrollFingerprints).isTrue()
      job.cancel()
    }

  @Test
  fun testNextButtonSendsNextStep() =
    testScope.runTest {
      var step: FingerprintNavigationStep.UiStep? = null
      val job = launch { navigationViewModel.navigateTo.collect { step = it } }

      underTest.onNextButtonClicked()

      runCurrent()

      assertThat(step).isNull()
      job.cancel()
    }

  @Test
  fun testAddAnotherSendsAction() =
    testScope.runTest {
      var step: FingerprintNavigationStep.UiStep? = null
      val job = launch { navigationViewModel.navigateTo.collect { step = it } }

      underTest.onAddAnotherButtonClicked()

      runCurrent()

      assertThat(step).isInstanceOf(FingerprintNavigationStep.Enrollment::class.java)
      job.cancel()
    }
}
