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

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** ViewModel used to drive UDFPS Enrollment through [UdfpsEnrollFragment] */
class UdfpsViewModel() : ViewModel() {

  private val isSetupWizard = flowOf(false)

  /** Indicates which Enrollment stage we are currently in. */
  private val sensorLocationInternal = Pair(540, 1713)
  private val sensorRadius = 100
  private val sensorRect =
    Rect(
      this.sensorLocationInternal.first - sensorRadius,
      this.sensorLocationInternal.second - sensorRadius,
      this.sensorLocationInternal.first + sensorRadius,
      this.sensorLocationInternal.second + sensorRadius,
    )

  private val stageThresholds = flowOf(listOf(.25, .5, .75, .875))

  /** Indicates if accessibility is enabled */
  val accessibilityEnabled = flowOf(false)

  /** Indicates the locates of the fingerprint sensor. */
  val sensorLocation: Flow<Rect> = flowOf(sensorRect)

  /** This is currently not hooked up to fingerprint manager, and is being fed mock events. */
  val udfpsEvent: Flow<UdfpsEnrollEvent> =
    flow {
        enrollEvents.forEach { events ->
          events.forEach { event -> emit(event) }
          delay(1000)
        }
      }
      .flowOn(Dispatchers.IO)

  /** Determines the current [StageViewModel] enrollment is in */
  val enrollStage: Flow<StageViewModel> =
    combine(stageThresholds, udfpsEvent) { thresholds, event ->
        if (event is UdfpsProgress) {
          thresholdToStageMap(thresholds, event.totalSteps - event.remainingSteps, event.totalSteps)
        } else {
          null
        }
      }
      .filterNotNull()

  /** The header text for UDFPS enrollment */
  val headerText: Flow<HeaderText> =
    combine(isSetupWizard, accessibilityEnabled, enrollStage) { isSuw, isAccessibility, stage ->
      return@combine HeaderText(isSuw, isAccessibility, stage)
    }

  private val shouldClearDescriptionText = enrollStage.map { it is StageViewModel.Unknown }

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

  /** The lottie that should be shown for UDFPS Enrollment */
  val lottie: Flow<EducationAnimationModel> =
    combine(isSetupWizard, accessibilityEnabled, enrollStage) { isSuw, isAccessibility, stage ->
      return@combine EducationAnimationModel(isSuw, isAccessibility, stage)
    }.distinctUntilChanged()

  class UdfpsEnrollmentFactory() : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return UdfpsViewModel() as T
    }
  }

  companion object {
    private val navStep = FingerprintNavigationStep.Enrollment::class
    private const val TAG = "UDFPSViewModel"
    private val ENROLLMENT_STAGES_ORDERED =
      listOf(
        StageViewModel.Center,
        StageViewModel.Guided,
        StageViewModel.Fingertip,
        StageViewModel.LeftEdge,
        StageViewModel.RightEdge,
      )

    /**
     * [thresholds] is a list of 4 numbers from [0,1] that separate enrollment into 5 stages. The
     * stage is determined by mapping [thresholds] * [maxSteps] and finding where the [currentStep]
     * is.
     *
     * Each number in the array should be strictly increasing such as [0.2, 0.5, 0.6, 0.8]
     */
    private fun thresholdToStageMap(
      thresholds: List<Double>,
      currentStep: Int,
      maxSteps: Int,
    ): StageViewModel {
      val stageIterator = ENROLLMENT_STAGES_ORDERED.iterator()
      thresholds.forEach {
        val thresholdLimit = it * maxSteps
        val curr = stageIterator.next()
        if (currentStep < thresholdLimit) {
          return curr
        }
      }
      return stageIterator.next()
    }

    /** This will be removed */
    private val enrollEvents: List<List<UdfpsEnrollEvent>> =
      listOf(
        listOf(OverlayShown),
        CreateProgress(10, 10),
        CreateProgress(9, 10),
        CreateProgress(8, 10),
        CreateProgress(7, 10),
        CreateProgress(6, 10),
        CreateProgress(5, 10),
        CreateProgress(4, 10),
        CreateProgress(3, 10),
        CreateProgress(2, 10),
        CreateProgress(1, 10),
        CreateProgress(0, 10),
      )

    /** This will be removed */
    private fun CreateProgress(remaining: Int, total: Int): List<UdfpsEnrollEvent> {
      return listOf(PointerDown, Acquired(true), UdfpsProgress(remaining, total), PointerUp)
    }
  }
}
