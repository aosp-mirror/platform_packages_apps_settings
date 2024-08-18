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

import android.hardware.fingerprint.FingerprintManager
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EnrolledFingerprintsInteractorImpl(
  private val fingerprintManager: FingerprintManager,
  userId: Int,
) : EnrolledFingerprintsInteractor {
  override val enrolledFingerprints: Flow<List<FingerprintData>?> = flow {
    emit(
      fingerprintManager
        .getEnrolledFingerprints(userId)
        ?.map { (FingerprintData(it.name.toString(), it.biometricId, it.deviceId)) }
        ?.toList()
    )
  }
}
