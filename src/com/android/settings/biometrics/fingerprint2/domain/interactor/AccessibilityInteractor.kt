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

import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/** Represents all of the information on accessibility state. */
interface AccessibilityInteractor {
    /** A flow that contains whether or not accessibility is enabled */
    val isAccessibilityEnabled: Flow<Boolean>
}

class AccessibilityInteractorImpl(
    accessibilityManager: AccessibilityManager,
    activityScope: LifecycleCoroutineScope
) : AccessibilityInteractor {
  /** A flow that contains whether or not accessibility is enabled */
  override val isAccessibilityEnabled: Flow<Boolean> =
    callbackFlow {
        val listener =
            AccessibilityManager.AccessibilityStateChangeListener { enabled -> trySend(enabled) }
        accessibilityManager.addAccessibilityStateChangeListener(listener)

        // This clause will be called when no one is listening to the flow
        awaitClose { accessibilityManager.removeAccessibilityStateChangeListener(listener) }
    }
      .stateIn(
        activityScope, // This is going to tied to the activity scope
          SharingStarted.WhileSubscribed(), // When no longer subscribed, we removeTheListener
        false
      )
}