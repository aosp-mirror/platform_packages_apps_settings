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
import android.graphics.PointF
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
import com.android.settings.R
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams
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
  @ColorInt private var enrollIconColor = 0
  @ColorInt private var movingTargetFill = 0
  private var currentScale = 1.0f
  private var alpha = 0
  private var stopDrawing = false

  /**
   * This is the physical location of the sensor. This rect will be updated by [drawSensorRectAt]
   */
  private val sensorRectBounds: Rect = Rect()

  /**
   * The following values are used to describe where the icon should be drawn. [sensorLeftOffset]
   * and [sensorTopOffset] are changed based on the current guided enrollment step which is given by
   * the [UdfpsEnrollHelperV2]
   */
  private var sensorLeftOffset = 0f
  private var sensorTopOffset = 0f

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

    sensorOutlinePaint =
      Paint(0 /* flags */).apply {
        isAntiAlias = true
        setColor(movingTargetFill)
        style = Paint.Style.FILL
      }

    blueFill =
      Paint(0 /* flags */).apply {
        isAntiAlias = true
        setColor(movingTargetFill)
        style = Paint.Style.FILL
      }

    movingTargetFpIcon =
      context.resources.getDrawable(R.drawable.ic_enrollment_fingerprint, null).apply {
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
  fun drawSensorRectAt(overlayParams: UdfpsOverlayParams) {
    Log.e(TAG, "UdfpsEnrollIcon#drawSensorRect(${overlayParams.sensorBounds})")
    val sensorRect = overlayParams.sensorBounds
    sensorRectBounds.set(sensorRect)
    fingerprintDrawable.bounds = sensorRect
    movingTargetFpIcon.bounds = sensorRect

    // End existing animation if we get an update of the sensor rect.
    targetAnimatorSet?.end()

    invalidateSelf()
  }

  /** Stop drawing the fingerprint icon. */
  fun stopDrawing() {
    stopDrawing = true
    invalidateSelf()
  }

  /** Resume drawing the fingerprint icon */
  fun startDrawing() {
    stopDrawing = false
    invalidateSelf()
  }

  override fun draw(canvas: Canvas) {
    if (stopDrawing) {
      return
    }
    movingTargetFpIcon.alpha = alpha
    fingerprintDrawable.setAlpha(alpha)
    sensorOutlinePaint.setAlpha(alpha)
    val currLocation = getCurrLocation()
    canvas.scale(currentScale, currentScale, currLocation.centerX(), currLocation.centerY())

    canvas.drawOval(currLocation, sensorOutlinePaint)
    fingerprintDrawable.bounds = currLocation.toRect()
    fingerprintDrawable.draw(canvas)
  }

  private fun getCurrLocation(): RectF {
    val x = sensorRectBounds.left + sensorLeftOffset
    val y = sensorRectBounds.top + sensorTopOffset
    return RectF(x, y, x + sensorRectBounds.width(), y + sensorRectBounds.height())
  }

  private fun animateMovement(leftOffset: Float, topOffset: Float, scaleMovement: Boolean) {
    if (leftOffset == sensorLeftOffset && topOffset == sensorTopOffset) {
      return
    }

    val currLocation = getCurrLocation()

    val xAnimator = ValueAnimator.ofFloat(currLocation.left - sensorRectBounds.left, leftOffset)
    xAnimator.addUpdateListener {
      sensorLeftOffset = it.animatedValue as Float
      invalidateSelf()
    }

    val yAnimator = ValueAnimator.ofFloat(currLocation.top - sensorRectBounds.top, topOffset)
    yAnimator.addUpdateListener {
      sensorTopOffset = it.animatedValue as Float
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

    targetAnimatorSet?.cancel()
    targetAnimatorSet = AnimatorSet()

    targetAnimatorSet?.let {
      it.interpolator = AccelerateDecelerateInterpolator()
      it.setDuration(duration)
      it.playTogether(animators.toList())
      it.start()
    }
  }

  /**
   * Indicates a change to guided enrollment has occurred. Also indicates if we are recreating the
   * view, in which case their is no need to animate the icon to whatever position it was in.
   */
  fun updateGuidedEnrollment(point: PointF, isRecreating: Boolean) {
    val pointIsZero = point.x == 0f && point.y == 0f
    val shouldAnimateMovement = pointIsZero || !isRecreating
    animateMovement(point?.x ?: 0f, point?.y ?: 0f, shouldAnimateMovement)
  }

  companion object {
    private const val TAG = "UdfpsEnrollDrawableV2"
    private const val DEFAULT_STROKE_WIDTH = 3f
    private const val TARGET_ANIM_DURATION_LONG = 800L
    private const val SCALE_MAX = 0.25f

    private fun createUdfpsIcon(context: Context): ShapeDrawable {
      val fpPath = context.resources.getString(R.string.config_udfpsIcon)
      val drawable =
        ShapeDrawable(PathShape(PathParser.createPathFromPathData(fpPath), 72f, 72f)).apply {
          mutate()
          paint.style = Paint.Style.STROKE
          paint.strokeCap = Paint.Cap.ROUND
          paint.strokeWidth = DEFAULT_STROKE_WIDTH
        }
      return drawable
    }
  }
}
