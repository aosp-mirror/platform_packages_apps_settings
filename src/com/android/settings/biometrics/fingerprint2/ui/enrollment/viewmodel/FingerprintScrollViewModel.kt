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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** This class is responsible for ensuring a users consent to use FingerprintEnrollment. */
class FingerprintScrollViewModel : ViewModel() {

  private val _hasReadConsentScreen: MutableStateFlow<Boolean> = MutableStateFlow(false)

  /** Indicates if a user has consented to FingerprintEnrollment */
  val hasReadConsentScreen: Flow<Boolean> = _hasReadConsentScreen.asStateFlow()

  /** Indicates that a user has consented to FingerprintEnrollment */
  fun userConsented() {
    _hasReadConsentScreen.update { true }
  }

  class FingerprintScrollViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {
      return FingerprintScrollViewModel() as T
    }
  }
}
