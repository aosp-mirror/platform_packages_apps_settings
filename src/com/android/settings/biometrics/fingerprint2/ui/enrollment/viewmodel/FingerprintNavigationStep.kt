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

import com.android.settings.biometrics.fingerprint2.lib.model.FastEnroll
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintFlow
import com.android.systemui.biometrics.shared.model.FingerprintSensor

/**
 * A [FingerprintAction] event notifies the current [FingerprintNavigationStep] that an event
 * occurred. Depending on the type of [FingerprintAction] and the current
 * [FingerprintNavigationStep], the navstep will potentially produce a new
 * [FingerprintNavigationStep] indicating either 1). Control flow has changed 2). The activity has
 * finished 3). A transition is required
 */
enum class FingerprintAction {
  NEXT,
  PREV,
  CONFIRM_DEVICE_SUCCESS,
  CONFIRM_DEVICE_FAIL,
  TRANSITION_FINISHED,
  DID_GO_TO_BACKGROUND,
  ACTIVITY_CREATED,
  NEGATIVE_BUTTON_PRESSED,
  USER_CLICKED_FINISH,
  ADD_ANOTHER,
}

/** State that can be used to help a [FingerprintNavigationStep] determine the next step to take. */
data class NavigationState(
  val flowType: FingerprintFlow,
  val hasConfirmedDeviceCredential: Boolean,
  val fingerprintSensor: FingerprintSensor?,
)

/**
 * A generic interface for operating on (state, action) -> state? which will produce either another
 * FingerprintNavStep if something is required, or nothing.
 *
 * Note during the lifetime of the Activity, their should only be one [FingerprintNavigationStep] at
 * a time.
 */
sealed interface FingerprintNavigationStep {
  fun update(state: NavigationState, action: FingerprintAction): FingerprintNavigationStep?

  /**
   * This indicates that a transition should occur from one screen to another. This class should
   * contain all necessary info about the transition.
   *
   * A transition step will cause a screen to change ownership from the current screen to the
   * [nextUiStep], after the transition has been completed and a
   * [FingerprintAction.TRANSITION_FINISHED] has been sent, the [nextUiStep] will be given control.
   */
  class TransitionStep(val nextUiStep: UiStep) : FingerprintNavigationStep {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? {
      return when (action) {
        FingerprintAction.TRANSITION_FINISHED -> nextUiStep
        else -> null
      }
    }

    override fun toString(): String = "TransitionStep(nextUiStep=$nextUiStep)"
  }

  /** Indicates we should finish the enrolling activity */
  data class Finish(val result: Int?) : FingerprintNavigationStep {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? = null
  }

  /** UiSteps should have a 1 to 1 mapping between each screen of FingerprintEnrollment */
  sealed class UiStep : FingerprintNavigationStep

  /** This is the landing page for enrollment, where no content is shown. */
  data object Init : UiStep() {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? {
      return when (action) {
        FingerprintAction.ACTIVITY_CREATED -> {
          if (!state.hasConfirmedDeviceCredential) {
            TransitionStep(ConfirmDeviceCredential)
          } else if (state.flowType is FastEnroll) {
            TransitionStep(Enrollment(state.fingerprintSensor!!))
          } else {
            TransitionStep(Introduction)
          }
        }
        else -> null
      }
    }
  }

  /** Indicates the ConfirmDeviceCredential activity is being presented to the user */
  data object ConfirmDeviceCredential : UiStep() {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? {
      return when (action) {
        FingerprintAction.CONFIRM_DEVICE_SUCCESS -> TransitionStep(Introduction)
        FingerprintAction.CONFIRM_DEVICE_FAIL -> Finish(null)
        else -> null
      }
    }
  }

  /** Indicates the FingerprintIntroduction screen is being presented to the user */
  data object Introduction : UiStep() {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? {
      return when (action) {
        FingerprintAction.NEXT -> TransitionStep(Education(state.fingerprintSensor!!))
        FingerprintAction.NEGATIVE_BUTTON_PRESSED,
        FingerprintAction.PREV -> Finish(null)
        else -> null
      }
    }
  }

  /** Indicates the FingerprintEducation screen is being presented to the user */
  data class Education(val sensor: FingerprintSensor) : UiStep() {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? {
      return when (action) {
        FingerprintAction.NEXT -> TransitionStep(Enrollment(state.fingerprintSensor!!))
        FingerprintAction.NEGATIVE_BUTTON_PRESSED,
        FingerprintAction.PREV -> TransitionStep(Introduction)
        else -> null
      }
    }
  }

  /** Indicates the Enrollment screen is being presented to the user */
  data class Enrollment(val sensor: FingerprintSensor) : UiStep() {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? {
      return when (action) {
        FingerprintAction.NEXT -> TransitionStep(Confirmation)
        FingerprintAction.NEGATIVE_BUTTON_PRESSED,
        FingerprintAction.USER_CLICKED_FINISH,
        FingerprintAction.DID_GO_TO_BACKGROUND -> Finish(null)
        else -> null
      }
    }
  }

  /** Indicates the Confirmation screen is being presented to the user */
  data object Confirmation : UiStep() {
    override fun update(
      state: NavigationState,
      action: FingerprintAction,
    ): FingerprintNavigationStep? {
      return when (action) {
        FingerprintAction.NEXT -> Finish(null)
        FingerprintAction.PREV -> TransitionStep(Education(state.fingerprintSensor!!))
        FingerprintAction.ADD_ANOTHER -> TransitionStep(Enrollment(state.fingerprintSensor!!))
        else -> null
      }
    }
  }
}
