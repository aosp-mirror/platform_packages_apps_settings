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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintFlow
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintNavigationStep.Introduction
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import kotlinx.coroutines.flow.Flow

/** A view model for fingerprint enroll introduction. */
class FingerprintEnrollIntroViewModel(
  val navigationViewModel: FingerprintNavigationViewModel,
  private val fingerprintFlowViewModel: FingerprintFlowViewModel,
  private val fingerprintManagerInteractor: FingerprintManagerInteractor,
) : ViewModel() {

  /** Represents a stream of [FingerprintSensor] */
  val sensor: Flow<FingerprintSensor?> = fingerprintManagerInteractor.sensorPropertiesInternal

  /** Represents a stream of [FingerprintFlow] */
  val fingerprintFlow: Flow<FingerprintFlow?> = fingerprintFlowViewModel.fingerprintFlow

  /** Indicates the primary button has been clicked */
  fun primaryButtonClicked() {
    navigationViewModel.update(FingerprintAction.NEXT, navStep, "${TAG}#onNextClicked")
  }

  /** Indicates the secondary button has been clicked */
  fun onSecondaryButtonClicked() {
    navigationViewModel.update(
      FingerprintAction.NEGATIVE_BUTTON_PRESSED,
      navStep,
      "${TAG}#negativeButtonClicked",
    )
  }

  class FingerprintEnrollIntoViewModelFactory(
    val navigationViewModel: FingerprintNavigationViewModel,
    val fingerprintFlowViewModel: FingerprintFlowViewModel,
    val fingerprintManagerInteractor: FingerprintManagerInteractor,
  ) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return FingerprintEnrollIntroViewModel(
        navigationViewModel,
        fingerprintFlowViewModel,
        fingerprintManagerInteractor,
      )
        as T
    }
  }

  companion object {
    val navStep = Introduction::class
    private const val TAG = "FingerprintEnrollIntroViewModel"
  }
}
