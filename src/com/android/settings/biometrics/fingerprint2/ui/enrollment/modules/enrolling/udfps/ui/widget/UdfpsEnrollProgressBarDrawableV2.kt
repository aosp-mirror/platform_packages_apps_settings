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

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.annotation.ColorInt
import androidx.core.graphics.toRectF
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import kotlin.math.max
import kotlin.math.min

/**
 * UDFPS enrollment progress bar. This view is responsible for drawing the progress ring and its
 * fill around the center of the UDFPS sensor.
 */
class UdfpsEnrollProgressBarDrawableV2(private val mContext: Context, attrs: AttributeSet?) :
  Drawable() {
  private val sensorRect: Rect = Rect()
  private val strokeWidthPx: Float

  @ColorInt private val progressColor: Int
  @ColorInt private var helpColor: Int = 0
  @ColorInt private val onFirstBucketFailedColor: Int

  private val backgroundPaint: Paint

  @VisibleForTesting val fillPaint: Paint
  private val vibrator: Vibrator
  private var isAccessibilityEnabled: Boolean = false
  private var afterFirstTouch = false
  private var remainingSteps = 0
  private var totalSteps = 0
  private var progress = 0f
  private var progressAnimator: ValueAnimator? = null
  private val progressUpdateListener: AnimatorUpdateListener
  private var showingHelp = false
  private var fillColorAnimator: ValueAnimator? = null
  private val fillColorUpdateListener: AnimatorUpdateListener
  private var backgroundColorAnimator: ValueAnimator? = null
  private val backgroundColorUpdateListener: AnimatorUpdateListener
  private var complete = false
  private var movingTargetFill = 0
  private var movingTargetFillError = 0
  private var enrollProgressColor = 0
  private var enrollProgressHelp = 0
  private var enrollProgressHelpWithTalkback = 0
  private val progressBarRadius: Int

  init {
    val ta =
      mContext.obtainStyledAttributes(
        attrs,
        R.styleable.BiometricsEnrollView,
        R.attr.biometricsEnrollStyle,
        R.style.BiometricsEnrollStyle,
      )
    movingTargetFill = ta.getColor(R.styleable.BiometricsEnrollView_biometricsMovingTargetFill, 0)
    movingTargetFillError =
      ta.getColor(R.styleable.BiometricsEnrollView_biometricsMovingTargetFillError, 0)
    enrollProgressColor = ta.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollProgress, 0)
    enrollProgressHelp =
      ta.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollProgressHelp, 0)
    enrollProgressHelpWithTalkback =
      ta.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollProgressHelpWithTalkback, 0)
    ta.recycle()
    val density = mContext.resources.displayMetrics.densityDpi.toFloat()
    strokeWidthPx = STROKE_WIDTH_DP * (density / DisplayMetrics.DENSITY_DEFAULT)
    progressColor = enrollProgressColor
    onFirstBucketFailedColor = movingTargetFillError
    updateHelpColor()
    backgroundPaint = Paint().apply {
      strokeWidth = strokeWidthPx
      setColor(movingTargetFill)
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
    }

    // Progress fill should *not* use the extracted system color.
    fillPaint = Paint().apply {
      strokeWidth = strokeWidthPx
      setColor(progressColor)
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
    }
    vibrator = mContext.getSystemService(Vibrator::class.java)!!

    progressBarRadius = mContext.resources.getInteger(R.integer.config_udfpsEnrollProgressBar)

    progressUpdateListener = AnimatorUpdateListener { animation: ValueAnimator ->
      progress = animation.getAnimatedValue() as Float
      invalidateSelf()
    }
    fillColorUpdateListener = AnimatorUpdateListener { animation: ValueAnimator ->
      fillPaint.setColor(animation.getAnimatedValue() as Int)
      invalidateSelf()
    }
    backgroundColorUpdateListener = AnimatorUpdateListener { animation: ValueAnimator ->
      backgroundPaint.setColor(animation.getAnimatedValue() as Int)
      invalidateSelf()
    }
  }

  /** Indicates enrollment progress has occurred. */
  fun onEnrollmentProgress(remaining: Int, totalSteps: Int) {
    afterFirstTouch = true
    updateState(remaining, totalSteps, false /* showingHelp */)
  }

  /** Indicates enrollment help has occurred. */
  fun onEnrollmentHelp(remaining: Int, totalSteps: Int) {
    updateState(remaining, totalSteps, true /* showingHelp */)
  }

  /** Indicates the last step was acquired. */
  fun onLastStepAcquired() {
    updateState(0, totalSteps, false /* showingHelp */)
  }

  override fun draw(canvas: Canvas) {

    canvas.save()
    // This takes the sensors bounding box and expands it by [progressBarRadius] in all directions
    val sensorProgressRect = Rect(sensorRect)
    sensorProgressRect.inset(
      -progressBarRadius,
      -progressBarRadius,
      -progressBarRadius,
      -progressBarRadius,
    )

    // Rotate -90 degrees to make the progress start from the top right and not the bottom
    // right
    canvas.rotate(
      -90f,
      sensorProgressRect.centerX().toFloat(),
      sensorProgressRect.centerY().toFloat(),
    )
    if (progress < 1f) {
      // Draw the background color of the progress circle.
      canvas.drawArc(
        sensorProgressRect.toRectF(),
        0f /* startAngle */,
        360f /* sweepAngle */,
        false /* useCenter */,
        backgroundPaint,
      )
    }
    if (progress > 0f) {
      // Draw the filled portion of the progress circle.
      canvas.drawArc(
        sensorProgressRect.toRectF(),
        0f /* startAngle */,
        360f * progress /* sweepAngle */,
        false /* useCenter */,
        fillPaint,
      )
    }
    canvas.restore()
  }

  /** Do nothing here, we will control the alpha internally. */
  override fun setAlpha(alpha: Int) {}

  /** Do not apply color filters here for enrollment. */
  override fun setColorFilter(colorFilter: ColorFilter?) {}

  /** Legacy code, returning PixelFormat.UNKNOWN */
  override fun getOpacity(): Int {
    return PixelFormat.UNKNOWN
  }
  /**
   * Draws the progress with locations [sensorLocationX] [sensorLocationY], note these must be with
   * respect to the parent framelayout.
   */
  fun drawProgressAt(sensorRect: Rect) {
    this.sensorRect.set(sensorRect)
  }

  /** Indicates if accessibility is enabled or not. */
  fun setAccessibilityEnabled(enabled: Boolean) {
    isAccessibilityEnabled = enabled
    updateHelpColor()
  }

  private fun updateHelpColor() {
    helpColor =
      if (!isAccessibilityEnabled) {
        enrollProgressHelp
      } else {
        enrollProgressHelpWithTalkback
      }
  }

  private fun updateState(remainingSteps: Int, totalSteps: Int, showingHelp: Boolean) {
    updateProgress(remainingSteps, totalSteps, showingHelp)
    updateFillColor(showingHelp)
  }

  private fun updateProgress(remainingSteps: Int, totalSteps: Int, showingHelp: Boolean) {
    if (this.remainingSteps == remainingSteps && this.totalSteps == totalSteps) {
      return
    }
    if (this.showingHelp) {
      if (vibrator != null && isAccessibilityEnabled) {
        vibrator.vibrate(
          Process.myUid(),
          mContext.opPackageName,
          VIBRATE_EFFECT_ERROR,
          javaClass.getSimpleName() + "::onEnrollmentHelp",
          FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES,
        )
      }
    } else {
      // If the first touch is an error, remainingSteps will be -1 and the callback
      // doesn't come from onEnrollmentHelp. If we are in the accessibility flow,
      // we still would like to vibrate.
      if (vibrator != null) {
        if (remainingSteps == -1 && isAccessibilityEnabled) {
          vibrator.vibrate(
            Process.myUid(),
            mContext.opPackageName,
            VIBRATE_EFFECT_ERROR,
            javaClass.getSimpleName() + "::onFirstTouchError",
            FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES,
          )
        } else if (remainingSteps != -1 && !isAccessibilityEnabled) {
          vibrator.vibrate(
            Process.myUid(),
            mContext.opPackageName,
            SUCCESS_VIBRATION_EFFECT,
            javaClass.getSimpleName() + "::OnEnrollmentProgress",
            HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES,
          )
        }
      }
    }
    this.showingHelp = showingHelp
    this.remainingSteps = remainingSteps
    this.totalSteps = totalSteps
    val progressSteps = max(0.0, (totalSteps - remainingSteps).toDouble()).toInt()

    // If needed, add 1 to progress and total steps to account for initial touch.
    val adjustedSteps = if (afterFirstTouch) progressSteps + 1 else progressSteps
    val adjustedTotal = if (afterFirstTouch) this.totalSteps + 1 else this.totalSteps
    val targetProgress =
      min(1.0, (adjustedSteps.toFloat() / adjustedTotal.toFloat()).toDouble()).toFloat()
    if (progressAnimator != null && progressAnimator!!.isRunning) {
      progressAnimator!!.cancel()
    }
    progressAnimator =
      ValueAnimator.ofFloat(progress, targetProgress).also {
        it.setDuration(PROGRESS_ANIMATION_DURATION_MS)
        it.addUpdateListener(progressUpdateListener)
        it.start()
      }
    if (remainingSteps == 0) {
      startCompletionAnimation()
    } else if (remainingSteps > 0) {
      rollBackCompletionAnimation()
    }
  }

  private fun animateBackgroundColor() {
    if (backgroundColorAnimator != null && backgroundColorAnimator!!.isRunning) {
      backgroundColorAnimator!!.end()
    }
    backgroundColorAnimator =
      ValueAnimator.ofArgb(backgroundPaint.color, onFirstBucketFailedColor).also {
        it.setDuration(FILL_COLOR_ANIMATION_DURATION_MS)
        it.repeatCount = 1
        it.repeatMode = ValueAnimator.REVERSE
        it.interpolator = DEACCEL
        it.addUpdateListener(backgroundColorUpdateListener)
        it.start()
      }
  }

  private fun updateFillColor(showingHelp: Boolean) {
    if (!afterFirstTouch && showingHelp) {
      // If we are on the first touch, animate the background color
      // instead of the progress color.
      animateBackgroundColor()
      return
    }
    if (fillColorAnimator != null && fillColorAnimator!!.isRunning) {
      fillColorAnimator!!.end()
    }
    @ColorInt val targetColor = if (showingHelp) helpColor else progressColor
    fillColorAnimator =
      ValueAnimator.ofArgb(fillPaint.color, targetColor).also {
        it.setDuration(FILL_COLOR_ANIMATION_DURATION_MS)
        it.repeatCount = 1
        it.repeatMode = ValueAnimator.REVERSE
        it.interpolator = DEACCEL
        it.addUpdateListener(fillColorUpdateListener)
        it.start()
      }
  }

  private fun startCompletionAnimation() {
    if (complete) {
      return
    }
    complete = true
  }

  private fun rollBackCompletionAnimation() {
    if (!complete) {
      return
    }
    complete = false
  }

  private fun loadResources(context: Context, attrs: AttributeSet?) {}

  companion object {
    private const val TAG = "UdfpsProgressBar"
    private const val FILL_COLOR_ANIMATION_DURATION_MS = 350L
    private const val PROGRESS_ANIMATION_DURATION_MS = 400L
    private const val STROKE_WIDTH_DP = 12f
    private val DEACCEL: Interpolator = DecelerateInterpolator()
    private val VIBRATE_EFFECT_ERROR = VibrationEffect.createWaveform(longArrayOf(0, 5, 55, 60), -1)
    private val FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES =
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY)
    private val HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
    private val SUCCESS_VIBRATION_EFFECT = VibrationEffect.get(VibrationEffect.EFFECT_CLICK)
  }
}
