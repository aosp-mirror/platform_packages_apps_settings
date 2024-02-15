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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update

/**
 * This class is a wrapper around the [FingerprintEnrollViewModel] and decides when the user should
 * or should not be enrolling.
 */
class FingerprintEnrollEnrollingViewModel(
  private val fingerprintEnrollViewModel: FingerprintEnrollViewModel,
  backgroundViewModel: BackgroundViewModel,
) : ViewModel() {

  private val _didTryEnrollment = MutableStateFlow(false)
  private val _userDidEnroll = MutableStateFlow(false)
  /** Indicates if the enrollment flow should be running. */
  val enrollFlowShouldBeRunning: Flow<Boolean> =
    _userDidEnroll.combine(backgroundViewModel.background) { shouldEnroll, isInBackground ->
      if (isInBackground) {
        false
      } else {
        shouldEnroll
      }
    }

  /**
   * Used to indicate the consumer of the view model is ready for an enrollment. Note that this does
   * not necessarily try an enrollment.
   */
  fun canEnroll() {
    // Update _consumerShouldEnroll after updating the other values.
    if (!_didTryEnrollment.value) {
      _didTryEnrollment.update { true }
      _userDidEnroll.update { true }
    }
  }

  /** Used to indicate to stop the enrollment. */
  fun stopEnroll() {
    _userDidEnroll.update { false }
  }

  /** Collects the enrollment flow based on [enrollFlowShouldBeRunning] */
  val enrollFLow =
    enrollFlowShouldBeRunning.transformLatest {
      if (it) {
        fingerprintEnrollViewModel.enrollFlow.collect { event -> emit(event) }
      }
    }

  class FingerprintEnrollEnrollingViewModelFactory(
    private val fingerprintEnrollViewModel: FingerprintEnrollViewModel,
    private val backgroundViewModel: BackgroundViewModel,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return FingerprintEnrollEnrollingViewModel(fingerprintEnrollViewModel, backgroundViewModel)
        as T
    }
  }
}
