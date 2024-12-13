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

import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.settings.SettingsApplication
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import kotlinx.coroutines.flow.Flow

/** Models the UI state for [FingerprintEnrollConfirmationV2Fragment] */
class FingerprintEnrollConfirmationViewModel(
  private val navigationViewModel: FingerprintNavigationViewModel,
  private val canEnrollFingerprintsInteractor: CanEnrollFingerprintsInteractor,
) : ViewModel() {

  /**
   * Indicates if the add another button is possible. This should only be true when the user is able
   * to enroll more fingerprints.
   */
  val isAddAnotherButtonVisible: Flow<Boolean> =
    canEnrollFingerprintsInteractor.canEnrollFingerprints

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

  companion object {
    private const val TAG = "FingerprintEnrollConfirmationViewModel"
    private val navStep = FingerprintNavigationStep.Confirmation::class
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val settingsApplication =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SettingsApplication
        val biometricEnvironment = settingsApplication.biometricEnvironment
        val provider = ViewModelProvider(this[VIEW_MODEL_STORE_OWNER_KEY]!!)
        FingerprintEnrollConfirmationViewModel(
          provider[FingerprintNavigationViewModel::class],
          biometricEnvironment!!.createCanEnrollFingerprintsInteractor(),
        )
      }
    }
  }
}
