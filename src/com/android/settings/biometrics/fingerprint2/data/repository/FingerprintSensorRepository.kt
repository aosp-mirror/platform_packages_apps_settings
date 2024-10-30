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

import android.annotation.SuppressLint
import android.hardware.biometrics.ComponentInfoInternal
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback
import android.util.Log
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provides the [FingerprintSensor]
 *
 * TODO(b/313493336): Move this to systemui
 */
interface FingerprintSensorRepository {
  /** Get the [FingerprintSensor] */
  val fingerprintSensor: Flow<FingerprintSensor>

  /** Indicates if this device supports the side fingerprint sensor */
  val hasSideFps: Flow<Boolean>
}

class FingerprintSensorRepositoryImpl(
  private val fingerprintManager: FingerprintManager,
  backgroundDispatcher: CoroutineDispatcher,
  activityScope: CoroutineScope,
) : FingerprintSensorRepository {

  private val _fingerprintSensor = MutableSharedFlow<FingerprintSensor>(replay = 1)
  override val fingerprintSensor: Flow<FingerprintSensor>
    get() = _fingerprintSensor.asSharedFlow()

  init {
    activityScope.launch {
      callbackFlow{
        val callback =
          object : IFingerprintAuthenticatorsRegisteredCallback.Stub() {
            @SuppressLint("LongLogTag")
            override fun onAllAuthenticatorsRegistered(
              sensors: List<FingerprintSensorPropertiesInternal>
            ) {
              if (sensors.isEmpty()) {
                Log.e(TAG, "empty sensors from onAllAuthenticatorsRegistered")
              } else {
                trySend(sensors[0])
                channel.close()
              }
            }
          }
        withContext(backgroundDispatcher) {
          fingerprintManager.addAuthenticatorsRegisteredCallback(callback)
        }
        awaitClose {}
      }.collect {
        _fingerprintSensor.emit(it.toFingerprintSensor())
      }
    }
  }

  override val hasSideFps: Flow<Boolean> =
    fingerprintSensor.flatMapLatest { flow { emit(fingerprintManager.isPowerbuttonFps()) } }

  private companion object {
    const val TAG = "FingerprintSensorRepository"
  }
}
