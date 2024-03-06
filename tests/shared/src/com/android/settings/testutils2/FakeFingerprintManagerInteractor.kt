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

package com.android.settings.testutils2

import com.android.settings.biometrics.fingerprint2.shared.domain.interactor.FingerprintManagerInteractor
import com.android.settings.biometrics.fingerprint2.shared.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.shared.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.shared.model.FingerprintData
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.SensorStrength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** Fake to be used by other classes to easily fake the FingerprintManager implementation. */
class FakeFingerprintManagerInteractor : FingerprintManagerInteractor {

  var enrollableFingerprints: Int = 5
  var enrolledFingerprintsInternal: MutableList<FingerprintData> = mutableListOf()
  var challengeToGenerate: Pair<Long, ByteArray> = Pair(-1L, byteArrayOf())
  var authenticateAttempt = FingerprintAuthAttemptModel.Success(1)
  var enrollStateViewModel: List<FingerEnrollState> =
    listOf(FingerEnrollState.EnrollProgress(5, 5))
  var pressToAuthEnabled = true

  var sensorProp =
    FingerprintSensor(
      0 /* sensorId */,
      SensorStrength.STRONG,
      5,
      FingerprintSensorType.POWER_BUTTON
    )

  override suspend fun authenticate(): FingerprintAuthAttemptModel {
    return authenticateAttempt
  }

  override suspend fun generateChallenge(gateKeeperPasswordHandle: Long): Pair<Long, ByteArray> {
    return challengeToGenerate
  }

  override val enrolledFingerprints: Flow<List<FingerprintData>> = flow {
    emit(enrolledFingerprintsInternal)
  }

  override val canEnrollFingerprints: Flow<Boolean> = flow {
    emit(enrolledFingerprintsInternal.size < enrollableFingerprints)
  }

  override val sensorPropertiesInternal: Flow<FingerprintSensor?> = flow { emit(sensorProp) }

  override val maxEnrollableFingerprints: Flow<Int> = flow { emit(enrollableFingerprints) }

  override suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason
  ): Flow<FingerEnrollState> = flowOf(*enrollStateViewModel.toTypedArray())

  override suspend fun removeFingerprint(fp: FingerprintData): Boolean {
    return enrolledFingerprintsInternal.remove(fp)
  }

  override suspend fun renameFingerprint(fp: FingerprintData, newName: String) {
    if (enrolledFingerprintsInternal.remove(fp)) {
      enrolledFingerprintsInternal.add(FingerprintData(newName, fp.fingerId, fp.deviceId))
    }
  }

  override suspend fun hasSideFps(): Boolean {
    return sensorProp.sensorType == FingerprintSensorType.POWER_BUTTON
  }

  override suspend fun pressToAuthEnabled(): Boolean {
    return pressToAuthEnabled
  }
}
