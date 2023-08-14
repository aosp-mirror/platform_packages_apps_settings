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
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintStateViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Represents all of the fingerprint information needed for fingerprint enrollment. */
class FingerprintViewModel(fingerprintManagerInteractor: FingerprintManagerInteractor) :
  ViewModel() {

  private val _fingerprintViewModel: MutableStateFlow<FingerprintStateViewModel?> =
    MutableStateFlow(null)

  /**
   * A flow that contains a [FingerprintStateViewModel] which contains the relevant information for
   * enrollment
   */
  val fingerprintStateViewModel: Flow<FingerprintStateViewModel?> =
    _fingerprintViewModel.asStateFlow()

  init {
    viewModelScope.launch {
      val enrolledFingerprints =
        fingerprintManagerInteractor.enrolledFingerprints.last().map {
          com.android.settings.biometrics.fingerprint2.shared.model.FingerprintViewModel(
            it.name,
            it.fingerId,
            it.deviceId
          )
        }
      val sensorProps = fingerprintManagerInteractor.sensorPropertiesInternal().first()
      val maxFingerprints = 5
      _fingerprintViewModel.update {
        FingerprintStateViewModel(
          enrolledFingerprints,
          enrolledFingerprints.size < maxFingerprints,
          maxFingerprints,
          sensorProps.isAnySidefpsType,
          false,
          sensorProps,
        )
      }
    }
  }

  class FingerprintViewModelFactory(val interactor: FingerprintManagerInteractor) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {

      return FingerprintViewModel(interactor) as T
    }
  }
}
