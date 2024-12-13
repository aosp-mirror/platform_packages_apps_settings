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

package com.android.settings.biometrics.fingerprint2.domain.interactor

import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepository
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import kotlinx.coroutines.flow.Flow

/** Interactor that propagates the type of [FingerprintSensor] this device supports. */
interface FingerprintSensorInteractor {
  /** Get the [FingerprintSensor] */
  val fingerprintSensor: Flow<FingerprintSensor>
}

class FingerprintSensorInteractorImpl(repo: FingerprintSensorRepository) :
  FingerprintSensorInteractor {
  override val fingerprintSensor: Flow<FingerprintSensor> = repo.fingerprintSensor
}
