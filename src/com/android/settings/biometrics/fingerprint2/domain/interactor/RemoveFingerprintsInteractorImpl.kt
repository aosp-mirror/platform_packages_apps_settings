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
import android.hardware.fingerprint.FingerprintManager.RemovalCallback
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.RemoveFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RemoveFingerprintsInteractorImpl(
  private val fingerprintManager: FingerprintManager,
  private val userId: Int,
) : RemoveFingerprintInteractor {

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
      userId,
      callback,
    )
  }
}
