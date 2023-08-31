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

package com.android.settings.fingerprint2.domain.interactor

import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintSensorProperties.TYPE_POWER_BUTTON
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintAuthAttemptViewModel
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Fake to be used by other classes to easily fake the FingerprintManager implementation. */
class FakeFingerprintManagerInteractor : FingerprintManagerInteractor {

  var enrollableFingerprints: Int = 5
  var enrolledFingerprintsInternal: MutableList<FingerprintViewModel> = mutableListOf()
  var challengeToGenerate: Pair<Long, ByteArray> = Pair(-1L, byteArrayOf())
  var authenticateAttempt = FingerprintAuthAttemptViewModel.Success(1)
  var pressToAuthEnabled = true

  var sensorProps =
    listOf(
      FingerprintSensorPropertiesInternal(
        0 /* sensorId */,
        SensorProperties.STRENGTH_STRONG,
        5 /* maxEnrollmentsPerUser */,
        emptyList() /* ComponentInfoInternal */,
        TYPE_POWER_BUTTON,
        true /* resetLockoutRequiresHardwareAuthToken */
      )
    )

  override suspend fun authenticate(): FingerprintAuthAttemptViewModel {
    return authenticateAttempt
  }

  override suspend fun generateChallenge(gateKeeperPasswordHandle: Long): Pair<Long, ByteArray> {
    return challengeToGenerate
  }
  override val enrolledFingerprints: Flow<List<FingerprintViewModel>> =
    flowOf(enrolledFingerprintsInternal)

  override val canEnrollFingerprints: Flow<Boolean> =
    flowOf(enrolledFingerprintsInternal.size < enrollableFingerprints)

  override val sensorPropertiesInternal: Flow<FingerprintSensorPropertiesInternal?> =
    flowOf(sensorProps.first())

  override val maxEnrollableFingerprints: Flow<Int> = flowOf(enrollableFingerprints)

  override suspend fun removeFingerprint(fp: FingerprintViewModel): Boolean {
    return enrolledFingerprintsInternal.remove(fp)
  }

  override suspend fun renameFingerprint(fp: FingerprintViewModel, newName: String) {
    if (enrolledFingerprintsInternal.remove(fp)) {
      enrolledFingerprintsInternal.add(FingerprintViewModel(newName, fp.fingerId, fp.deviceId))
    }
  }

  override suspend fun hasSideFps(): Boolean {
    return sensorProps.any { it.isAnySidefpsType }
  }

  override suspend fun pressToAuthEnabled(): Boolean {
    return pressToAuthEnabled
  }
}
