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
import android.hardware.fingerprint.FingerprintEnrollOptions
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintManager.GenerateChallengeCallback
import android.hardware.fingerprint.FingerprintManager.RemovalCallback
import android.os.CancellationSignal
import android.util.Log
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepository
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.settings.password.ChooseLockSettingsHelper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "FingerprintManagerInteractor"

class FingerprintManagerInteractorImpl(
  applicationContext: Context,
  private val backgroundDispatcher: CoroutineDispatcher,
  private val fingerprintManager: FingerprintManager?,
  fingerprintSensorRepository: FingerprintSensorRepository,
  private val gatekeeperPasswordProvider: GatekeeperPasswordProvider,
  private val fingerprintEnrollStateRepository: FingerprintEnrollInteractor,
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
      fingerprintManager?.generateChallenge(applicationContext.userId, callback)
    }

  override val enrolledFingerprints: Flow<List<FingerprintData>?> = flow {
    emit(
      fingerprintManager?.getEnrolledFingerprints(applicationContext.userId)
        ?.map { (FingerprintData(it.name.toString(), it.biometricId, it.deviceId)) }?.toList()
    )
  }

  override val canEnrollFingerprints: Flow<Boolean> = flow {
    emit(
      fingerprintManager?.getEnrolledFingerprints(applicationContext.userId)?.size  ?: maxFingerprints < maxFingerprints
    )
  }

  override val sensorPropertiesInternal = fingerprintSensorRepository.fingerprintSensor

  override val maxEnrollableFingerprints = flow { emit(maxFingerprints) }

  override suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason,
    fingerprintEnrollOptions: FingerprintEnrollOptions,
  ): Flow<FingerEnrollState> =
    fingerprintEnrollStateRepository.enroll(
      hardwareAuthToken,
      enrollReason,
      fingerprintEnrollOptions,
    )

  override suspend fun removeFingerprint(fp: FingerprintData): Boolean = suspendCoroutine {
    val callback =
      object : RemovalCallback() {
        override fun onRemovalError(
          fp: android.hardware.fingerprint.Fingerprint,
          errMsgId: Int,
          errString: CharSequence,
        ) {
          it.resume(false)
        }

        override fun onRemovalSucceeded(
          fp: android.hardware.fingerprint.Fingerprint?,
          remaining: Int,
        ) {
          it.resume(true)
        }
      }
    fingerprintManager?.remove(
      android.hardware.fingerprint.Fingerprint(fp.name, fp.fingerId, fp.deviceId),
      applicationContext.userId,
      callback,
    )
  }

  override suspend fun renameFingerprint(fp: FingerprintData, newName: String) {
    withContext(backgroundDispatcher) {
      fingerprintManager?.rename(fp.fingerId, applicationContext.userId, newName)
    }
  }

  override suspend fun hasSideFps(): Boolean? = suspendCancellableCoroutine {
    it.resume(fingerprintManager?.isPowerbuttonFps)
  }

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
      fingerprintManager?.authenticate(
        null,
        cancellationSignal,
        authenticationCallback,
        null,
        applicationContext.userId,
      )
    }
}
