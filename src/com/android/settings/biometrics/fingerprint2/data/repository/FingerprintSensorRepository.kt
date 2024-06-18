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

import android.hardware.biometrics.ComponentInfoInternal
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Provides the [FingerprintSensor]
 *
 * TODO(b/313493336): Move this to systemui
 */
interface FingerprintSensorRepository {
  /** Get the [FingerprintSensor] */
  val fingerprintSensor: Flow<FingerprintSensor>
}

class FingerprintSensorRepositoryImpl(
    fingerprintManager: FingerprintManager,
    backgroundDispatcher: CoroutineDispatcher,
    activityScope: CoroutineScope,
) : FingerprintSensorRepository {

  override val fingerprintSensor: Flow<FingerprintSensor> =
    callbackFlow {
        val callback =
          object : IFingerprintAuthenticatorsRegisteredCallback.Stub() {
            override fun onAllAuthenticatorsRegistered(
              sensors: List<FingerprintSensorPropertiesInternal>
            ) {
              if (sensors.isEmpty()) {
                trySend(DEFAULT_PROPS)
              } else {
                trySend(sensors[0].toFingerprintSensor())
              }
            }
          }
        withContext(backgroundDispatcher) {
          fingerprintManager?.addAuthenticatorsRegisteredCallback(callback)
        }
        awaitClose {}
      }
      .stateIn(activityScope, started = SharingStarted.Eagerly, initialValue = DEFAULT_PROPS)

    companion object {
        private const val TAG = "FingerprintSensorRepoImpl"

        private val DEFAULT_PROPS =
            FingerprintSensorPropertiesInternal(
                -1 /* sensorId */,
                SensorProperties.STRENGTH_CONVENIENCE,
                0 /* maxEnrollmentsPerUser */,
                listOf<ComponentInfoInternal>(),
                FingerprintSensorProperties.TYPE_UNKNOWN,
                false /* halControlsIllumination */,
                true /* resetLockoutRequiresHardwareAuthToken */,
                listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
            )
                .toFingerprintSensor()
    }
}
