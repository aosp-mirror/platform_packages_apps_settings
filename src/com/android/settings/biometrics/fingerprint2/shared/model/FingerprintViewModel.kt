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

/** The various types of fingerprint sensors */
sealed class SensorType {
  /** Rear fingerprint sensor */
  data object RFPS : SensorType()

  /** Optical under display sensor */
  data object Optical : SensorType()

  /** Ultrasonic under display sensor */
  data object Ultrasonic : SensorType()

  /** Side fingerprint sensor */
  data object SFPS : SensorType()

  /** Unkonwn fingerprint sensor */
  data object Unknown : SensorType()
}

/** The strength of a given sensor */
sealed class SensorStrength {
  data object Convenient : SensorStrength()
  data object Weak : SensorStrength()
  data object Strong : SensorStrength()
  data object Unknown : SensorStrength()
}

data class FingerprintSensorPropertyViewModel(
  val sensorId: Int,
  val sensorStrength: SensorStrength,
  val maxEnrollmentsPerUser: Int,
  val sensorType: SensorType
)
