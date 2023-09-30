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

package com.android.settings.biometrics.fingerprint2.domain.interactor

import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintManager.GenerateChallengeCallback
import android.hardware.fingerprint.FingerprintManager.RemovalCallback
import android.os.CancellationSignal
import android.util.Log
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint2.conversion.toOriginalReason
import com.android.settings.biometrics.fingerprint2.shared.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.shared.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.shared.model.FingerEnrollStateViewModel
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintAuthAttemptViewModel
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintViewModel
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "FingerprintManagerInteractor"

class FingerprintManagerInteractorImpl(
  applicationContext: Context,
  private val backgroundDispatcher: CoroutineDispatcher,
  private val fingerprintManager: FingerprintManager,
  private val gatekeeperPasswordProvider: GatekeeperPasswordProvider,
  private val pressToAuthProvider: () -> Boolean,
) : FingerprintManagerInteractor {

  private val maxFingerprints =
    applicationContext.resources.getInteger(
      com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser
    )
  private val applicationContext = applicationContext.applicationContext

  override suspend fun generateChallenge(gateKeeperPasswordHandle: Long): Pair<Long, ByteArray> =
    suspendCoroutine {
      val callback = GenerateChallengeCallback { _, userId, challenge ->
        val intent = Intent()
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gateKeeperPasswordHandle)
        val challengeToken =
          gatekeeperPasswordProvider.requestGatekeeperHat(intent, challenge, userId)

        gatekeeperPasswordProvider.removeGatekeeperPasswordHandle(intent, false)
        val p = Pair(challenge, challengeToken)
        it.resume(p)
      }
      fingerprintManager.generateChallenge(applicationContext.userId, callback)
    }

  override val enrolledFingerprints: Flow<List<FingerprintViewModel>> = flow {
    emit(
      fingerprintManager
        .getEnrolledFingerprints(applicationContext.userId)
        .map { (FingerprintViewModel(it.name.toString(), it.biometricId, it.deviceId)) }
        .toList()
    )
  }

  override val canEnrollFingerprints: Flow<Boolean> = flow {
    emit(
      fingerprintManager.getEnrolledFingerprints(applicationContext.userId).size < maxFingerprints
    )
  }

  override val sensorPropertiesInternal = flow {
    val sensorPropertiesInternal = fingerprintManager.sensorPropertiesInternal
    emit(
      if (sensorPropertiesInternal.isEmpty()) null
      else sensorPropertiesInternal.first().toFingerprintSensor()
    )
  }

  override val maxEnrollableFingerprints = flow { emit(maxFingerprints) }

  override suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason,
  ): Flow<FingerEnrollStateViewModel> = callbackFlow {
    var streamEnded = false
    val enrollmentCallback =
      object : FingerprintManager.EnrollmentCallback() {
        override fun onEnrollmentProgress(remaining: Int) {
          trySend(FingerEnrollStateViewModel.EnrollProgress(remaining)).onFailure { error ->
            Log.d(TAG, "onEnrollmentProgress($remaining) failed to send, due to $error")
          }
          if (remaining == 0) {
            streamEnded = true
          }
        }

        override fun onEnrollmentHelp(helpMsgId: Int, helpString: CharSequence?) {
          trySend(FingerEnrollStateViewModel.EnrollHelp(helpMsgId, helpString.toString()))
            .onFailure { error -> Log.d(TAG, "onEnrollmentHelp failed to send, due to $error") }
        }

        override fun onEnrollmentError(errMsgId: Int, errString: CharSequence?) {
          trySend(FingerEnrollStateViewModel.EnrollError(errMsgId, errString.toString()))
            .onFailure { error -> Log.d(TAG, "onEnrollmentError failed to send, due to $error") }
          streamEnded = true
        }
      }

    val cancellationSignal = CancellationSignal()
    fingerprintManager.enroll(
      hardwareAuthToken,
      cancellationSignal,
      applicationContext.userId,
      enrollmentCallback,
      enrollReason.toOriginalReason(),
    )
    awaitClose {
      // If the stream has not been ended, and the user has stopped collecting the flow
      // before it was over, send cancel.
      if (!streamEnded) {
        cancellationSignal.cancel()
      }
    }
  }

  override suspend fun removeFingerprint(fp: FingerprintViewModel): Boolean = suspendCoroutine {
    val callback =
      object : RemovalCallback() {
        override fun onRemovalError(
          fp: android.hardware.fingerprint.Fingerprint,
          errMsgId: Int,
          errString: CharSequence
        ) {
          it.resume(false)
        }

        override fun onRemovalSucceeded(
          fp: android.hardware.fingerprint.Fingerprint?,
          remaining: Int
        ) {
          it.resume(true)
        }
      }
    fingerprintManager.remove(
      android.hardware.fingerprint.Fingerprint(fp.name, fp.fingerId, fp.deviceId),
      applicationContext.userId,
      callback
    )
  }

  override suspend fun renameFingerprint(fp: FingerprintViewModel, newName: String) {
    withContext(backgroundDispatcher) {
      fingerprintManager.rename(fp.fingerId, applicationContext.userId, newName)
    }
  }

  override suspend fun hasSideFps(): Boolean = suspendCancellableCoroutine {
    it.resume(fingerprintManager.isPowerbuttonFps)
  }

  override suspend fun pressToAuthEnabled(): Boolean = suspendCancellableCoroutine {
    it.resume(pressToAuthProvider())
  }

  override suspend fun authenticate(): FingerprintAuthAttemptViewModel =
    suspendCancellableCoroutine { c: CancellableContinuation<FingerprintAuthAttemptViewModel> ->
      val authenticationCallback =
        object : FingerprintManager.AuthenticationCallback() {

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (c.isCompleted) {
              Log.d(TAG, "framework sent down onAuthError after finish")
              return
            }
            c.resume(FingerprintAuthAttemptViewModel.Error(errorCode, errString.toString()))
          }

          override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            if (c.isCompleted) {
              Log.d(TAG, "framework sent down onAuthError after finish")
              return
            }
            c.resume(FingerprintAuthAttemptViewModel.Success(result.fingerprint?.biometricId ?: -1))
          }
        }

      val cancellationSignal = CancellationSignal()
      c.invokeOnCancellation { cancellationSignal.cancel() }
      fingerprintManager.authenticate(
        null,
        cancellationSignal,
        authenticationCallback,
        null,
        applicationContext.userId
      )
    }
}
