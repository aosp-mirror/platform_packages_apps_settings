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

import android.content.Context
import android.view.OrientationEventListener
import com.android.internal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Interactor which provides information about orientation
 */
interface OrientationInteractor {
  /** A flow that contains the information about the orientation changing */
  val orientation: Flow<Int>
  /** A flow that contains the rotation info */
  val rotation: Flow<Int>
  /**
   * A Helper function that computes rotation if device is in
   * [R.bool.config_reverseDefaultConfigRotation]
   */
  fun getRotationFromDefault(rotation: Int): Int
}

class OrientationInteractorImpl(private val context: Context, activityScope: CoroutineScope) :
  OrientationInteractor {

  override val orientation: Flow<Int> = callbackFlow {
    val orientationEventListener =
      object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
          trySend(orientation)
        }
      }
    orientationEventListener.enable()
    awaitClose { orientationEventListener.disable() }
  }

  override val rotation: Flow<Int> =
    callbackFlow {
      val orientationEventListener =
        object : OrientationEventListener(context) {
          override fun onOrientationChanged(orientation: Int) {
            trySend(getRotationFromDefault(context.display!!.rotation))
          }
        }
      orientationEventListener.enable()
      awaitClose { orientationEventListener.disable() }
    }
      .stateIn(
        activityScope, // This is tied to the activity scope
        SharingStarted.WhileSubscribed(), // When no longer subscribed, we removeTheListener
        context.display!!.rotation,
      )

  override fun getRotationFromDefault(rotation: Int): Int {
    val isReverseDefaultRotation =
      context.resources.getBoolean(R.bool.config_reverseDefaultRotation)
    return if (isReverseDefaultRotation) {
      (rotation + 1) % 4
    } else {
      rotation
    }
  }
}