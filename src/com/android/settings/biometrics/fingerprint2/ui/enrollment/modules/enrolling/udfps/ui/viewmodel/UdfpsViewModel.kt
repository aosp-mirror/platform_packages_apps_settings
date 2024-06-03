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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel

import android.graphics.Point
import android.graphics.PointF
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.data.model.EnrollStageModel
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepository
import com.android.settings.biometrics.fingerprint2.data.repository.SimulatedTouchEventsRepository
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.DebuggingInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.DisplayDensityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrollStageInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintVibrationEffects
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.UdfpsEnrollInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.VibrationInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.model.DescriptionText
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.model.HeaderText
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.BackgroundViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintAction
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollEnrollingViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel used to drive UDFPS Enrollment through [UdfpsEnrollFragment] */
class UdfpsViewModel(
  val vibrationInteractor: VibrationInteractor,
  displayDensityInteractor: DisplayDensityInteractor,
  val navigationViewModel: FingerprintNavigationViewModel,
  debuggingInteractor: DebuggingInteractor,
  val fingerprintEnrollEnrollingViewModel: FingerprintEnrollEnrollingViewModel,
  simulatedTouchEventsDebugRepository: SimulatedTouchEventsRepository,
  enrollStageInteractor: EnrollStageInteractor,
  orientationInteractor: OrientationInteractor,
  backgroundViewModel: BackgroundViewModel,
  sensorRepository: FingerprintSensorRepository,
  udfpsEnrollInteractor: UdfpsEnrollInteractor,
  fingerprintManager: FingerprintManagerInteractor,
  val udfpsLastStepViewModel: UdfpsLastStepViewModel,
  accessibilityInteractor: AccessibilityInteractor,
) : ViewModel() {

  private val isSetupWizard = flowOf(false)
  private var shouldResetErollment = false

  private var _enrollState: Flow<FingerEnrollState?> =
    fingerprintManager.sensorPropertiesInternal.filterNotNull().combine(
      fingerprintEnrollEnrollingViewModel.enrollFlow
    ) { props, enroll ->
      if (props.sensorType.isUdfps()) {
        enroll
      } else {
        null
      }
    }

  /** The current state of the enrollment. */
  var enrollState: Flow<FingerEnrollState> =
    combine(fingerprintEnrollEnrollingViewModel.enrollFlowShouldBeRunning, _enrollState) {
        shouldBeRunning,
        state ->
        if (shouldBeRunning) {
          state
        } else {
          null
        }
      }
      .filterNotNull()

  /** Indicates that overlay has been shown */
  val overlayShown =
    enrollState
      .filterIsInstance<FingerEnrollState.OverlayShown>()
      .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

  private var _userInteractedWithSensor = MutableStateFlow(false)

  /**
   * This indicates whether the user has interacted with the sensor or not. This indicates if we are
   * in the initial state of the UI.
   */
  val userInteractedWithSensor: Flow<Boolean> =
    enrollState.transform {
      val interactiveMessage =
        when (it) {
          is FingerEnrollState.Acquired,
          is FingerEnrollState.EnrollError,
          is FingerEnrollState.EnrollHelp,
          is FingerEnrollState.EnrollProgress,
          is FingerEnrollState.PointerDown,
          is FingerEnrollState.PointerUp -> true
          else -> false
        }
      val hasPreviouslyInteracted = _userInteractedWithSensor.value or interactiveMessage
      _userInteractedWithSensor.update { hasPreviouslyInteracted }
      emit(hasPreviouslyInteracted)
    }

  /**
   * Forwards the property sensor information. This is typically used to recreate views that must be
   * aligned with the sensor.
   */
  val sensorLocation = sensorRepository.fingerprintSensor

  /** Indicates a step of guided enrollment, the ui should animate the icon to the new location. */
  val guidedEnrollment: Flow<PointF> =
    udfpsEnrollInteractor.guidedEnrollmentOffset
      .distinctUntilChanged()
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

  private var _lastOrientation: Int? = null

  /** In case of rotations we should ensure the UI does not re-animate the last state. */
  private val shouldReplayLastEvent =
    orientationInteractor.rotation.transform {
      if (_lastOrientation != null && _lastOrientation!! != it) {
        emit(true)
      } else {
        emit(false)
      }
      _lastOrientation = it
    }

  /**
   * This is the saved progress, this is for when views are recreated and need saved state for the
   * first time.
   */
  var progressSaved: Flow<FingerEnrollState.EnrollProgress> =
    enrollState
      .filterIsInstance<FingerEnrollState.EnrollProgress>()
      .combineTransform(shouldReplayLastEvent) { enroll, shouldReplay ->
        if (shouldReplay) {
          emit(enroll)
        }
      }
      .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

  /** Indicates if accessibility is enabled */
  val accessibilityEnabled =
    accessibilityInteractor.isAccessibilityEnabled.shareIn(
      this.viewModelScope,
      SharingStarted.Eagerly,
      replay = 1,
    )

  /** Indicates if we are in reverse landscape orientation. */
  val isReverseLandscape =
    orientationInteractor.rotation
      .transform { emit(it == Surface.ROTATION_270) }
      .distinctUntilChanged()

  /** Indicates if we are in the landscape orientation */
  val isLandscape =
    orientationInteractor.rotation
      .transform { emit(it == Surface.ROTATION_90) }
      .distinctUntilChanged()

  /** This sends touch exploration events only used for debugging purposes. */
  val touchExplorationDebug: Flow<Point> =
    debuggingInteractor.debuggingEnabled.combineTransform(
      simulatedTouchEventsDebugRepository.touchExplorationDebug
    ) { enabled, point ->
      if (enabled) {
        emit(point)
      }
    }

  /** Determines the current [EnrollStageModel] enrollment is in */
  private val enrollStage: Flow<EnrollStageModel> =
    combine(enrollStageInteractor.enrollStageThresholds, enrollState) { thresholds, event ->
        if (event is FingerEnrollState.EnrollProgress) {
          val progress =
            (event.totalStepsRequired - event.remainingSteps).toFloat() / event.totalStepsRequired
          var stageToReturn: EnrollStageModel = EnrollStageModel.Center
          thresholds.forEach { (threshold, stage) ->
            if (progress < threshold) {
              return@forEach
            }
            stageToReturn = stage
          }
          stageToReturn
        } else {
          null
        }
      }
      .filterNotNull()
      .shareIn(this.viewModelScope, SharingStarted.Eagerly, replay = 1)

  /** The saved version of [guidedEnrollment] */
  val guidedEnrollmentSaved: Flow<PointF> =
    combineTransform(guidedEnrollment, shouldReplayLastEvent, enrollStage) {
        point,
        shouldReplay,
        stage ->
        if (shouldReplay && stage is EnrollStageModel.Guided) {
          emit(point)
        }
      }
      .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

  init {
    viewModelScope.launch {
      enrollState
        .combine(accessibilityEnabled) { event, isEnabled -> Pair(event, isEnabled) }
        .collect {
          if (
            when (it.first) {
              is FingerEnrollState.EnrollError -> true
              is FingerEnrollState.EnrollHelp -> it.second
              is FingerEnrollState.EnrollProgress -> true
              else -> false
            }
          ) {
            vibrate(it.first)
          }
        }
    }
    viewModelScope.launch {
      enrollStage.collect {
        udfpsEnrollInteractor.updateGuidedEnrollment(it is EnrollStageModel.Guided)
      }
    }

    viewModelScope.launch {
      enrollState.filterIsInstance<FingerEnrollState.EnrollProgress>().collect {
        udfpsEnrollInteractor.onEnrollmentStep(it.remainingSteps, it.totalStepsRequired)
      }
    }

    viewModelScope.launch {
      backgroundViewModel.background.filter { it }.collect { didGoToBackground() }
    }
  }

  /** Indicates if we should show the lottie. */
  val shouldShowLottie: Flow<Boolean> =
    combine(
        displayDensityInteractor.displayDensity,
        displayDensityInteractor.defaultDisplayDensity,
        displayDensityInteractor.fontScale,
      ) { currDisplayDensity, defaultDisplayDensity, fontScale ->
        if (fontScale > 1.0f) {
          false
        } else {
          defaultDisplayDensity == currDisplayDensity
        }
      }
      .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

  /** The header text for UDFPS enrollment */
  val headerText: Flow<HeaderText> =
    combine(isSetupWizard, accessibilityEnabled, enrollStage) { isSuw, isAccessibility, stage ->
        return@combine HeaderText(isSuw, isAccessibility, stage)
      }
      .shareIn(this.viewModelScope, SharingStarted.Eagerly, replay = 1)

  /** Indicates if we should or shold not draw the fingerprint icon */
  val shouldDrawIcon: Flow<Boolean> =
    enrollState.transform { state ->
      when (state) {
        is FingerEnrollState.EnrollProgress,
        is FingerEnrollState.EnrollError,
        is FingerEnrollState.PointerUp -> emit(true)
        is FingerEnrollState.PointerDown -> emit(false)
        else -> {}
      }
    }

  private val shouldClearDescriptionText = enrollStage.map { it is EnrollStageModel.Unknown }

  /** The description text for UDFPS enrollment */
  val descriptionText: Flow<DescriptionText?> =
    combine(isSetupWizard, accessibilityEnabled, enrollStage, shouldClearDescriptionText) {
        isSuw,
        isAccessibility,
        stage,
        shouldClearText ->
        if (shouldClearText) {
          return@combine null
        } else {
          return@combine DescriptionText(isSuw, isAccessibility, stage)
        }
      }
      .shareIn(this.viewModelScope, SharingStarted.Eagerly, replay = 1)

  /** Indicates if the consumer is ready for enrollment */
  fun readyForEnrollment() {
    if (shouldResetErollment) {
      shouldResetErollment = false
      _enrollState = fingerprintEnrollEnrollingViewModel.enrollFlow
    }
    fingerprintEnrollEnrollingViewModel.canEnroll()
  }

  /** Indicates if enrollment should stop */
  fun stopEnrollment() {
    fingerprintEnrollEnrollingViewModel.stopEnroll()
  }

  /** Indicates the negative button has been clicked */
  fun negativeButtonClicked() {
    navigationViewModel.update(
      FingerprintAction.NEGATIVE_BUTTON_PRESSED,
      navStep,
      "$TAG#negativeButtonClicked",
    )
    doReset()
  }

  /** Indicates that an enrollment was completed */
  fun finishedSuccessfully() {
    doReset()
    navigationViewModel.update(FingerprintAction.NEXT, navStep, "${TAG}#progressFinished")
  }

  /** Indicates that the application went to the background. */
  fun didGoToBackground() {
    navigationViewModel.update(
      FingerprintAction.DID_GO_TO_BACKGROUND,
      navStep,
      "$TAG#didGoToBackground",
    )
    stopEnrollment()
  }

  private fun doReset() {
    _enrollState = fingerprintEnrollEnrollingViewModel.enrollFlow
    _userInteractedWithSensor.update { false }
  }

  /** The lottie that should be shown for UDFPS Enrollment */
  val lottie: Flow<EducationAnimationModel> =
    combine(isSetupWizard, accessibilityEnabled, enrollStage) { isSuw, isAccessibility, stage ->
        return@combine EducationAnimationModel(isSuw, isAccessibility, stage)
      }
      .distinctUntilChanged()
      .shareIn(this.viewModelScope, SharingStarted.Eagerly, replay = 1)

  /** Indicates we should send a vibration event */
  private fun vibrate(event: FingerEnrollState) {
    val vibrationEvent =
      when (event) {
        is FingerEnrollState.EnrollError -> FingerprintVibrationEffects.UdfpsError
        is FingerEnrollState.EnrollHelp -> FingerprintVibrationEffects.UdfpsHelp
        is FingerEnrollState.EnrollProgress -> FingerprintVibrationEffects.UdfpsSuccess
        else -> FingerprintVibrationEffects.UdfpsError
      }
    vibrationInteractor.vibrate(vibrationEvent, "UdfpsEnrollFragment")
  }

  /** Indicates an error sent by the HAL has been acknowledged by the user */
  fun errorDialogShown(it: FingerEnrollState.EnrollError) {
    navigationViewModel.update(
      FingerprintAction.USER_CLICKED_FINISH,
      navStep,
      "${TAG}#userClickedStopEnrollingDialog",
    )
  }

  class UdfpsEnrollmentFactory(
    private val vibrationInteractor: VibrationInteractor,
    private val displayDensityInteractor: DisplayDensityInteractor,
    private val navigationViewModel: FingerprintNavigationViewModel,
    private val debuggingInteractor: DebuggingInteractor,
    private val fingerprintEnrollEnrollingViewModel: FingerprintEnrollEnrollingViewModel,
    private val simulatedTouchEventsRepository: SimulatedTouchEventsRepository,
    private val enrollStageInteractor: EnrollStageInteractor,
    private val orientationInteractor: OrientationInteractor,
    private val backgroundViewModel: BackgroundViewModel,
    private val sensorRepository: FingerprintSensorRepository,
    private val udfpsEnrollInteractor: UdfpsEnrollInteractor,
    private val fingerprintManager: FingerprintManagerInteractor,
    private val udfpsLastStepViewModel: UdfpsLastStepViewModel,
    private val accessibilityInteractor: AccessibilityInteractor,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return UdfpsViewModel(
        vibrationInteractor,
        displayDensityInteractor,
        navigationViewModel,
        debuggingInteractor,
        fingerprintEnrollEnrollingViewModel,
        simulatedTouchEventsRepository,
        enrollStageInteractor,
        orientationInteractor,
        backgroundViewModel,
        sensorRepository,
        udfpsEnrollInteractor,
        fingerprintManager,
        udfpsLastStepViewModel,
        accessibilityInteractor,
      )
        as T
    }
  }

  companion object {
    private val navStep = FingerprintNavigationStep.Enrollment::class
    private const val TAG = "UDFPSViewModel"
  }
}
