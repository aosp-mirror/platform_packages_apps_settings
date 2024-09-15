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

import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.settings.SettingsApplication
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintVibrationEffects
import com.android.settings.biometrics.fingerprint2.domain.interactor.VibrationInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollEnrollingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This class is responsible for determining if we should animate the last step of an enrollment. It
 * seems to be due to poor hardware implementation that the last step of a UDFPS enrollment takes
 * much longer then normal (likely due to to saving the whole enrollment/or some kind of
 * verification)
 *
 * Because of this, we will use the information of if a fingerprint was acquired(good) + enrollment
 * has reached the last step
 */
class UdfpsLastStepViewModel(
  private val fingerprintEnrollEnrollingViewModel: FingerprintEnrollEnrollingViewModel,
  private val vibrationInteractor: VibrationInteractor,
) : ViewModel() {

  private val lastStep: MutableStateFlow<FingerEnrollState.EnrollProgress?> = MutableStateFlow(null)
  private val stepSize: MutableStateFlow<Int?> = MutableStateFlow(null)

  init {
    viewModelScope.launch {
      val steps =
        fingerprintEnrollEnrollingViewModel.enrollFlow
          .filterIsInstance<FingerEnrollState.EnrollProgress>()
          .distinctUntilChanged()
          .take(2)
          .toList()
      stepSize.update { steps[0].remainingSteps - steps[1].remainingSteps }
      lastStep.update { FingerEnrollState.EnrollProgress(0, steps[0].totalStepsRequired) }
    }
  }

  private val enrollProgress =
    fingerprintEnrollEnrollingViewModel.enrollFlow.filterIsInstance<
      FingerEnrollState.EnrollProgress
    >()

  /** Indicates if we should animate the completion of udfps enrollment. */
  val shouldAnimateCompletion =
    enrollProgress
      .transform { it ->
        if (it.remainingSteps == stepSize.value) {
          fingerprintEnrollEnrollingViewModel.enrollFlow
            .filterIsInstance<FingerEnrollState.Acquired>()
            .filter { acquired -> acquired.acquiredGood }
            .collect {
              vibrationInteractor.vibrate(
                FingerprintVibrationEffects.UdfpsSuccess,
                "UdfpsLastStepViewModel",
              )

              emit(lastStep.value)
            }
        }
      }
      .filterNotNull()

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val settingsApplication =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SettingsApplication
        val biometricEnvironment = settingsApplication.biometricEnvironment
        val provider = ViewModelProvider(this[VIEW_MODEL_STORE_OWNER_KEY]!!)
        UdfpsLastStepViewModel(
          provider[FingerprintEnrollEnrollingViewModel::class],
          biometricEnvironment!!.vibrationInteractor,
        )
      }
    }
  }
}
