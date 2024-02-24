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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.FingerprintManagerInteractor
import kotlinx.coroutines.flow.Flow

/**
 * Models the UI state for [FingerprintEnrollConfirmationV2Fragment]
 */
class FingerprintEnrollConfirmationViewModel(
  private val navigationViewModel: FingerprintNavigationViewModel,
  fingerprintInteractor: FingerprintManagerInteractor,
) : ViewModel() {

  /**
   * Indicates if the add another button is possible. This should only be true when the user is able
   * to enroll more fingerprints.
   */
  val isAddAnotherButtonVisible: Flow<Boolean> = fingerprintInteractor.canEnrollFingerprints

  /**
   * Indicates that the user has clicked the next button and is done with fingerprint enrollment.
   */
  fun onNextButtonClicked() {
    navigationViewModel.update(FingerprintAction.NEXT, navStep, "onNextButtonClicked")
  }

  /**
   * Indicates that the user has clicked the add another button and will be sent to the enrollment
   * screen.
   */
  fun onAddAnotherButtonClicked() {
    navigationViewModel.update(FingerprintAction.ADD_ANOTHER, navStep, "onAddAnotherButtonClicked")
  }

  class FingerprintEnrollConfirmationViewModelFactory(
    private val navigationViewModel: FingerprintNavigationViewModel,
    private val fingerprintInteractor: FingerprintManagerInteractor,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return FingerprintEnrollConfirmationViewModel(navigationViewModel, fingerprintInteractor) as T
    }
  }

  companion object {
    private const val TAG = "FingerprintEnrollConfirmationViewModel"
    private val navStep = FingerprintNavigationStep.Confirmation::class
  }
}
