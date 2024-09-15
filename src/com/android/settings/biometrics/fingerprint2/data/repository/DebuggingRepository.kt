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

import android.os.Build

/** Indicates if the developer has debugging features enabled. */
interface DebuggingRepository {

  /** A function that will return if a build is debuggable */
  fun isDebuggingEnabled(): Boolean

  /** A function that will return if udfps enrollment should be swapped with debug repos */
  fun isUdfpsEnrollmentDebuggingEnabled(): Boolean
}

class DebuggingRepositoryImpl : DebuggingRepository {
  /**
   * This flag can be flipped by the engineer which should allow for certain debugging features to
   * be enabled.
   */
  private val isBuildDebuggable = Build.IS_DEBUGGABLE
  /** This flag indicates if udfps should use debug repos to supply data to its various views. */
  private val udfpsEnrollmentDebugEnabled = false

  override fun isDebuggingEnabled(): Boolean {
    return isBuildDebuggable
  }

  override fun isUdfpsEnrollmentDebuggingEnabled(): Boolean {
    return isDebuggingEnabled() && udfpsEnrollmentDebugEnabled
  }
}
