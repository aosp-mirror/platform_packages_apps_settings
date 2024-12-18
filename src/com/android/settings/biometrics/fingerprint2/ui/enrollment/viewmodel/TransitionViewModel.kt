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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel

/** Indicates the type of transitions that can occur between fragments */
sealed class Transition {
  /** Indicates the new fragment should slide in from the left side */
  data object EnterFromLeft : Transition()

  /** Indicates the new fragment should slide in from the right side */
  data object EnterFromRight : Transition()

  /** Indicates the old fragment should slide out to the left side */
  data object ExitToLeft : Transition()

  /** Indicates the old fragment should slide out to the right side */
  data object ExitToRight : Transition()
}
