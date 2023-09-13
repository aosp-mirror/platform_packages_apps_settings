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

import android.hardware.fingerprint.FingerprintSensorProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/** Represents all of the fingerprint information needed for fingerprint enrollment. */
class FingerprintEnrollViewModel(fingerprintManagerInteractor: FingerprintManagerInteractor) :
  ViewModel() {

  /** Represents the stream of [FingerprintSensorProperties.SensorType] */
  val sensorType: Flow<Int> =
    fingerprintManagerInteractor.sensorPropertiesInternal.transform { it?.sensorType }

  class FingerprintEnrollViewModelFactory(val interactor: FingerprintManagerInteractor) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {
      return FingerprintEnrollViewModel(interactor) as T
    }
  }
}
