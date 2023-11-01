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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.shared.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "FingerprintEnrollNavigationViewModel"

/**
 * This class is responsible for sending a [NavigationStep] which indicates where the user is in the
 * Fingerprint Enrollment flow
 */
class FingerprintEnrollNavigationViewModel(
  private val dispatcher: CoroutineDispatcher,
  private val fingerprintManagerInteractor: FingerprintManagerInteractor,
  private val gatekeeperViewModel: FingerprintGatekeeperViewModel,
  private val firstStep: NextStepViewModel,
  private val navState: NavState,
  private val theFingerprintFlow: FingerprintFlow,
) : ViewModel() {

  private class InternalNavigationStep(
    lastStep: NextStepViewModel,
    nextStep: NextStepViewModel,
    forward: Boolean,
    var canNavigate: Boolean,
  ) : NavigationStep(lastStep, nextStep, forward)

  private var _fingerprintFlow = MutableStateFlow<FingerprintFlow?>(theFingerprintFlow)

  /** A flow that indicates the [FingerprintFlow] */
  val fingerprintFlow: Flow<FingerprintFlow?> = _fingerprintFlow.asStateFlow()

  private val _navigationStep =
    MutableStateFlow(
      InternalNavigationStep(PlaceHolderState, firstStep, forward = false, canNavigate = true)
    )

  init {
    viewModelScope.launch {
      gatekeeperViewModel.credentialConfirmed.filterNotNull().collect {
        if (_navigationStep.value.currStep is LaunchConfirmDeviceCredential) {
          if (it) nextStep() else finish()
        }
      }
    }
  }

  /**
   * A flow that contains the [NavigationStep] used to indicate where in the enrollment process the
   * user is.
   */
  val navigationViewModel: Flow<NavigationStep> = _navigationStep.asStateFlow()

  /** This action indicates that the UI should actually update the navigation to the given step. */
  val navigationAction: Flow<NavigationStep?> =
    _navigationStep.shareIn(viewModelScope, SharingStarted.Lazily, 0)

  /** Used to start the next step of Fingerprint Enrollment. */
  fun nextStep() {
    viewModelScope.launch {
      val currStep = _navigationStep.value.currStep
      val nextStep = currStep.next(navState)
      Log.d(TAG, "nextStep(${currStep} -> $nextStep)")
      _navigationStep.update {
        InternalNavigationStep(currStep, nextStep, forward = true, canNavigate = false)
      }
    }
  }

  /** Go back a step of fingerprint enrollment. */
  fun prevStep() {
    viewModelScope.launch {
      val currStep = _navigationStep.value.currStep
      val nextStep = currStep.prev(navState)
      _navigationStep.update {
        InternalNavigationStep(currStep, nextStep, forward = false, canNavigate = false)
      }
    }
  }

  private fun finish() {
    _navigationStep.update {
      InternalNavigationStep(Finish(null), Finish(null), forward = false, canNavigate = false)
    }
  }

  class FingerprintEnrollNavigationViewModelFactory(
    private val backgroundDispatcher: CoroutineDispatcher,
    private val fingerprintManagerInteractor: FingerprintManagerInteractor,
    private val fingerprintGatekeeperViewModel: FingerprintGatekeeperViewModel,
    private val canSkipConfirm: Boolean,
    private val fingerprintFlow: FingerprintFlow,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {

      val navState = NavState(canSkipConfirm)
      return FingerprintEnrollNavigationViewModel(
        backgroundDispatcher,
        fingerprintManagerInteractor,
        fingerprintGatekeeperViewModel,
        Start.next(navState),
        navState,
        fingerprintFlow,
      )
        as T
    }
  }
}
