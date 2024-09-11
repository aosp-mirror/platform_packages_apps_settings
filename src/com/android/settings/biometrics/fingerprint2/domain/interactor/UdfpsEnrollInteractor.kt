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
import android.graphics.PointF
import android.util.TypedValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * This interactor provides information about the current offset of the sensor for guided enrollment
 * on UDFPS devices.
 */
interface UdfpsEnrollInteractor {
  /** Indicates at which step a UDFPS enrollment is in. */
  fun onEnrollmentStep(stepsRemaining: Int, totalStep: Int)

  /** Indicates if guided enrollment should be enabled or not. */
  fun updateGuidedEnrollment(enabled: Boolean)

  /**
   * A flow indicating how much the sensor image drawable should be offset for guided enrollment. A
   * null point indicates that the icon should be in its default position.
   */
  val guidedEnrollmentOffset: Flow<PointF>
}

/** Keeps track of which guided enrollment point we should be using */
class UdfpsEnrollInteractorImpl(
  applicationContext: Context,
  accessibilityInteractor: AccessibilityInteractor,
) : UdfpsEnrollInteractor {

  private var isGuidedEnrollment = MutableStateFlow(false)
  // Number of pixels per mm
  val px =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_MM,
      1f,
      applicationContext.resources.displayMetrics,
    )
  private val guidedEnrollmentPoints: MutableList<PointF> =
    mutableListOf(
      PointF(2.00f * px, 0.00f * px),
      PointF(0.87f * px, -2.70f * px),
      PointF(-1.80f * px, -1.31f * px),
      PointF(-1.80f * px, 1.31f * px),
      PointF(0.88f * px, 2.70f * px),
      PointF(3.94f * px, -1.06f * px),
      PointF(2.90f * px, -4.14f * px),
      PointF(-0.52f * px, -5.95f * px),
      PointF(-3.33f * px, -3.33f * px),
      PointF(-3.99f * px, -0.35f * px),
      PointF(-3.62f * px, 2.54f * px),
      PointF(-1.49f * px, 5.57f * px),
      PointF(2.29f * px, 4.92f * px),
      PointF(3.82f * px, 1.78f * px),
    )

  override fun onEnrollmentStep(stepsRemaining: Int, totalStep: Int) {
    val index = (totalStep - stepsRemaining) % guidedEnrollmentPoints.size
    _guidedEnrollment.update { guidedEnrollmentPoints[index] }
  }

  override fun updateGuidedEnrollment(enabled: Boolean) {
    isGuidedEnrollment.update { enabled }
  }

  private val _guidedEnrollment = MutableStateFlow(PointF(0f, 0f))
  override val guidedEnrollmentOffset: Flow<PointF> =
    combine(
      _guidedEnrollment,
      accessibilityInteractor.isAccessibilityEnabled,
      isGuidedEnrollment,
    ) { point, accessibilityEnabled, guidedEnrollmentEnabled ->
      if (accessibilityEnabled || !guidedEnrollmentEnabled) {
        return@combine PointF(0f, 0f)
      } else {
        return@combine PointF(point.x * SCALE, point.y * SCALE)
      }
    }

  companion object {
    private const val SCALE = 0.5f
  }
}
