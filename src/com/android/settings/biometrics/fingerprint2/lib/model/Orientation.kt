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

package com.android.settings.biometrics.fingerprint2.lib.model

/** The orientation events correspond to androids internal orientation events. */
sealed class Orientation {
  /** Indicates the device is in landscape orientation */
  data object Landscape : Orientation()

  /** Indicates the device is in reverse landscape orientation */
  data object ReverseLandscape : Orientation()

  /** Indicates the device is in portrait orientation */
  data object Portrait : Orientation()

  /** Indicates the device is in the upside down portrait orientation */
  data object UpsideDownPortrait : Orientation()
}
