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
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractor
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.toSensorType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update

private const val TAG = "FingerprintEnrollViewModel"

/** Represents all of the fingerprint information needed for a fingerprint enrollment process. */
class FingerprintEnrollViewModel(
  private val fingerprintManagerInteractor: FingerprintManagerInteractor,
  backgroundDispatcher: CoroutineDispatcher,
) : ViewModel() {

  /** Represents the stream of [FingerprintSensorType] */
  val sensorType: Flow<FingerprintSensorType> =
    fingerprintManagerInteractor.sensorPropertiesInternal.filterNotNull().map {
      it.sensorType.toSensorType()
    }

  private var _enrollReason: MutableStateFlow<EnrollReason> =
    MutableStateFlow(EnrollReason.FindSensor)
  private var _hardwareAuthToken: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
  private var _consumerShouldEnroll: MutableStateFlow<Boolean> = MutableStateFlow(false)

  /**
   * A flow that contains a [FingerprintEnrollViewModel] which contains the relevant information for
   * an enrollment process
   */
  val enrollFlow: Flow<FingerEnrollStateViewModel> =
    combine(_consumerShouldEnroll, _hardwareAuthToken, _enrollReason) {
        consumerShouldEnroll,
        hardwareAuthToken,
        enrollReason ->
        Triple(consumerShouldEnroll, hardwareAuthToken, enrollReason)
      }
      .transformLatest {
        // transformLatest() instead of transform() is used here for cancelling previous enroll()
        // whenever |consumerShouldEnroll| is changed. Otherwise the latest value will be suspended
        // since enroll() is an infinite callback flow.
        (consumerShouldEnroll, hardwareAuthToken, enrollReason) ->
        if (consumerShouldEnroll && hardwareAuthToken != null) {
          fingerprintManagerInteractor.enroll(hardwareAuthToken, enrollReason).collect { emit(it) }
        }
      }
      .flowOn(backgroundDispatcher)

  /** Used to indicate the consumer of the view model is ready for an enrollment. */
  fun startEnroll(hardwareAuthToken: ByteArray?, enrollReason: EnrollReason) {
    _enrollReason.update { enrollReason }
    _hardwareAuthToken.update { hardwareAuthToken }
    // Update _consumerShouldEnroll after updating the other values.
    _consumerShouldEnroll.update { true }
  }

  /** Used to indicate to stop the enrollment. */
  fun stopEnroll() {
    _consumerShouldEnroll.update { false }
  }

  class FingerprintEnrollViewModelFactory(
    val interactor: FingerprintManagerInteractor,
    val backgroundDispatcher: CoroutineDispatcher
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {
      return FingerprintEnrollViewModel(interactor, backgroundDispatcher) as T
    }
  }
}
