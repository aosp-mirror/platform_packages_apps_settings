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

package com.android.settings.biometrics.fingerprint2.conversion

import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import com.android.settings.biometrics.fingerprint2.shared.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintSensorPropertyViewModel
import com.android.settings.biometrics.fingerprint2.shared.model.SensorStrength
import com.android.settings.biometrics.fingerprint2.shared.model.SensorType

class Util {
  companion object {
    fun sensorPropsToViewModel(
      props: FingerprintSensorPropertiesInternal
    ): FingerprintSensorPropertyViewModel {
      val sensorStrength: SensorStrength =
        when (props.sensorStrength) {
          FingerprintSensorProperties.STRENGTH_CONVENIENCE -> SensorStrength.Convenient
          FingerprintSensorProperties.STRENGTH_WEAK -> SensorStrength.Weak
          FingerprintSensorProperties.STRENGTH_STRONG -> SensorStrength.Strong
          else -> SensorStrength.Unknown
        }
      val sensorType: SensorType =
        when (props.sensorType) {
          FingerprintSensorProperties.TYPE_UDFPS_OPTICAL -> SensorType.Optical
          FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC -> SensorType.Ultrasonic
          FingerprintSensorProperties.TYPE_REAR -> SensorType.RFPS
          FingerprintSensorProperties.TYPE_POWER_BUTTON -> SensorType.SFPS
          else -> SensorType.Unknown
        }
      return FingerprintSensorPropertyViewModel(
        props.sensorId,
        sensorStrength,
        props.maxEnrollmentsPerUser,
        sensorType
      )
    }
  }

}
fun EnrollReason.toOriginalReason(): Int {
  return when (this) {
    EnrollReason.EnrollEnrolling -> FingerprintManager.ENROLL_ENROLL
    EnrollReason.FindSensor -> FingerprintManager.ENROLL_FIND_SENSOR
  }
}
