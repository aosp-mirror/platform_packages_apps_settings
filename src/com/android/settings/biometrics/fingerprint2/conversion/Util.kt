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

import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_CANCELED
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_UNABLE_TO_PROCESS
import android.hardware.fingerprint.FingerprintManager
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.shared.model.FingerEnrollState

object Util {
  fun EnrollReason.toOriginalReason(): Int {
    return when (this) {
      EnrollReason.EnrollEnrolling -> FingerprintManager.ENROLL_ENROLL
      EnrollReason.FindSensor -> FingerprintManager.ENROLL_FIND_SENSOR
    }
  }

  fun Int.toEnrollError(isSetupWizard: Boolean): FingerEnrollState.EnrollError {
    val errTitle =
      when (this) {
        FingerprintManager.FINGERPRINT_ERROR_TIMEOUT ->
          R.string.security_settings_fingerprint_enroll_error_dialog_title
        FingerprintManager.FINGERPRINT_ERROR_BAD_CALIBRATION ->
          R.string.security_settings_fingerprint_bad_calibration_title
        else -> R.string.security_settings_fingerprint_enroll_error_unable_to_process_dialog_title
      }
    val errString =
      if (isSetupWizard) {
        when (this) {
          FingerprintManager.FINGERPRINT_ERROR_TIMEOUT ->
            R.string.security_settings_fingerprint_enroll_error_dialog_title
          FingerprintManager.FINGERPRINT_ERROR_BAD_CALIBRATION ->
            R.string.security_settings_fingerprint_bad_calibration_title
          else -> R.string.security_settings_fingerprint_enroll_error_unable_to_process_dialog_title
        }
      } else {
        when (this) {
          // This message happens when the underlying crypto layer
          // decides to revoke the enrollment auth token
          FingerprintManager.FINGERPRINT_ERROR_TIMEOUT ->
            R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message
          FingerprintManager.FINGERPRINT_ERROR_BAD_CALIBRATION ->
            R.string.security_settings_fingerprint_bad_calibration
          FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS ->
            R.string.security_settings_fingerprint_enroll_error_unable_to_process_message
          // There's nothing specific to tell the user about. Ask them to try again.
          else -> R.string.security_settings_fingerprint_enroll_error_generic_dialog_message
        }
      }

    return FingerEnrollState.EnrollError(
      errTitle,
      errString,
      this == FINGERPRINT_ERROR_UNABLE_TO_PROCESS,
      this == FINGERPRINT_ERROR_CANCELED,
    )
  }

}

