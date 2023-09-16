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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel

import android.annotation.StringRes

/**
 * Represents a fingerprint enrollment state. See [FingerprintManager.EnrollmentCallback] for more
 * information
 */
sealed class FingerEnrollStateViewModel {
  /** Represents enrollment step progress. */
  data class EnrollProgress(
    val remainingSteps: Int,
  ) : FingerEnrollStateViewModel()
  /** Represents that recoverable error has been encountered during enrollment. */
  data class EnrollHelp(
    @StringRes val helpMsgId: Int,
    val helpString: String,
  ) : FingerEnrollStateViewModel()
  /** Represents that an unrecoverable error has been encountered and the operation is complete. */
  data class EnrollError(
    @StringRes val errMsgId: Int,
    val errString: String,
  ) : FingerEnrollStateViewModel()
}
