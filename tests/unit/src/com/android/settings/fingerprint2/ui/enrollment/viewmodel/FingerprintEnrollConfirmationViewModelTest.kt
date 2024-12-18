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

import android.hardware.biometrics.ComponentInfoInternal
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.settings.biometrics.fingerprint2.lib.model.Default
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintAction
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollConfirmationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintFlowViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationViewModel
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class FingerprintEnrollConfirmationViewModelTest {
  @JvmField @Rule var rule = MockitoJUnit.rule()

  @get:Rule val instantTaskRule = InstantTaskExecutorRule()
  private var backgroundDispatcher = StandardTestDispatcher()
  private var testScope = TestScope(backgroundDispatcher)
  val fingerprintFlowViewModel = FingerprintFlowViewModel()
  val fakeFingerprintManagerInteractor = FakeFingerprintManagerInteractor()
  lateinit var navigationViewModel: FingerprintNavigationViewModel
  lateinit var underTest: FingerprintEnrollConfirmationViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(backgroundDispatcher)
    fingerprintFlowViewModel.updateFlowType(Default)
    navigationViewModel = FingerprintNavigationViewModel(fakeFingerprintManagerInteractor)
    underTest =
      FingerprintEnrollConfirmationViewModel(navigationViewModel, fakeFingerprintManagerInteractor)
    navigationViewModel.updateFingerprintFlow(Default)
    navigationViewModel.hasConfirmedDeviceCredential(true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun bringToConfirmation() {
    navigationViewModel.update(
      FingerprintAction.NEXT,
      FingerprintNavigationStep.Introduction::class,
      "Intro.Test.NEXT",
    )
    navigationViewModel.update(
      FingerprintAction.NEXT,
      FingerprintNavigationStep.Education::class,
      "Edu.Test.NEXT",
    )
    navigationViewModel.update(
      FingerprintAction.NEXT,
      FingerprintNavigationStep.Enrollment::class,
      "Enrollment.Test.NEXT",
    )
  }

  @Test
  fun testCanEnrollFingerprints() =
    testScope.runTest {
      advanceUntilIdle()
      bringToConfirmation()
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensorPropertiesInternal(
            0 /* sensorId */,
            SensorProperties.STRENGTH_STRONG,
            5 /* maxEnrollmentsPerUser */,
            listOf<ComponentInfoInternal>(),
            FingerprintSensorProperties.TYPE_POWER_BUTTON,
            false /* halControlsIllumination */,
            true /* resetLockoutRequiresHardwareAuthToken */,
            listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
          )
          .toFingerprintSensor()

      fakeFingerprintManagerInteractor.enrolledFingerprintsInternal = mutableListOf()
      fakeFingerprintManagerInteractor.enrollableFingerprints = 5

      var canEnrollFingerprints: Boolean = false
      val job = launch {
        underTest.isAddAnotherButtonVisible.collect { canEnrollFingerprints = it }
      }

      advanceUntilIdle()
      assertThat(canEnrollFingerprints).isTrue()
      job.cancel()
    }

  @Test
  fun testNextButtonSendsNextStep() =
    testScope.runTest {
      advanceUntilIdle()
      bringToConfirmation()
      var step: FingerprintNavigationStep.UiStep? = null
      val job = launch { navigationViewModel.navigateTo.collect { step = it } }

      underTest.onNextButtonClicked()

      advanceUntilIdle()

      assertThat(step).isNull()
      job.cancel()
    }

  @Test
  fun testAddAnotherSendsAction() =
    testScope.runTest {
      advanceUntilIdle()
      bringToConfirmation()
      advanceUntilIdle()

      var step: FingerprintNavigationStep.UiStep? = null
      val job = launch { navigationViewModel.navigateTo.collect { step = it } }

      underTest.onAddAnotherButtonClicked()

      advanceUntilIdle()

      assertThat(step).isNull()
      job.cancel()
    }
}
