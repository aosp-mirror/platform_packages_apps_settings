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
import com.android.settings.biometrics.fingerprint2.shared.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.shared.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.shared.model.FingerEnrollState
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest

/** Represents all of the fingerprint information needed for a fingerprint enrollment process. */
class FingerprintEnrollViewModel(
  private val fingerprintManagerInteractor: FingerprintManagerInteractor,
  gatekeeperViewModel: FingerprintGatekeeperViewModel,
  navigationViewModel: FingerprintEnrollNavigationViewModel,
) : ViewModel() {

  /**
   * Cached value of [FingerprintSensorType]
   *
   * This is typically used by fragments that change their layout/behavior based on this
   * information. This value should be set before any fragment is created.
   */
  var sensorTypeCached: FingerprintSensorType? = null
  private var _enrollReason: Flow<EnrollReason?> =
    navigationViewModel.navigationViewModel.map {
      when (it.currStep) {
        is Enrollment -> EnrollReason.EnrollEnrolling
        is Education -> EnrollReason.FindSensor
        else -> null
      }
    }

  /** Represents the stream of [FingerprintSensorType] */
  val sensorType: Flow<FingerprintSensorType> =
    fingerprintManagerInteractor.sensorPropertiesInternal.filterNotNull().map { it.sensorType }

  /**
   * A flow that contains a [FingerprintEnrollViewModel] which contains the relevant information for
   * an enrollment process
   *
   * This flow should be the only flow which calls enroll().
   */
  val _enrollFlow: Flow<FingerEnrollState> =
    combine(gatekeeperViewModel.gatekeeperInfo, _enrollReason) { hardwareAuthToken, enrollReason,
        ->
        Pair(hardwareAuthToken, enrollReason)
      }
      .transformLatest {
        /** [transformLatest] is used as we want to make sure to cancel previous API call. */
        (hardwareAuthToken, enrollReason) ->
        if (hardwareAuthToken is GatekeeperInfo.GatekeeperPasswordInfo && enrollReason != null) {
          fingerprintManagerInteractor.enroll(hardwareAuthToken.token, enrollReason).collect {
            emit(it)
          }
        }
      }
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

  /**
   * This flow will kick off education when
   * 1) There is an active subscriber to this flow
   * 2) shouldEnroll is true and we are on the FindSensor step
   */
  val educationEnrollFlow: Flow<FingerEnrollState?> =
    _enrollReason.filterNotNull().transformLatest { enrollReason ->
      if (enrollReason == EnrollReason.FindSensor) {
        _enrollFlow.collect { event -> emit(event) }
      } else {
        emit(null)
      }
    }

  /**
   * This flow will kick off enrollment when
   * 1) There is an active subscriber to this flow
   * 2) shouldEnroll is true and we are on the EnrollEnrolling step
   */
  val enrollFlow: Flow<FingerEnrollState?> =
    _enrollReason.filterNotNull().transformLatest { enrollReason ->
      if (enrollReason == EnrollReason.EnrollEnrolling) {
        _enrollFlow.collect { event -> emit(event) }
      } else {
        emit(null)
      }
    }

  class FingerprintEnrollViewModelFactory(
    val interactor: FingerprintManagerInteractor,
    val gatekeeperViewModel: FingerprintGatekeeperViewModel,
    val navigationViewModel: FingerprintEnrollNavigationViewModel,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      modelClass: Class<T>,
    ): T {
      return FingerprintEnrollViewModel(
        interactor,
        gatekeeperViewModel,
        navigationViewModel,
      )
        as T
    }
  }
}
