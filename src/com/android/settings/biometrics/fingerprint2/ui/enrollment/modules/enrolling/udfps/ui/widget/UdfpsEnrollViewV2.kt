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
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.Acquired
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.OverlayShown
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.PointerDown
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.PointerUp
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.StageViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsEnrollEvent
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsError
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsHelp
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.UdfpsProgress

/**
 * View corresponding with fingerprint_v2_udfps_enroll_view.xml. This view is responsible for
 * drawing the [UdfpsEnrollIconV2] and the [UdfpsEnrollProgressBarDrawableV2].
 */
class UdfpsEnrollViewV2(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
  private var isAccessibilityEnabled: Boolean = false
  private lateinit var sensorRect: Rect
  private val fingerprintIcon: UdfpsEnrollIconV2 = UdfpsEnrollIconV2(mContext, attrs)
  private val fingerprintProgressDrawable: UdfpsEnrollProgressBarDrawableV2 =
    UdfpsEnrollProgressBarDrawableV2(mContext, attrs)
  private var mTotalSteps = -1
  private var mRemainingSteps = -1

  /**
   * This function computes the center (x,y) location with respect to the parent [FrameLayout] for
   * the [UdfpsEnrollProgressBarDrawableV2]. It also computes the [Rect] with respect to the parent
   * [FrameLayout] for the [UdfpsEnrollIconV2].
   */
  fun setSensorRect(rect: Rect) {
    this.sensorRect = rect

    findViewById<ImageView?>(R.id.udfps_enroll_animation_fp_progress_view)?.also {
      it.setImageDrawable(fingerprintProgressDrawable)
    }
    findViewById<ImageView>(R.id.udfps_enroll_animation_fp_view)?.also {
      it.setImageDrawable(fingerprintIcon)
    }
    val parentView = parent as ViewGroup
    val coords = parentView.getLocationOnScreen()
    val parentLeft = coords[0]
    val parentTop = coords[1]
    val sensorRectOffset = Rect(sensorRect)
    sensorRectOffset.offset(-parentLeft, -parentTop)

    fingerprintIcon.drawSensorRectAt(sensorRectOffset)
    fingerprintProgressDrawable.drawProgressAt(sensorRectOffset)
  }

  /** Updates the current enrollment stage. */
  fun updateStage(it: StageViewModel) {
    fingerprintIcon.updateStage(it)
  }

  /** Receive enroll progress event */
  fun onUdfpsEvent(event: UdfpsEnrollEvent) {
    when (event) {
      is UdfpsProgress -> onEnrollmentProgress(event.remainingSteps, event.totalSteps)
      is Acquired -> onAcquired(event.acquiredGood)
      is UdfpsHelp -> onEnrollmentHelp()
      is PointerDown -> onPointerDown()
      is PointerUp -> onPointerUp()
      OverlayShown -> overlayShown()
      is UdfpsError -> udfpsError(event.errMsgId, event.errString)
    }
  }

  /** Indicates if accessibility is enabled. */
  fun setAccessibilityEnabled(enabled: Boolean) {
    this.isAccessibilityEnabled = enabled
    fingerprintProgressDrawable.setAccessibilityEnabled(enabled)
  }

  private fun udfpsError(errMsgId: Int, errString: String) {}

  private fun overlayShown() {
    Log.e(TAG, "Implement overlayShown")
  }

  /** Receive enroll progress event */
  private fun onEnrollmentProgress(remaining: Int, totalSteps: Int) {
    fingerprintIcon.onEnrollmentProgress(remaining, totalSteps)
    fingerprintProgressDrawable.onEnrollmentProgress(remaining, totalSteps)
  }

  /** Receive enroll help event */
  private fun onEnrollmentHelp() {
    fingerprintProgressDrawable.onEnrollmentHelp()
  }

  /** Receive onAcquired event */
  private fun onAcquired(isAcquiredGood: Boolean) {
    val animateIfLastStepGood = isAcquiredGood && mRemainingSteps <= 2 && mRemainingSteps >= 0
    if (animateIfLastStepGood) fingerprintProgressDrawable.onLastStepAcquired()
  }

  /** Receive onPointerDown event */
  private fun onPointerDown() {
    fingerprintIcon.stopDrawing()
  }

  /** Receive onPointerUp event */
  private fun onPointerUp() {
    fingerprintIcon.startDrawing()
  }

  companion object {
    private const val TAG = "UdfpsEnrollView"
  }
}
