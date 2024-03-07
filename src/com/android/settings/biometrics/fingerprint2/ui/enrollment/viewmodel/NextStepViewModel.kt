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

/**
 * A class that represents an action that the consumer should transition between lastStep and
 * currStep and in what direction this transition is occurring (e.g. forward or backwards)
 */
open class NavigationStep(
  val lastStep: NextStepViewModel,
  val currStep: NextStepViewModel,
  val forward: Boolean
) {
  override fun toString(): String {
    return "lastStep=$lastStep, currStep=$currStep, forward=$forward"
  }
}

/** The navigation state used by a [NavStep] to determine what the [NextStepViewModel] should be. */
class NavState(val confirmedDevice: Boolean)

interface NavStep<T> {
  fun next(state: NavState): T
  fun prev(state: NavState): T
}

/**
 * A class to represent a high level step (I.E. EnrollmentIntroduction) for FingerprintEnrollment.
 */
sealed class NextStepViewModel : NavStep<NextStepViewModel>

/**
 * This is the initial state for the previous step, used to indicate that there have been no
 * previous states.
 */
object PlaceHolderState : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel = Finish(null)

  override fun prev(state: NavState): NextStepViewModel = Finish(null)
}

/**
 * This state is the initial state for the current step, and will be used to determine if the user
 * needs to [LaunchConfirmDeviceCredential] if not, it will go to [Intro]
 */
data object Start : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel =
    if (state.confirmedDevice) Intro else LaunchConfirmDeviceCredential

  override fun prev(state: NavState): NextStepViewModel = Finish(null)
}

/** State indicating enrollment has been completed */
class Finish(val resultCode: Int?) : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel = Finish(resultCode)
  override fun prev(state: NavState): NextStepViewModel = Finish(null)
}

/** State for the FingerprintEnrollment introduction */
data object Intro : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel = Education
  override fun prev(state: NavState): NextStepViewModel = Finish(null)
}

/** State for the FingerprintEnrollment education */
data object Education : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel = Enrollment
  override fun prev(state: NavState): NextStepViewModel = Intro
}

/** State for the FingerprintEnrollment enrollment */
data object Enrollment : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel = Confirmation
  override fun prev(state: NavState): NextStepViewModel = Education
}

/** State for the FingerprintEnrollment confirmation */
object Confirmation : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel = Finish(0)
  override fun prev(state: NavState): NextStepViewModel = Intro
}

/**
 * State used to send the user to the ConfirmDeviceCredential activity. This activity can either
 * confirm a users device credential, or have them create one.
 */
object LaunchConfirmDeviceCredential : NextStepViewModel() {
  override fun next(state: NavState): NextStepViewModel = Intro
  override fun prev(state: NavState): NextStepViewModel = Finish(0)
}
