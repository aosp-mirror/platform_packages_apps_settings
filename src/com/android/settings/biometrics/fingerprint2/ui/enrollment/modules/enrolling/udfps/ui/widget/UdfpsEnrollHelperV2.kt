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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.widget

import android.content.Context
import android.graphics.PointF
import android.util.TypedValue
import android.view.accessibility.AccessibilityManager
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.StageViewModel

/** Keeps track of which guided enrollment point we should be using */
class UdfpsEnrollHelperV2(private val mContext: Context) {

  private var isGuidedEnrollment: Boolean = false
  private val accessibilityEnabled: Boolean
  private val guidedEnrollmentPoints: MutableList<PointF>
  private var index = 0

  init {
    val am = mContext.getSystemService(AccessibilityManager::class.java)
    accessibilityEnabled = am!!.isEnabled
    guidedEnrollmentPoints = ArrayList()

    // Number of pixels per mm
    val px =
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1f, mContext.resources.displayMetrics)
    guidedEnrollmentPoints.add(PointF(2.00f * px, 0.00f * px))
    guidedEnrollmentPoints.add(PointF(0.87f * px, -2.70f * px))
    guidedEnrollmentPoints.add(PointF(-1.80f * px, -1.31f * px))
    guidedEnrollmentPoints.add(PointF(-1.80f * px, 1.31f * px))
    guidedEnrollmentPoints.add(PointF(0.88f * px, 2.70f * px))
    guidedEnrollmentPoints.add(PointF(3.94f * px, -1.06f * px))
    guidedEnrollmentPoints.add(PointF(2.90f * px, -4.14f * px))
    guidedEnrollmentPoints.add(PointF(-0.52f * px, -5.95f * px))
    guidedEnrollmentPoints.add(PointF(-3.33f * px, -3.33f * px))
    guidedEnrollmentPoints.add(PointF(-3.99f * px, -0.35f * px))
    guidedEnrollmentPoints.add(PointF(-3.62f * px, 2.54f * px))
    guidedEnrollmentPoints.add(PointF(-1.49f * px, 5.57f * px))
    guidedEnrollmentPoints.add(PointF(2.29f * px, 4.92f * px))
    guidedEnrollmentPoints.add(PointF(3.82f * px, 1.78f * px))
  }

  /**
   * This indicates whether we should be offsetting the enrollment icon based on
   * [guidedEnrollmentPoints]
   */
  fun onUpdateStage(stage: StageViewModel) {
    this.isGuidedEnrollment = stage is StageViewModel.Guided
  }

  /** Updates [index] to be used by [guidedEnrollmentPoints] */
  fun onEnrollmentProgress(remaining: Int, totalSteps: Int) {
    index = totalSteps - remaining
  }

  /**
   * Returns the current guided enrollment point, or (0,0) if we are not in guided enrollment or are
   * in accessibility.
   */
  val guidedEnrollmentLocation: PointF?
    get() {
      if (accessibilityEnabled || !isGuidedEnrollment) {
        return null
      }
      var scale = SCALE
      val originalPoint = guidedEnrollmentPoints[index % guidedEnrollmentPoints.size]
      return PointF(originalPoint.x * scale, originalPoint.y * scale)
    }

  companion object {
    private const val TAG = "UdfpsEnrollHelperV2"
    private const val SCALE = 0.5f
  }
}
