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

import android.view.MotionEvent
import kotlinx.coroutines.flow.Flow

/**
 * This repository simulates touch events. This is mainly used to debug accessibility and ensure
 * that talkback is correct.
 */
interface SimulatedTouchEventsRepository {
  /** A flow simulating user touches. */
  val touchExplorationDebug: Flow<MotionEvent>
}
