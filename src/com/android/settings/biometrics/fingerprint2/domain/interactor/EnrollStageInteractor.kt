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

import com.android.settings.biometrics.fingerprint2.lib.model.StageViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

typealias EnrollStageThresholds = Map<Float, StageViewModel>

/** Interactor that provides enroll stages for enrollment. */
interface EnrollStageInteractor {

  /** Provides enroll stages for enrollment. */
  val enrollStageThresholds: Flow<EnrollStageThresholds>
}

class EnrollStageInteractorImpl() : EnrollStageInteractor {
  override val enrollStageThresholds: Flow<EnrollStageThresholds> =
    flowOf(
      mapOf(
        0.0f to StageViewModel.Center,
        0.25f to StageViewModel.Guided,
        0.5f to StageViewModel.Fingertip,
        0.75f to StageViewModel.LeftEdge,
        0.875f to StageViewModel.RightEdge,
      )
    )
}
