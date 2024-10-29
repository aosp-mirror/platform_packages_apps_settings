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

package com.android.settings.biometrics.fingerprint2.data.repository

import android.hardware.biometrics.BiometricStateListener
import android.hardware.fingerprint.FingerprintManager
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Repository that contains information about fingerprint enrollments. */
interface FingerprintEnrollmentRepository {
  /** The current enrollments of the user */
  val currentEnrollments: Flow<List<FingerprintData>?>

  /** Indicates the maximum fingerprints that are enrollable * */
  val maxFingerprintsEnrollable: Flow<Int>

  /** Indicates if a user can enroll another fingerprint */
  val canEnrollUser: Flow<Boolean>

  val enrollStageCount: Int

  /**
   * Returns the threshold for the given stage of fingerprint enrollment.
   *
   * @param index The index of the enrollment stage.
   * @return The threshold for the enrollment stage.
   */
  fun getEnrollStageThreshold(index: Int): Float

  /**
   * Indicates if we should use the default settings for maximum enrollments or the sensor props
   * from the fingerprint sensor
   */
  fun setShouldUseSettingsMaxFingerprints(useSettings: Boolean)
}

class FingerprintEnrollmentRepositoryImpl(
  private val fingerprintManager: FingerprintManager,
  userRepo: UserRepo,
  settingsRepository: FingerprintSettingsRepository,
  backgroundDispatcher: CoroutineDispatcher,
  applicationScope: CoroutineScope,
  sensorRepo: FingerprintSensorRepository,
) : FingerprintEnrollmentRepository {

  private val _shouldUseSettingsMaxFingerprints = MutableStateFlow(false)
  val shouldUseSettingsMaxFingerprints = _shouldUseSettingsMaxFingerprints.asStateFlow()

  private val enrollmentChangedFlow: Flow<Int?> =
    callbackFlow {
        val callback =
          object : BiometricStateListener() {
            override fun onEnrollmentsChanged(userId: Int, sensorId: Int, hasEnrollments: Boolean) {
              trySend(userId)
            }
          }
        withContext(backgroundDispatcher) {
          fingerprintManager.registerBiometricStateListener(callback)
        }
        awaitClose {
          // no way to unregister
        }
      }
      .stateIn(applicationScope, started = SharingStarted.Eagerly, initialValue = null)

  override val currentEnrollments: Flow<List<FingerprintData>> =
    userRepo.currentUser
      .distinctUntilChanged()
      .combine(enrollmentChangedFlow) { currentUser, _ -> getFingerprintsForUser(currentUser) }
      .filterNotNull()
      .flowOn(backgroundDispatcher)

  override val maxFingerprintsEnrollable: Flow<Int> =
    shouldUseSettingsMaxFingerprints.combine(sensorRepo.fingerprintSensor) {
      shouldUseSettings,
      sensor ->
      if (shouldUseSettings) {
        settingsRepository.maxEnrollableFingerprints()
      } else {
        sensor.maxEnrollmentsPerUser
      }
    }

  override val canEnrollUser: Flow<Boolean> =
    currentEnrollments.combine(maxFingerprintsEnrollable) { enrollments, maxFingerprints ->
      enrollments.size < maxFingerprints
    }

  override fun setShouldUseSettingsMaxFingerprints(useSettings: Boolean) {
    _shouldUseSettingsMaxFingerprints.update { useSettings }
  }

  private fun getFingerprintsForUser(userId: Int): List<FingerprintData>? {
    return fingerprintManager
      .getEnrolledFingerprints(userId)
      ?.map { (FingerprintData(it.name.toString(), it.biometricId, it.deviceId)) }
      ?.toList()
  }

  override val enrollStageCount: Int
    get() = fingerprintManager.enrollStageCount

  override fun getEnrollStageThreshold(index: Int): Float =
    fingerprintManager.getEnrollStageThreshold(index)
}
