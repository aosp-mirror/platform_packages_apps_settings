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

package com.android.settings.fingerprint2.ui.enrollment.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.android.settings.biometrics.fingerprint2.lib.model.Default
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.BackgroundViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintFlowViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Enrollment
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.GatekeeperInfo
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.NavigationState
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.SensorStrength
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
class FingerprintEnrollEnrollingViewModelTest {
  @JvmField @Rule var rule = MockitoJUnit.rule()

  @get:Rule val instantTaskRule = InstantTaskExecutorRule()

  private var backgroundDispatcher = StandardTestDispatcher()
  private lateinit var enrollEnrollingViewModel: FingerprintEnrollEnrollingViewModel
  private lateinit var backgroundViewModel: BackgroundViewModel
  private lateinit var gateKeeperViewModel: FingerprintGatekeeperViewModel
  private lateinit var navigationViewModel: FingerprintNavigationViewModel
  private val defaultGatekeeperInfo = GatekeeperInfo.GatekeeperPasswordInfo(byteArrayOf(1, 3), 3)
  private var testScope = TestScope(backgroundDispatcher)

  private lateinit var fakeFingerprintManagerInteractor: FakeFingerprintManagerInteractor

  private fun initialize(gatekeeperInfo: GatekeeperInfo = defaultGatekeeperInfo) {
    fakeFingerprintManagerInteractor = FakeFingerprintManagerInteractor()
    gateKeeperViewModel =
      FingerprintGatekeeperViewModel.FingerprintGatekeeperViewModelFactory(
          gatekeeperInfo,
          fakeFingerprintManagerInteractor,
        )
        .create(FingerprintGatekeeperViewModel::class.java)
    val sensor = FingerprintSensor(1, SensorStrength.STRONG, 5, FingerprintSensorType.POWER_BUTTON)
    val fingerprintFlowViewModel = FingerprintFlowViewModel(Default)

    navigationViewModel =
      FingerprintNavigationViewModel(
        Enrollment(sensor),
        false,
        fingerprintFlowViewModel,
        fakeFingerprintManagerInteractor,
      )

    backgroundViewModel =
      BackgroundViewModel.BackgroundViewModelFactory().create(BackgroundViewModel::class.java)
    backgroundViewModel.inForeground()
    val fingerprintEnrollViewModel =
      FingerprintEnrollViewModel.FingerprintEnrollViewModelFactory(
          fakeFingerprintManagerInteractor,
          gateKeeperViewModel,
          navigationViewModel,
        )
        .create(FingerprintEnrollViewModel::class.java)
    enrollEnrollingViewModel =
      FingerprintEnrollEnrollingViewModel.FingerprintEnrollEnrollingViewModelFactory(
          fingerprintEnrollViewModel,
          backgroundViewModel,
        )
        .create(FingerprintEnrollEnrollingViewModel::class.java)
  }

  @Before
  fun setup() {
    Dispatchers.setMain(backgroundDispatcher)
    initialize()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun testEnrollShouldBeFalse() =
    testScope.runTest {
      var shouldEnroll = false

      val job = launch {
        enrollEnrollingViewModel.enrollFlowShouldBeRunning.collect { shouldEnroll = it }
      }

      assertThat(shouldEnroll).isFalse()
      runCurrent()

      enrollEnrollingViewModel.canEnroll()
      runCurrent()

      assertThat(shouldEnroll).isTrue()
      job.cancel()
    }

  @Test
  fun testEnrollShouldBeFalseWhenBackground() =
    testScope.runTest {
      var shouldEnroll = false

      val job = launch {
        enrollEnrollingViewModel.enrollFlowShouldBeRunning.collect { shouldEnroll = it }
      }

      assertThat(shouldEnroll).isFalse()
      runCurrent()

      enrollEnrollingViewModel.canEnroll()
      runCurrent()

      assertThat(shouldEnroll).isTrue()

      backgroundViewModel.wentToBackground()
      runCurrent()
      assertThat(shouldEnroll).isFalse()

      job.cancel()
    }
}
