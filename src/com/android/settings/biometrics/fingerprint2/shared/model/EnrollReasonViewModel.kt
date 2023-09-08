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

package com.android.settings.biometrics.fingerprint2.shared.model

/** The reason for enrollment */
enum class EnrollReason {
  /**
   * The enroll happens on education screen. This is to support legacy flows where we require the
   * user to touch the sensor before going ahead to the EnrollEnrolling flow
   */
  FindSensor,
  /** The enroll happens on enrolling screen. */
  EnrollEnrolling
}
