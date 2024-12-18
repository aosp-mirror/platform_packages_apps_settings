/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint2.domain.interactor

import android.hardware.fingerprint.FingerprintEnrollOptions
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.util.Log
import com.android.settings.biometrics.fingerprint2.conversion.Util.toEnrollError
import com.android.settings.biometrics.fingerprint2.conversion.Util.toOriginalReason
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintFlow
import com.android.settings.biometrics.fingerprint2.lib.model.SetupWizard
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update

/** This repository is responsible for collecting all state related to the enroll API. */
interface FingerprintEnrollInteractor {

  /**
   * By calling this function, [fingerEnrollState] will begin to be populated with data on success.
   */
  suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason,
    fingerprintEnrollOptions: FingerprintEnrollOptions,
  ): Flow<FingerEnrollState>
}

class FingerprintEnrollInteractorImpl(
  private val userId: Int,
  private val fingerprintManager: FingerprintManager?,
  private val fingerprintFlow: FingerprintFlow,
) : FingerprintEnrollInteractor {
  private val enrollRequestOutstanding = MutableStateFlow(false)

  override suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason,
    fingerprintEnrollOptions: FingerprintEnrollOptions,
  ): Flow<FingerEnrollState> = callbackFlow {
    // TODO (b/308456120) Improve this logic
    if (enrollRequestOutstanding.value) {
      Log.d(TAG, "Outstanding enroll request, waiting 150ms")
      delay(150)
      if (enrollRequestOutstanding.value) {
        Log.e(TAG, "Request still present, continuing")
      }
    }

    enrollRequestOutstanding.update { true }

    var streamEnded = false
    var totalSteps: Int? = null
    val enrollmentCallback =
      object : FingerprintManager.EnrollmentCallback() {
        override fun onEnrollmentProgress(remaining: Int) {
          // This is sort of an implementation detail, but unfortunately the API isn't
          // very expressive. If anything we should look at changing the FingerprintManager API.
          if (totalSteps == null) {
            totalSteps = remaining + 1
          }

          trySend(FingerEnrollState.EnrollProgress(remaining, totalSteps!!)).onFailure { error ->
            Log.d(TAG, "onEnrollmentProgress($remaining) failed to send, due to $error")
          }

          if (remaining == 0) {
            streamEnded = true
            enrollRequestOutstanding.update { false }
          }
        }

        override fun onEnrollmentHelp(helpMsgId: Int, helpString: CharSequence?) {
          trySend(FingerEnrollState.EnrollHelp(helpMsgId, helpString.toString())).onFailure { error
            ->
            Log.d(TAG, "onEnrollmentHelp failed to send, due to $error")
          }
        }

        override fun onEnrollmentError(errMsgId: Int, errString: CharSequence?) {
          trySend(errMsgId.toEnrollError(fingerprintFlow == SetupWizard)).onFailure { error ->
            Log.d(TAG, "onEnrollmentError failed to send, due to $error")
          }
          Log.d(TAG, "onEnrollmentError($errMsgId)")
          streamEnded = true
          enrollRequestOutstanding.update { false }
        }

        override fun onUdfpsPointerDown(sensorId: Int) {
          trySend(FingerEnrollState.PointerDown(sensorId)).onFailure { error ->
            Log.d(TAG, "onUdfpsPointerDown failed to send, due to $error")
          }
        }

        override fun onUdfpsPointerUp(sensorId: Int) {
          trySend(FingerEnrollState.PointerUp(sensorId)).onFailure { error ->
            Log.d(TAG, "onUdfpsPointerUp failed to send, due to $error")
          }
        }

        override fun onUdfpsOverlayShown() {
          trySend(FingerEnrollState.OverlayShown).onFailure { error ->
            Log.d(TAG, "OverlayShown failed to send, due to $error")
          }
        }

        override fun onAcquired(isAcquiredGood: Boolean) {
          trySend(FingerEnrollState.Acquired(isAcquiredGood)).onFailure { error ->
            Log.d(TAG, "Acquired failed to send, due to $error")
          }
        }
      }

    val cancellationSignal = CancellationSignal()

    fingerprintManager?.enroll(
      hardwareAuthToken,
      cancellationSignal,
      userId,
      enrollmentCallback,
      enrollReason.toOriginalReason(),
      fingerprintEnrollOptions,
    )
    awaitClose {
      // If the stream has not been ended, and the user has stopped collecting the flow
      // before it was over, send cancel.
      if (!streamEnded) {
        Log.e(TAG, "Cancel is sent from settings for enroll()")
        cancellationSignal.cancel()
      }
    }
  }

  companion object {
    private const val TAG = "FingerprintEnrollStateRepository"
  }
}
