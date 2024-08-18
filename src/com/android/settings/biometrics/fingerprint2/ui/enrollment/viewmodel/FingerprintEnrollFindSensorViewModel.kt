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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel

import android.hardware.fingerprint.FingerprintEnrollOptions
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.settings.SettingsApplication
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.SensorInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.SetupWizard
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Education
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Models the UI state for fingerprint enroll education */
class FingerprintEnrollFindSensorViewModel(
    private val navigationViewModel: FingerprintNavigationViewModel,
    private val fingerprintEnrollViewModel: FingerprintEnrollViewModel,
    private val gatekeeperViewModel: FingerprintGatekeeperViewModel,
    backgroundViewModel: BackgroundViewModel,
    fingerprintFlowViewModel: FingerprintFlowViewModel,
    accessibilityInteractor: AccessibilityInteractor,
    foldStateInteractor: FoldStateInteractor,
    orientationInteractor: OrientationInteractor,
    sensorInteractor: SensorInteractor,
) : ViewModel() {

  /** Represents the stream of sensor type. */
  val sensorType: Flow<FingerprintSensorType> =
    sensorInteractor.sensorPropertiesInternal.filterNotNull().map { it.sensorType }
  private val _isUdfps: Flow<Boolean> =
    sensorType.map {
      it == FingerprintSensorType.UDFPS_OPTICAL || it == FingerprintSensorType.UDFPS_ULTRASONIC
    }
  private val _isSfps: Flow<Boolean> = sensorType.map { it == FingerprintSensorType.POWER_BUTTON }
  private val _isRearSfps: Flow<Boolean> = sensorType.map { it == FingerprintSensorType.REAR }

  /** Represents the stream of showing primary button. */
  val showPrimaryButton: Flow<Boolean> = _isUdfps.filter { it }

  private val _showSfpsLottie = _isSfps.filter { it }

  /** Represents the stream of showing sfps lottie and the information Pair(isFolded, rotation). */
  val sfpsLottieInfo: Flow<Pair<Boolean, Int>> =
    combineTransform(
      _showSfpsLottie,
      foldStateInteractor.isFolded,
      orientationInteractor.rotationFromDefault,
    ) { _, isFolded, rotation ->
      emit(Pair(isFolded, rotation))
    }

  private val _showUdfpsLottie = _isUdfps.filter { it }

  /** Represents the stream of showing udfps lottie and whether accessibility is enabled. */
  val udfpsLottieInfo: Flow<Boolean> =
    _showUdfpsLottie.combine(accessibilityInteractor.isAccessibilityEnabled) {
      _,
      isAccessibilityEnabled ->
      isAccessibilityEnabled
    }

  /** Represents the stream of showing rfps animation. */
  val showRfpsAnimation: Flow<Boolean> = _isRearSfps.filter { it }

  private val _showErrorDialog: MutableStateFlow<Pair<Int, Boolean>?> = MutableStateFlow(null)

  /** Represents the stream of showing error dialog. */
  val showErrorDialog = _showErrorDialog.filterNotNull()

  private var _didTryEducation = false
  private var _education: MutableStateFlow<Boolean> = MutableStateFlow(false)

  /** Indicates if the education flow should be running. */
  private val educationFlowShouldBeRunning: Flow<Boolean> =
    _education.combine(backgroundViewModel.background) { shouldRunEducation, isInBackground ->
      !isInBackground && shouldRunEducation
    }

  init {
    // Start or end enroll flow
    viewModelScope.launch {
      combine(
          sensorType,
          gatekeeperViewModel.hasValidGatekeeperInfo,
          gatekeeperViewModel.gatekeeperInfo,
          navigationViewModel.currentScreen,
        ) { sensorType, hasValidGatekeeperInfo, gatekeeperInfo, currStep ->
          val shouldStartEnroll =
            currStep is Education &&
              sensorType != FingerprintSensorType.UDFPS_OPTICAL &&
              sensorType != FingerprintSensorType.UDFPS_ULTRASONIC &&
              hasValidGatekeeperInfo
          if (shouldStartEnroll) (gatekeeperInfo as GatekeeperInfo.GatekeeperPasswordInfo).token
          else null
        }
        .collect { token ->
          if (token != null) {
            canStartEducation()
          } else {
            stopEducation()
          }
        }
    }

    // Enroll progress flow
    viewModelScope.launch {
      educationFlowShouldBeRunning.collect {
        // Only collect the flow when we should be running.
        if (it) {
          combine(
              fingerprintEnrollViewModel.educationEnrollFlow.filterNotNull(),
              fingerprintFlowViewModel.fingerprintFlow,
            ) { educationFlow, type ->
              Pair(educationFlow, type)
            }
            .collect { (educationFlow, type) ->
              when (educationFlow) {
                is FingerEnrollState.EnrollProgress -> proceedToEnrolling()
                is FingerEnrollState.EnrollError -> {
                  if (educationFlow.isCancelled) {
                    proceedToEnrolling()
                  } else {
                    _showErrorDialog.update { Pair(educationFlow.errString, type == SetupWizard) }
                  }
                }
                is FingerEnrollState.EnrollHelp -> {}
                else -> {}
              }
            }
        }
      }
    }
  }

  /** Indicates if education can begin */
  private fun canStartEducation() {
    if (!_didTryEducation) {
      _didTryEducation = true
      _education.update { true }
    }
  }

  /** Indicates that education has finished */
  private fun stopEducation() {
    _education.update { false }
  }

  /** Indicates enrollment to start */
  fun enroll(enrollOptions: FingerprintEnrollOptions) {
    fingerprintEnrollViewModel.enroll(enrollOptions)
  }

  /** Proceed to EnrollEnrolling page. */
  fun proceedToEnrolling() {
    stopEducation()
    navigationViewModel.update(FingerprintAction.NEXT, navStep, "$TAG#proceedToEnrolling")
  }

  /** Indicates the secondary button has been clicked */
  fun secondaryButtonClicked() {
    navigationViewModel.update(
      FingerprintAction.NEGATIVE_BUTTON_PRESSED,
      navStep,
      "${TAG}#secondaryButtonClicked",
    )
  }

  companion object {
    private const val TAG = "FingerprintEnrollFindSensorViewModel"
    private val navStep = Education::class

    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val settingsApplication =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SettingsApplication
        val biometricEnvironment = settingsApplication.biometricEnvironment!!
        val provider = ViewModelProvider(this[VIEW_MODEL_STORE_OWNER_KEY]!!)

        FingerprintEnrollFindSensorViewModel(
          provider[FingerprintNavigationViewModel::class],
          provider[FingerprintEnrollViewModel::class],
          provider[FingerprintGatekeeperViewModel::class],
          provider[BackgroundViewModel::class],
          provider[FingerprintFlowViewModel::class],
          biometricEnvironment.accessibilityInteractor,
          biometricEnvironment.foldStateInteractor,
          biometricEnvironment.orientationInteractor,
          biometricEnvironment.createSensorPropertiesInteractor(),
        )
      }
    }
  }
}
