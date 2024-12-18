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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.common.util

import android.content.Intent
import android.hardware.fingerprint.FingerprintEnrollOptions
import com.android.settings.biometrics.BiometricUtils

fun Intent.toFingerprintEnrollOptions(): FingerprintEnrollOptions {
  val reason: Int = this.getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1)
  val builder: FingerprintEnrollOptions.Builder = FingerprintEnrollOptions.Builder()
  builder.setEnrollReason(FingerprintEnrollOptions.ENROLL_REASON_UNKNOWN)
  if (reason != -1) {
    builder.setEnrollReason(reason)
  }
  return builder.build()
}
