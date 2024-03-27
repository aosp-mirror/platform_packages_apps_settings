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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel

/**
 * A view model that describes the various stages of UDFPS Enrollment. This stages typically update
 * the enrollment UI in a major way, such as changing the lottie animation or changing the location
 * of the where the user should press their fingerprint
 */
sealed class StageViewModel {
  data object Unknown : StageViewModel()

  data object Guided : StageViewModel()

  data object Center : StageViewModel()

  data object Fingertip : StageViewModel()

  data object LeftEdge : StageViewModel()

  data object RightEdge : StageViewModel()
}
