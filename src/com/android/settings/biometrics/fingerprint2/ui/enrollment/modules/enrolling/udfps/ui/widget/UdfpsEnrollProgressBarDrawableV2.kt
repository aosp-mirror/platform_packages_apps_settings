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
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toRectF
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * UDFPS enrollment progress bar. This view is responsible for drawing the progress ring and its
 * fill around the center of the UDFPS sensor.
 */
class UdfpsEnrollProgressBarDrawableV2(private val context: Context, attrs: AttributeSet?) :
  Drawable() {
  private val sensorRect: Rect = Rect()
  private var onFinishedCompletionAnimation: (() -> Unit)? = null
  private var rotation: Int = 0
  private val strokeWidthPx: Float

  @ColorInt private val progressColor: Int
  @ColorInt private var helpColor: Int = 0
  @ColorInt private val onFirstBucketFailedColor: Int

  private val backgroundPaint: Paint

  @VisibleForTesting val fillPaint: Paint
  private var isAccessibilityEnabled: Boolean = false
  private var afterFirstTouch = false
  private var remainingSteps = 0
  private var totalSteps = 0
  private var progress = 0f
  private var progressAnimator: ValueAnimator? = null
  private val progressUpdateListener: AnimatorUpdateListener
  private var fillColorAnimator: ValueAnimator? = null
  private val fillColorUpdateListener: AnimatorUpdateListener
  private var backgroundColorAnimator: ValueAnimator? = null
  private val backgroundColorUpdateListener: AnimatorUpdateListener
  private var movingTargetFill = 0
  private var movingTargetFillError = 0
  private var enrollProgressColor = 0
  private var enrollProgressHelp = 0
  private var enrollProgressHelpWithTalkback = 0
  private val progressBarRadius: Int
  private var checkMarkDrawable: Drawable
  private var checkMarkAnimator: ValueAnimator? = null

  private var fillColorAnimationDuration = FILL_COLOR_ANIMATION_DURATION_MS
  private var animateArcDuration = PROGRESS_ANIMATION_DURATION_MS
  private var checkmarkAnimationDelayDuration = CHECKMARK_ANIMATION_DELAY_MS
  private var checkmarkAnimationDuration = CHECKMARK_ANIMATION_DURATION_MS

  init {
    val ta =
      context.obtainStyledAttributes(
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
    val density = context.resources.displayMetrics.densityDpi.toFloat()
    strokeWidthPx = STROKE_WIDTH_DP * (density / DisplayMetrics.DENSITY_DEFAULT)
    progressColor = enrollProgressColor
    onFirstBucketFailedColor = movingTargetFillError
    updateHelpColor()
    backgroundPaint =
      Paint().apply {
        strokeWidth = strokeWidthPx
        setColor(movingTargetFill)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
      }

    checkMarkDrawable = context.getDrawable(R.drawable.udfps_enroll_checkmark)!!

    // Progress fill should *not* use the extracted system color.
    fillPaint =
      Paint().apply {
        strokeWidth = strokeWidthPx
        setColor(progressColor)
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
      }

    progressBarRadius = context.resources.getInteger(R.integer.config_udfpsEnrollProgressBar)

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
  fun onEnrollmentProgress(remaining: Int, totalSteps: Int, isRecreating: Boolean = false) {
    afterFirstTouch = true
    updateProgress(remaining, totalSteps, isRecreating)
  }

  /** Indicates enrollment help has occurred. */
  fun onEnrollmentHelp() {
    if (remainingSteps == totalSteps) {
      // We haven't had any progress, animate the background arc fill
      animateBackgroundColor()
    } else {
      // else we have had progress, animate the progress fill
      flashHelpFillColor()
    }
  }

  /** Indicates the last step was acquired. */
  fun onLastStepAcquired() {}

  override fun draw(canvas: Canvas) {

    canvas.save()
    // This takes the sensors bounding box and expands it by [progressBarRadius] in all directions
    val sensorProgressRect = getSensorProgressRect()

    // Rotate -90 degrees to make the progress start from the top right and not the bottom
    // right
    canvas.rotate(
      rotation - 90f,
      sensorProgressRect.centerX().toFloat(),
      sensorProgressRect.centerY().toFloat(),
    )
    if (progress < 1f) {
      // Draw the background color of the progress circle.
      canvas.drawArc(
        sensorProgressRect.toRectF(),
        0f, /* startAngle */
        360f, /* sweepAngle */
        false, /* useCenter */
        backgroundPaint,
      )
    }
    if (progress > 0f) {
      // Draw the filled portion of the progress circle.
      canvas.drawArc(
        sensorProgressRect.toRectF(),
        0f, /* startAngle */
        360f * progress, /* sweepAngle */
        false, /* useCenter */
        fillPaint,
      )
    }

    canvas.restore()
    checkMarkDrawable.draw(canvas)
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
  fun drawProgressAt(overlayParams: UdfpsOverlayParams) {
    this.sensorRect.set(overlayParams.sensorBounds)
    invalidateSelf()
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

  private fun updateProgress(remainingSteps: Int, totalSteps: Int, isRecreating: Boolean) {
    if (this.remainingSteps == remainingSteps && this.totalSteps == totalSteps) {
      return
    }

    // If we are restoring this view from a saved state, set animation duration to 0 to avoid
    // animating progress that has already occurred.
    if (isRecreating) {
      setAnimationTimeToZero()
    } else {
      restoreAnimationTime()
    }

    this.remainingSteps = remainingSteps
    this.totalSteps = totalSteps
    val targetProgress = (totalSteps - remainingSteps).toFloat().div(max(1, totalSteps))

    if (progressAnimator != null && progressAnimator!!.isRunning) {
      progressAnimator!!.cancel()
    }
    /** The [progressUpdateListener] will force re-[draw]s to occur depending on the progress. */
    progressAnimator =
      ValueAnimator.ofFloat(progress, targetProgress).also {
        it.setDuration(animateArcDuration)
        it.addUpdateListener(progressUpdateListener)
        it.start()
      }
    if (remainingSteps == 0) {
      runCompletionAnimation()
    }
  }

  private fun runCompletionAnimation() {
    checkMarkAnimator?.cancel()

    checkMarkAnimator = ValueAnimator.ofFloat(0f, 1f)
    checkMarkAnimator?.apply {
      startDelay = checkmarkAnimationDelayDuration
      setDuration(checkmarkAnimationDuration)
      interpolator = OvershootInterpolator()
      addUpdateListener {
        val newBounds = getCheckMarkStartBounds()
        val scale = it.animatedFraction
        newBounds.set(
          newBounds.left,
          newBounds.top,
          (newBounds.left + (newBounds.width() * scale)).toInt(),
          (newBounds.top + (newBounds.height() * scale)).toInt(),
        )
        checkMarkDrawable.bounds = newBounds
        checkMarkDrawable.setVisible(true, false)
      }
      doOnEnd { onFinishedCompletionAnimation?.let { it() } }

      start()
    }
  }

  /**
   * This returns the bounds for which the checkmark drawable should be drawn at. It should be drawn
   * on the arc of the progress bar at the 315 degree mark.
   */
  private fun getCheckMarkStartBounds(): Rect {
    val progressBounds = getSensorProgressRect()
    val radius = progressBounds.width() / 2.0

    var x = (cos(Math.toRadians(315.0)) * radius).toInt() + progressBounds.centerX()
    // Remember to negate this value as sin(>180) will return negative value
    var y = (-sin(Math.toRadians(315.0)) * radius).toInt() + progressBounds.centerY()
    // Subtract height|width /2 to make sure we draw in the middle of the arc.
    x -= (checkMarkDrawable.intrinsicWidth / 2.0).toInt()
    y -= (checkMarkDrawable.intrinsicHeight / 2.0).toInt()

    return Rect(x, y, x + checkMarkDrawable.intrinsicWidth, y + checkMarkDrawable.intrinsicHeight)
  }

  private fun getSensorProgressRect(): Rect {
    val sensorProgressRect = Rect(sensorRect)
    sensorProgressRect.inset(
      -progressBarRadius,
      -progressBarRadius,
      -progressBarRadius,
      -progressBarRadius,
    )
    return sensorProgressRect
  }

  /**
   * Flashes the background progress to a different color, followed by setting it back to the
   * original progress color.
   */
  private fun animateBackgroundColor() {
    if (backgroundColorAnimator != null && backgroundColorAnimator!!.isRunning) {
      backgroundColorAnimator!!.end()
    }
    backgroundColorAnimator =
      ValueAnimator.ofArgb(backgroundPaint.color, onFirstBucketFailedColor).also {
        it.setDuration(fillColorAnimationDuration)
        it.repeatCount = 1
        it.repeatMode = ValueAnimator.REVERSE
        it.interpolator = DEACCEL
        it.addUpdateListener(backgroundColorUpdateListener)
        it.doOnEnd { backgroundPaint.color = movingTargetFill }
        it.start()
      }
  }

  /**
   * Flashes the progress to a different color, followed by setting it back to the original progress
   * color.
   */
  private fun flashHelpFillColor() {
    if (fillColorAnimator != null && fillColorAnimator!!.isRunning) {
      fillColorAnimator!!.end()
      fillColorAnimator = null
    }
    @ColorInt val targetColor = helpColor
    fillColorAnimator =
      ValueAnimator.ofArgb(fillPaint.color, targetColor).also {
        it.setDuration(fillColorAnimationDuration)
        it.repeatCount = 1
        it.repeatMode = ValueAnimator.REVERSE
        it.interpolator = DEACCEL
        it.addUpdateListener(fillColorUpdateListener)
        it.doOnEnd { fillPaint.color = enrollProgressColor }
        it.start()
      }
  }

  /**
   * This sets animation time to 0. This typically happens after an activity recreation, we don't
   * want to re-animate the progress/success animation with the default timer
   */
  private fun setAnimationTimeToZero() {
    animateArcDuration = 0
    checkmarkAnimationDelayDuration = 0
    checkmarkAnimationDuration = 0
  }

  /** This sets animation timers back to normal, this happens after we have */
  private fun restoreAnimationTime() {
    animateArcDuration = PROGRESS_ANIMATION_DURATION_MS
    checkmarkAnimationDelayDuration = CHECKMARK_ANIMATION_DELAY_MS
    checkmarkAnimationDuration = CHECKMARK_ANIMATION_DURATION_MS
  }

  /**
   * Indicates that the finish animation has completed, and enrollment can proceed to the next stage
   */
  fun setFinishAnimationCompleted(onFinishedAnimation: () -> Unit) {
    this.onFinishedCompletionAnimation = onFinishedAnimation
  }

  companion object {
    private const val TAG = "UdfpsProgressBar"
    private const val FILL_COLOR_ANIMATION_DURATION_MS = 350L
    private const val PROGRESS_ANIMATION_DURATION_MS = 400L
    private const val CHECKMARK_ANIMATION_DELAY_MS = 200L
    private const val CHECKMARK_ANIMATION_DURATION_MS = 300L
    private const val STROKE_WIDTH_DP = 12f
    private val DEACCEL: Interpolator = DecelerateInterpolator()
  }
}
