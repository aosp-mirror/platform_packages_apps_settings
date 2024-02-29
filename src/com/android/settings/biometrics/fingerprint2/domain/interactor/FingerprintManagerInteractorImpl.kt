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
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricFingerprintConstants
import android.hardware.fingerprint.FingerprintEnrollOptions;
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintManager.GenerateChallengeCallback
import android.hardware.fingerprint.FingerprintManager.RemovalCallback
import android.os.CancellationSignal
import android.util.Log
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.BiometricUtils
import com.android.settings.biometrics.fingerprint2.conversion.Util.toEnrollError
import com.android.settings.biometrics.fingerprint2.conversion.Util.toOriginalReason
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepository
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintFlow
import com.android.settings.biometrics.fingerprint2.lib.model.SetupWizard
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import com.google.android.setupcompat.util.WizardManagerHelper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "FingerprintManagerInteractor"

class FingerprintManagerInteractorImpl(
  applicationContext: Context,
  private val backgroundDispatcher: CoroutineDispatcher,
  private val fingerprintManager: FingerprintManager,
  fingerprintSensorRepository: FingerprintSensorRepository,
  private val gatekeeperPasswordProvider: GatekeeperPasswordProvider,
  private val pressToAuthInteractor: PressToAuthInteractor,
  private val fingerprintFlow: FingerprintFlow,
  private val intent: Intent,
) : FingerprintManagerInteractor {

  private val maxFingerprints =
    applicationContext.resources.getInteger(
      com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser
    )
  private val applicationContext = applicationContext.applicationContext

  private val enrollRequestOutstanding = MutableStateFlow(false)

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

  override val enrolledFingerprints: Flow<List<FingerprintData>> = flow {
    emit(
      fingerprintManager
        .getEnrolledFingerprints(applicationContext.userId)
        .map { (FingerprintData(it.name.toString(), it.biometricId, it.deviceId)) }
        .toList()
    )
  }

  override val canEnrollFingerprints: Flow<Boolean> = flow {
    emit(
      fingerprintManager.getEnrolledFingerprints(applicationContext.userId).size < maxFingerprints
    )
  }

  override val sensorPropertiesInternal = fingerprintSensorRepository.fingerprintSensor

  override val maxEnrollableFingerprints = flow { emit(maxFingerprints) }

  override suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason,
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
      }

    val cancellationSignal = CancellationSignal()

    if (intent.getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1) === -1) {
      val isSuw: Boolean = WizardManagerHelper.isAnySetupWizard(intent)
      intent.putExtra(BiometricUtils.EXTRA_ENROLL_REASON,
      if (isSuw) FingerprintEnrollOptions.ENROLL_REASON_SUW else
        FingerprintEnrollOptions.ENROLL_REASON_SETTINGS)
    }

    fingerprintManager.enroll(
      hardwareAuthToken,
      cancellationSignal,
      applicationContext.userId,
      enrollmentCallback,
      enrollReason.toOriginalReason(),
      toFingerprintEnrollOptions(intent)
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
    fingerprintManager.remove(
      android.hardware.fingerprint.Fingerprint(fp.name, fp.fingerId, fp.deviceId),
      applicationContext.userId,
      callback,
    )
  }

  override suspend fun renameFingerprint(fp: FingerprintData, newName: String) {
    withContext(backgroundDispatcher) {
      fingerprintManager.rename(fp.fingerId, applicationContext.userId, newName)
    }
  }

  override suspend fun hasSideFps(): Boolean = suspendCancellableCoroutine {
    it.resume(fingerprintManager.isPowerbuttonFps)
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
      fingerprintManager.authenticate(
        null,
        cancellationSignal,
        authenticationCallback,
        null,
        applicationContext.userId,
      )
    }

  private fun toFingerprintEnrollOptions(intent: Intent): FingerprintEnrollOptions {
    val reason: Int =
      intent.getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1)
    val builder: FingerprintEnrollOptions.Builder = FingerprintEnrollOptions.Builder()
    builder.setEnrollReason(FingerprintEnrollOptions.ENROLL_REASON_UNKNOWN)
    if (reason != -1) {
      builder.setEnrollReason(reason)
    }
    return builder.build()
  }
}
