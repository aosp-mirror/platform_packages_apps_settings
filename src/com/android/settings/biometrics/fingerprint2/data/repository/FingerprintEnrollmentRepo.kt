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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/** Repository that contains information about fingerprint enrollments. */
interface FingerprintEnrollmentRepository {
  /** The current enrollments of the user */
  val currentEnrollments: Flow<List<FingerprintData>?>

  /** Indicates if a user can enroll another fingerprint */
  val canEnrollUser: Flow<Boolean>

  fun maxFingerprintsEnrollable(): Int
}

class FingerprintEnrollmentRepositoryImpl(
  fingerprintManager: FingerprintManager,
  userRepo: UserRepo,
  private val settingsRepository: FingerprintSettingsRepository,
  backgroundDispatcher: CoroutineDispatcher,
  applicationScope: CoroutineScope,
) : FingerprintEnrollmentRepository {

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
      .flatMapLatest { currentUser ->
        enrollmentChangedFlow.map { enrollmentChanged ->
          if (enrollmentChanged == null || enrollmentChanged == currentUser) {
            fingerprintManager
              .getEnrolledFingerprints(currentUser)
              ?.map { (FingerprintData(it.name.toString(), it.biometricId, it.deviceId)) }
              ?.toList()
          } else {
            null
          }
        }
      }
      .filterNotNull()
      .flowOn(backgroundDispatcher)

  override val canEnrollUser: Flow<Boolean> =
    currentEnrollments.map {
      it?.size?.let { it < settingsRepository.maxEnrollableFingerprints() } ?: false
    }

  override fun maxFingerprintsEnrollable(): Int {
    return settingsRepository.maxEnrollableFingerprints()
  }
}
