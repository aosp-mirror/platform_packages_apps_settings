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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

/** Interactor which provides information about orientation */
interface OrientationInteractor {
  /** A flow that contains the information about the orientation changing */
  val orientation: Flow<Int>
  /**
   * This indicates the surface rotation that hte view is currently in. For instance its possible to
   * rotate a view to 90 degrees but for it to still be portrait mode. In this case, this flow
   * should emit that we are in rotation 0 (SurfaceView.Rotation_0)
   */
  val rotation: Flow<Int>
  /**
   * A flow that contains the rotation info matched against the def [config_reverseDefaultRotation]
   */
  val rotationFromDefault: Flow<Int>

  /**
   * A Helper function that computes rotation if device is in
   * [R.bool.config_reverseDefaultConfigRotation]
   */
  fun getRotationFromDefault(rotation: Int): Int
}

class OrientationInteractorImpl(private val context: Context) : OrientationInteractor {

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

  override val rotation: Flow<Int> = orientation.transform { emit(context.display.rotation) }

  override val rotationFromDefault: Flow<Int> = rotation.map { getRotationFromDefault(it) }

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
