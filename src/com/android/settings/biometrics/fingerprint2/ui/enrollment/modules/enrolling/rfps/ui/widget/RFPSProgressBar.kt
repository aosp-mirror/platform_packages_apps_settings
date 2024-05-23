/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.rfps.ui.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import com.android.settings.R
import com.android.settings.widget.RingProgressBar

/** Progress bar for rear fingerprint enrollment. */
class RFPSProgressBar : RingProgressBar {

  private val fastOutSlowInInterpolator: Interpolator

  private val iconAnimationDrawable: AnimatedVectorDrawable
  private val iconBackgroundBlinksDrawable: AnimatedVectorDrawable

  private val maxProgress: Int

  private var progressAnimation: ObjectAnimator? = null

  private var shouldAnimateInternal: Boolean = false

  constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
    val fingerprintDrawable = background as LayerDrawable
    iconAnimationDrawable =
      fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_animation)
        as AnimatedVectorDrawable
    iconBackgroundBlinksDrawable =
      fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_background)
        as AnimatedVectorDrawable
    fastOutSlowInInterpolator =
      AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in)
    iconAnimationDrawable.registerAnimationCallback(
      object : Animatable2.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
          super.onAnimationEnd(drawable)
          if (shouldAnimateInternal) {
            animateIconAnimationInternal()
          }
        }
      }
    )

    progressBackgroundTintMode = PorterDuff.Mode.SRC

    val attributes =
      context.obtainStyledAttributes(R.style.RingProgressBarStyle, intArrayOf(android.R.attr.max))

    maxProgress = attributes.getInt(0, -1)

    attributes.recycle()
  }

  /** Indicates if the progress animation should be running */
  fun updateIconAnimation(shouldAnimate: Boolean) {
    if (shouldAnimate && !shouldAnimateInternal) {
      animateIconAnimationInternal()
    }

    shouldAnimateInternal = shouldAnimate

  }
  /** This function should only be called when actual progress has been made. */
  fun updateProgress(percentComplete: Float) {
    val progress = maxProgress - (percentComplete.coerceIn(0.0f, 100.0f) * maxProgress).toInt()
    iconBackgroundBlinksDrawable.start()

    progressAnimation?.isRunning?.let { progressAnimation!!.cancel() }

    progressAnimation = ObjectAnimator.ofInt(this, "progress", getProgress(), progress)

    progressAnimation?.interpolator = fastOutSlowInInterpolator
    progressAnimation?.setDuration(250)
    progressAnimation?.start()
  }

  private fun animateIconAnimationInternal() {
    iconAnimationDrawable.start()
  }
}
