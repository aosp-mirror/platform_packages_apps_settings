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

package com.android.settings.biometrics.fingerprint2.lib.domain.interactor

import kotlinx.coroutines.flow.Flow

/** Returns whether or not a user can enroll a fingerprint */
interface CanEnrollFingerprintsInteractor {
  /** Returns true if a user can enroll a fingerprint false otherwise. */
  val canEnrollFingerprints: Flow<Boolean>
  /** Indicates the maximum fingerprints enrollable for a given user */
  val maxFingerprintsEnrollable: Flow<Int>

  /**
   * Indicates if we should use the default settings for maximum enrollments or the sensor props
   * from the fingerprint sensor. This can be useful if you are supporting HIDL & AIDL enrollment
   * types from one code base. Prior to AIDL there was no way to determine how many
   * fingerprints were enrollable, Settings relied on
   * com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser.
   *
   * Typically Fingerprints with AIDL HAL's should not use this
   * (setShouldUseSettingsMaxFingerprints(false))
   */
  fun setShouldUseSettingsMaxFingerprints(useSettings: Boolean)
}
