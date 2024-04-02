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

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.ColorInt
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import android.util.AttributeSet
import android.util.Log
import android.util.PathParser
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.addListener
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel.StageViewModel
import kotlin.math.sin

/**
 * This class is responsible for drawing the udfps icon, and to update its movement based on the
 * various stages of enrollment
 */
class UdfpsEnrollIconV2 internal constructor(context: Context, attrs: AttributeSet?) : Drawable() {
  private var targetAnimatorSet: AnimatorSet? = null
  private val movingTargetFpIcon: Drawable
  private val fingerprintDrawable: ShapeDrawable
  private val sensorOutlinePaint: Paint
  private val blueFill: Paint
  private val helper = UdfpsEnrollHelperV2(context)
  @ColorInt private var enrollIconColor = 0
  @ColorInt private var movingTargetFill = 0
  private var currentScale = 1.0f
  private var alpha = 0

  /**
   * This is the physical location of the sensor. This rect will be updated by [drawSensorRectAt]
   */
  private val sensorRectBounds: Rect = Rect()

  /**
   * The following values are used to describe where the icon should be drawn. [currX] and [currY]
   * are changed based on the current guided enrollment step which is given by the
   * [UdfpsEnrollHelperV2]
   */
  private var currX = 0f
  private var currY = 0f

  private var sensorWidth = 0f
  private var sensorHeight = 0f

  init {
    fingerprintDrawable = createUdfpsIcon(context)
    context
      .obtainStyledAttributes(
        attrs,
        R.styleable.BiometricsEnrollView,
        R.attr.biometricsEnrollStyle,
        R.style.BiometricsEnrollStyle,
      )
      .let {
        enrollIconColor = it.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollIcon, 0)
        movingTargetFill =
          it.getColor(R.styleable.BiometricsEnrollView_biometricsMovingTargetFill, 0)
        it.recycle()
      }

    sensorOutlinePaint = Paint(0 /* flags */).apply {
      isAntiAlias = true
      setColor(movingTargetFill)
      style = Paint.Style.FILL
    }

    blueFill = Paint(0 /* flags */).apply {
      isAntiAlias = true
      setColor(movingTargetFill)
      style = Paint.Style.FILL
    }

    movingTargetFpIcon = context.resources.getDrawable(R.drawable.ic_enrollment_fingerprint, null).apply {
      setTint(enrollIconColor)
      mutate()
    }

    fingerprintDrawable.setTint(enrollIconColor)
    setAlpha(255)
  }

  override fun getAlpha(): Int {
    return alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {}

  override fun getOpacity(): Int {
    return PixelFormat.UNKNOWN
  }

  override fun setAlpha(alpha: Int) {
    this.alpha = alpha
  }

  /**
   * The [sensorRect] coordinates for the sensor area. The [sensorRect] should be the coordinates
   * with respect to its root frameview
   */
  fun drawSensorRectAt(sensorRect: Rect) {
    Log.e(TAG, "UdfpsEnrollIcon#drawSensorRect($sensorRect)")
    sensorRectBounds.set(sensorRect)
    fingerprintDrawable.bounds = sensorRect
    movingTargetFpIcon.bounds = sensorRect
    currX = sensorRect.left.toFloat()
    currY = sensorRect.top.toFloat()
    sensorWidth = (sensorRect.right - sensorRect.left).toFloat()
    sensorHeight = (sensorRect.bottom - sensorRect.top).toFloat()
    invalidateSelf()
  }

  /** Update the progress of the icon */
  fun onEnrollmentProgress(remaining: Int, totalSteps: Int) {
    helper.onEnrollmentProgress(remaining, totalSteps)
    val offset = helper.guidedEnrollmentLocation
    val currentBounds = getCurrLocation().toRect()
    if (offset != null) {
      // This is the desired location of the sensor rect, the [EnrollHelper]
      // offsets the initial sensor rect by a bit to get the user to move their finger a bit more.
      val targetRect = Rect(sensorRectBounds).toRectF()
      targetRect.offset(offset.x, offset.y)
      var shouldAnimateMovement =
        !currentBounds.equals(targetRect) && offset.x != 0f && offset.y != 0f
      if (shouldAnimateMovement) {
        targetAnimatorSet?.let { it.cancel() }
        animateMovement(currentBounds, targetRect, true)
      }
    } else {
      // If we are not offsetting the sensor, move it back to its original place
      animateMovement(currentBounds, sensorRectBounds.toRectF(), false)
    }

    invalidateSelf()
  }

  /** Update the stage of the icon */
  fun updateStage(it: StageViewModel) {
    helper.onUpdateStage(it)
    invalidateSelf()
  }

  /** Stop drawing the fingerprint icon. */
  fun stopDrawing() {
    alpha = 0
  }

  /** Resume drawing the fingerprint icon */
  fun startDrawing() {
    alpha = 255
  }

  override fun draw(canvas: Canvas) {
    movingTargetFpIcon.alpha = alpha
    fingerprintDrawable.setAlpha(alpha)
    sensorOutlinePaint.setAlpha(alpha)
    val currLocation = getCurrLocation()
    canvas.scale(currentScale, currentScale, currLocation.centerX(), currLocation.centerY())

    sensorRectBounds?.let { canvas.drawOval(currLocation, sensorOutlinePaint) }
    fingerprintDrawable.bounds = currLocation.toRect()
    fingerprintDrawable.draw(canvas)
  }

  private fun getCurrLocation(): RectF =
    RectF(currX, currY, currX + sensorWidth, currY + sensorHeight)

  private fun animateMovement(currentBounds: Rect, offsetRect: RectF, scaleMovement: Boolean) {
    if (currentBounds.equals(offsetRect)) {
      return
    }
    val xAnimator = ValueAnimator.ofFloat(currentBounds.left.toFloat(), offsetRect.left)
    xAnimator.addUpdateListener {
      currX = it.animatedValue as Float
      invalidateSelf()
    }

    val yAnimator = ValueAnimator.ofFloat(currentBounds.top.toFloat(), offsetRect.top)
    yAnimator.addUpdateListener {
      currY = it.animatedValue as Float
      invalidateSelf()
    }
    val animators = mutableListOf(xAnimator, yAnimator)
    val duration = TARGET_ANIM_DURATION_LONG
    if (scaleMovement) {
      val scaleAnimator = ValueAnimator.ofFloat(0f, Math.PI.toFloat())
      scaleAnimator.setDuration(duration)
      scaleAnimator.addUpdateListener { animation: ValueAnimator ->
        // Grow then shrink
        currentScale =
          (1 + SCALE_MAX * sin((animation.getAnimatedValue() as Float).toDouble()).toFloat())
        invalidateSelf()
      }
      scaleAnimator.addListener(onEnd = { currentScale = 1f })
      animators.add(scaleAnimator)
    }

    targetAnimatorSet = AnimatorSet()

    targetAnimatorSet?.let {
      it.interpolator = AccelerateDecelerateInterpolator()
      it.setDuration(duration)
      it.playTogether(animators.toList())
      it.start()
    }
  }

  companion object {
    private const val TAG = "UdfpsEnrollDrawableV2"
    private const val DEFAULT_STROKE_WIDTH = 3f
    private const val TARGET_ANIM_DURATION_LONG = 800L
    private const val SCALE_MAX = 0.25f

    private fun createUdfpsIcon(context: Context): ShapeDrawable {
      val fpPath = context.resources.getString(R.string.config_udfpsIcon)
      val drawable = ShapeDrawable(PathShape(PathParser.createPathFromPathData(fpPath), 72f, 72f)).apply {
        mutate()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = DEFAULT_STROKE_WIDTH
      }
      return drawable
    }
  }
}
