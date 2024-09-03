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

import android.hardware.biometrics.ComponentInfoInternal
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintEnrollOptions
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.AuthenitcateInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrollFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.GenerateChallengeInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.RemoveFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.RenameFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.SensorInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** Fake to be used by other classes to easily fake the FingerprintManager implementation. */
class FakeFingerprintManagerInteractor :
  AuthenitcateInteractor,
  CanEnrollFingerprintsInteractor,
  EnrolledFingerprintsInteractor,
  EnrollFingerprintInteractor,
  GenerateChallengeInteractor,
  RemoveFingerprintInteractor,
  RenameFingerprintInteractor,
  SensorInteractor {

  var enrollableFingerprints: Int = 5
  var enrolledFingerprintsInternal: MutableList<FingerprintData> = mutableListOf()
  var challengeToGenerate: Pair<Long, ByteArray> = Pair(-1L, byteArrayOf())
  var authenticateAttempt = FingerprintAuthAttemptModel.Success(1)
  var enrollStateViewModel: List<FingerEnrollState> = listOf(FingerEnrollState.EnrollProgress(5, 5))

  var sensorProp =
    FingerprintSensorPropertiesInternal(
        0 /* sensorId */,
        SensorProperties.STRENGTH_STRONG,
        5 /* maxEnrollmentsPerUser */,
        listOf<ComponentInfoInternal>(),
        FingerprintSensorProperties.TYPE_POWER_BUTTON,
        false /* halControlsIllumination */,
        true /* resetLockoutRequiresHardwareAuthToken */,
        listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
      )
      .toFingerprintSensor()

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

  override fun maxFingerprintsEnrollable(): Int {
    return enrollableFingerprints
  }

  override val sensorPropertiesInternal: Flow<FingerprintSensor?> = flow { emit(sensorProp) }
  override val hasSideFps: Flow<Boolean> =
    flowOf(sensorProp.sensorType == FingerprintSensorType.POWER_BUTTON)

  override suspend fun enroll(
    hardwareAuthToken: ByteArray?,
    enrollReason: EnrollReason,
    fingerprintEnrollOptions: FingerprintEnrollOptions,
  ): Flow<FingerEnrollState> = flowOf(*enrollStateViewModel.toTypedArray())

  override suspend fun removeFingerprint(fp: FingerprintData): Boolean {
    return enrolledFingerprintsInternal.remove(fp)
  }

  override suspend fun renameFingerprint(fp: FingerprintData, newName: String) {
    if (enrolledFingerprintsInternal.remove(fp)) {
      enrolledFingerprintsInternal.add(FingerprintData(newName, fp.fingerId, fp.deviceId))
    }
  }

}
