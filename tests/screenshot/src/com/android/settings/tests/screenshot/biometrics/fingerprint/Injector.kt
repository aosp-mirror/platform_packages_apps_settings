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
package com.android.settings.tests.screenshot.biometrics.fingerprint

import android.content.res.Configuration
import android.hardware.biometrics.ComponentInfoInternal
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.Default
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel.RFPSIconTouchViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.viewmodel.RFPSViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.BackgroundViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollConfirmationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollFindSensorViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollIntroViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintFlowViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintScrollViewModel
import com.android.settings.testutils2.FakeFingerprintManagerInteractor
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.DisplaySpec
import platform.test.screenshot.FragmentScreenshotTestRule
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.matchers.PixelPerfectMatcher

class Injector(step: FingerprintNavigationStep.UiStep) {

  var enrollFlow = Default
  var fingerprintSensor =
    FingerprintSensorPropertiesInternal(
        0 /* sensorId */,
        SensorProperties.STRENGTH_STRONG,
        5 /* maxEnrollmentsPerUser */,
        listOf<ComponentInfoInternal>(),
        FingerprintSensorProperties.TYPE_REAR,
        false /* halControlsIllumination */,
        true /* resetLockoutRequiresHardwareAuthToken */,
        listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
      )
      .toFingerprintSensor()

  var accessibilityInteractor =
    object : AccessibilityInteractor {
      override val isAccessibilityEnabled: Flow<Boolean> = flowOf(true)
    }

  var foldStateInteractor =
    object : FoldStateInteractor {
      private val _foldState = MutableStateFlow(false)
      override val isFolded: Flow<Boolean> = _foldState.asStateFlow()

      override fun onConfigurationChange(newConfig: Configuration) {
        _foldState.update { false }
      }
    }

  var orientationInteractor =
    object : OrientationInteractor {
      override val orientation: Flow<Int> = flowOf(Configuration.ORIENTATION_LANDSCAPE)
      override val rotation: Flow<Int> = flowOf(Surface.ROTATION_0)
      override val rotationFromDefault: Flow<Int> = rotation

      override fun getRotationFromDefault(rotation: Int): Int = rotation
    }
  var gatekeeperViewModel = FingerprintGatekeeperViewModel(fingerprintManagerInteractor)

  val flowViewModel = FingerprintFlowViewModel()

  var navigationViewModel = FingerprintNavigationViewModel(fingerprintManagerInteractor)
  var fingerprintViewModel =
    FingerprintEnrollIntroViewModel(
      navigationViewModel,
      flowViewModel,
      fingerprintManagerInteractor,
    )

  var fingerprintScrollViewModel = FingerprintScrollViewModel()
  var backgroundViewModel = BackgroundViewModel()

  var fingerprintEnrollViewModel =
    FingerprintEnrollViewModel(
      gatekeeperViewModel,
      navigationViewModel,
      fingerprintManagerInteractor,
      fingerprintManagerInteractor,
    )

  var fingerprintEnrollEnrollingViewModel =
    FingerprintEnrollEnrollingViewModel(fingerprintEnrollViewModel, backgroundViewModel)

  var rfpsIconTouchViewModel = RFPSIconTouchViewModel()
  var rfpsViewModel =
    RFPSViewModel(
      fingerprintEnrollEnrollingViewModel,
      navigationViewModel,
      orientationInteractor,
      fingerprintManagerInteractor,
    )

  val fingerprintEnrollConfirmationViewModel =
    FingerprintEnrollConfirmationViewModel(navigationViewModel, fingerprintManagerInteractor)

  var fingerprintFindSensorViewModel =
    FingerprintEnrollFindSensorViewModel(
      navigationViewModel,
      fingerprintEnrollViewModel,
      gatekeeperViewModel,
      backgroundViewModel,
      flowViewModel,
      accessibilityInteractor,
      foldStateInteractor,
      orientationInteractor,
      fingerprintManagerInteractor,
    )

  val factory =
    object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
          FingerprintEnrollIntroViewModel::class.java -> fingerprintViewModel
          FingerprintScrollViewModel::class.java -> fingerprintScrollViewModel
          FingerprintNavigationViewModel::class.java -> navigationViewModel
          FingerprintGatekeeperViewModel::class.java -> gatekeeperViewModel
          FingerprintEnrollFindSensorViewModel::class.java -> fingerprintFindSensorViewModel
          FingerprintEnrollViewModel::class.java -> fingerprintEnrollViewModel
          RFPSViewModel::class.java -> rfpsViewModel
          BackgroundViewModel::class.java -> backgroundViewModel
          RFPSIconTouchViewModel::class.java -> rfpsIconTouchViewModel
          FingerprintEnrollEnrollingViewModel::class.java -> fingerprintEnrollEnrollingViewModel
          FingerprintEnrollConfirmationViewModel::class.java ->
            fingerprintEnrollConfirmationViewModel
          else -> null
        }
          as T
      }
    }

  init {
    fingerprintEnrollViewModel.sensorTypeCached = fingerprintSensor.sensorType
    gatekeeperViewModel.onConfirmDevice(true, 100L)
    navigationViewModel.updateFingerprintFlow(enrollFlow)
    navigationViewModel.hasConfirmedDeviceCredential(true)
    flowViewModel.updateFlowType(enrollFlow)
  }

  companion object {
    private val Phone = DisplaySpec("phone", width = 1080, height = 2340, densityDpi = 420)
    private const val screenshotPath = "/settings_screenshots"
    val fingerprintManagerInteractor = FakeFingerprintManagerInteractor()

    fun BiometricFragmentScreenShotRule() =
      FragmentScreenshotTestRule(
        DeviceEmulationSpec.forDisplays(Phone).first(),
        GoldenPathManager(
          InstrumentationRegistry.getInstrumentation().context,
          InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath +
            screenshotPath,
        ),
        PixelPerfectMatcher(),
        true,
      )
  }
}
