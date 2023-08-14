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

package com.android.settings.biometrics.fingerprint2.shared.model

import android.hardware.fingerprint.FingerprintSensorPropertiesInternal

/** Represents the fingerprint data nad the relevant state. */
data class FingerprintStateViewModel(
  val fingerprintViewModels: List<FingerprintViewModel>,
  val canEnroll: Boolean,
  val maxFingerprints: Int,
  val hasSideFps: Boolean,
  val pressToAuth: Boolean,
  val sensorProps: FingerprintSensorPropertiesInternal,
)

data class FingerprintViewModel(
  val name: String,
  val fingerId: Int,
  val deviceId: Long,
)

sealed class FingerprintAuthAttemptViewModel {
  data class Success(
    val fingerId: Int,
  ) : FingerprintAuthAttemptViewModel()

  data class Error(
    val error: Int,
    val message: String,
  ) : FingerprintAuthAttemptViewModel()
}
