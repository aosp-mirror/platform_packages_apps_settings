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

import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.util.Log
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.AuthenitcateInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthenticateInteractorImpl(
  private val fingerprintManager: FingerprintManager,
  private val userId: Int,
) : AuthenitcateInteractor {

  override suspend fun authenticate(): FingerprintAuthAttemptModel =
    suspendCancellableCoroutine { c: CancellableContinuation<FingerprintAuthAttemptModel> ->
      val authenticationCallback =
        object : FingerprintManager.AuthenticationCallback() {

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (c.isCompleted) {
              Log.d(TAG, "framework sent down onAuthError after finish")
              return
            }
            c.resume(FingerprintAuthAttemptModel.Error(errorCode, errString.toString()))
          }

          override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            if (c.isCompleted) {
              Log.d(TAG, "framework sent down onAuthError after finish")
              return
            }
            c.resume(FingerprintAuthAttemptModel.Success(result.fingerprint?.biometricId ?: -1))
          }
        }

      val cancellationSignal = CancellationSignal()
      c.invokeOnCancellation { cancellationSignal.cancel() }
      fingerprintManager.authenticate(
        null,
        cancellationSignal,
        authenticationCallback,
        null,
        userId,
      )
    }

  companion object {
    private const val TAG = "AuthenticateInteractor"
  }
}
