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

package com.android.settings.fingerprint2.enrollment.viewmodel

import android.content.Context
import android.content.res.Configuration
import android.view.accessibility.AccessibilityManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.android.settings.biometrics.fingerprint2.shared.model.Default
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.AccessibilityViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.BackgroundViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.Education
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FoldStateViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.NextStepViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.OrientationStateViewModel
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.SensorStrength
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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

/** consistent with [ScreenSizeFoldProvider.INNER_SCREEN_SMALLEST_SCREEN_WIDTH_THRESHOLD_DP] */
private const val INNER_SCREEN_SMALLEST_SCREEN_WIDTH_THRESHOLD_DP = 600

@RunWith(MockitoJUnitRunner::class)
class FingerprintEnrollFindSensorViewModelV2Test {
  @JvmField @Rule var rule = MockitoJUnit.rule()
  @get:Rule val instantTaskRule = InstantTaskExecutorRule()

  private var backgroundDispatcher = StandardTestDispatcher()
  private var testScope = TestScope(backgroundDispatcher)
  private lateinit var fakeFingerprintManagerInteractor: FakeFingerprintManagerInteractor
  private lateinit var gatekeeperViewModel: FingerprintGatekeeperViewModel
  private lateinit var enrollViewModel: FingerprintEnrollViewModel
  private lateinit var navigationViewModel: FingerprintEnrollNavigationViewModel
  private lateinit var accessibilityViewModel: AccessibilityViewModel
  private lateinit var foldStateViewModel: FoldStateViewModel
  private lateinit var orientationStateViewModel: OrientationStateViewModel
  private lateinit var underTest: FingerprintEnrollFindSensorViewModel
  private lateinit var backgroundViewModel: BackgroundViewModel
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val accessibilityManager: AccessibilityManager =
    context.getSystemService(AccessibilityManager::class.java)!!

  @Before
  fun setup() {
    backgroundDispatcher = StandardTestDispatcher()
    testScope = TestScope(backgroundDispatcher)
    Dispatchers.setMain(backgroundDispatcher)

    fakeFingerprintManagerInteractor = FakeFingerprintManagerInteractor()
    gatekeeperViewModel =
      FingerprintGatekeeperViewModel.FingerprintGatekeeperViewModelFactory(
          null,
          fakeFingerprintManagerInteractor
        )
        .create(FingerprintGatekeeperViewModel::class.java)
    navigationViewModel =
      FingerprintEnrollNavigationViewModel.FingerprintEnrollNavigationViewModelFactory(
          backgroundDispatcher,
          fakeFingerprintManagerInteractor,
          gatekeeperViewModel,
          canSkipConfirm = true,
          Default,
        )
        .create(FingerprintEnrollNavigationViewModel::class.java)

    backgroundViewModel =
      BackgroundViewModel.BackgroundViewModelFactory().create(BackgroundViewModel::class.java)
    backgroundViewModel.inForeground()
    enrollViewModel =
      FingerprintEnrollViewModel.FingerprintEnrollViewModelFactory(
          fakeFingerprintManagerInteractor,
          gatekeeperViewModel,
          navigationViewModel,
        )
        .create(FingerprintEnrollViewModel::class.java)
    accessibilityViewModel =
      AccessibilityViewModel.AccessibilityViewModelFactory(accessibilityManager)
        .create(AccessibilityViewModel::class.java)
    foldStateViewModel =
      FoldStateViewModel.FoldStateViewModelFactory(context).create(FoldStateViewModel::class.java)
    orientationStateViewModel =
      OrientationStateViewModel.OrientationViewModelFactory(context)
        .create(OrientationStateViewModel::class.java)
    underTest =
      FingerprintEnrollFindSensorViewModel.FingerprintEnrollFindSensorViewModelFactory(
          navigationViewModel,
          enrollViewModel,
          gatekeeperViewModel,
          backgroundViewModel,
          accessibilityViewModel,
          foldStateViewModel,
          orientationStateViewModel
        )
        .create(FingerprintEnrollFindSensorViewModel::class.java)

    // Navigate to Education page
    navigationViewModel.nextStep()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // TODO(b/305094585): test enroll() logic

  @Test
  fun currentStepIsEducation() =
    testScope.runTest {
      var step: NextStepViewModel? = null
      val job = launch {
        navigationViewModel.navigationViewModel.collectLatest { step = it.currStep }
      }
      advanceUntilIdle()
      assertThat(step).isEqualTo(Education)
      job.cancel()
    }

  @Test
  fun udfpsLottieInfo() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensor(
          0 /* sensorId */,
          SensorStrength.STRONG,
          5,
          FingerprintSensorType.UDFPS_OPTICAL
        )

      var udfpsLottieInfo: Boolean? = null
      val job = launch { underTest.udfpsLottieInfo.collect { udfpsLottieInfo = it } }

      advanceUntilIdle()
      assertThat(udfpsLottieInfo).isNotNull()
      job.cancel()
    }

  @Test
  fun sfpsLottieInfoWhenFolded() =
    testScope.runTest {
      var isFolded = false
      var rotation: Int = -1
      val job = launch {
        underTest.sfpsLottieInfo.collect {
          isFolded = it.first
          rotation = it.second
        }
      }

      val config = createConfiguration(isFolded = true)
      foldStateViewModel.onConfigurationChange(config)
      advanceUntilIdle()
      assertThat(isFolded).isTrue()
      assertThat(rotation).isEqualTo(context.display!!.rotation)
      job.cancel()
    }

  @Test
  fun sfpsLottieInfoWhenUnFolded() =
    testScope.runTest {
      var isFolded = false
      var rotation: Int = -1
      val job = launch {
        underTest.sfpsLottieInfo.collect {
          isFolded = it.first
          rotation = it.second
        }
      }

      val config = createConfiguration(isFolded = false)
      foldStateViewModel.onConfigurationChange(config)
      advanceUntilIdle()
      assertThat(isFolded).isFalse()
      assertThat(rotation).isEqualTo(context.display!!.rotation)
      job.cancel()
    }

  @Test
  fun rfpsAnimation() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensor(0 /* sensorId */, SensorStrength.STRONG, 5, FingerprintSensorType.REAR)

      var showRfpsAnimation: Boolean? = null
      val job = launch { underTest.showRfpsAnimation.collect { showRfpsAnimation = it } }

      advanceUntilIdle()
      assertThat(showRfpsAnimation).isTrue()
      job.cancel()
    }

  @Test
  fun showPrimaryButton_ifUdfps() =
    testScope.runTest {
      fakeFingerprintManagerInteractor.sensorProp =
        FingerprintSensor(
          0 /* sensorId */,
          SensorStrength.STRONG,
          5,
          FingerprintSensorType.UDFPS_OPTICAL
        )

      var showPrimaryButton: Boolean? = null
      val job = launch { underTest.showPrimaryButton.collect { showPrimaryButton = it } }

      advanceUntilIdle()
      assertThat(showPrimaryButton).isTrue()
      job.cancel()
    }

  @Test
  fun doesNotShowPrimaryButton_ifNonUdfps() =
    testScope.runTest {
      var showPrimaryButton: Boolean? = null
      val job = launch { underTest.showPrimaryButton.collect { showPrimaryButton = it } }

      advanceUntilIdle()
      assertThat(showPrimaryButton).isNull()
      job.cancel()
    }

  private fun createConfiguration(isFolded: Boolean): Configuration {
    val config = Configuration()
    config.smallestScreenWidthDp =
      if (isFolded) INNER_SCREEN_SMALLEST_SCREEN_WIDTH_THRESHOLD_DP - 1
      else INNER_SCREEN_SMALLEST_SCREEN_WIDTH_THRESHOLD_DP + 1
    return config
  }
}
