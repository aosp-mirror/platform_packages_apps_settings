/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.ColorInt;
import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * View which plays an animation to indicate where the sensor is on the device.
 */
public class FingerprintLocationAnimationView extends View implements
        FingerprintFindSensorAnimation {

    private static final float MAX_PULSE_ALPHA = 0.15f;
    private static final long DELAY_BETWEEN_PHASE = 1000;

    private final Interpolator mLinearOutSlowInInterpolator;
    private final Interpolator mFastOutSlowInInterpolator;

    private final int mDotRadius;
    private final int mMaxPulseRadius;
    private final float mFractionCenterX;
    private final float mFractionCenterY;
    private final Paint mDotPaint = new Paint();
    private final Paint mPulsePaint = new Paint();
    private float mPulseRadius;
    private ValueAnimator mRadiusAnimator;
    private ValueAnimator mAlphaAnimator;

    public FingerprintLocationAnimationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDotRadius = getResources().getDimensionPixelSize(R.dimen.fingerprint_dot_radius);
        mMaxPulseRadius = getResources().getDimensionPixelSize(R.dimen.fingerprint_pulse_radius);
        mFractionCenterX = getResources().getFraction(
                R.fraction.fingerprint_sensor_location_fraction_x, 1, 1);
        mFractionCenterY = getResources().getFraction(
                R.fraction.fingerprint_sensor_location_fraction_y, 1, 1);
        @ColorInt int colorAccent = Utils.getColorAccent(context);
        mDotPaint.setAntiAlias(true);
        mPulsePaint.setAntiAlias(true);
        mDotPaint.setColor(colorAccent);
        mPulsePaint.setColor(colorAccent);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                android.R.interpolator.linear_out_slow_in);
        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                android.R.interpolator.linear_out_slow_in);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPulse(canvas);
        drawDot(canvas);
    }

    private void drawDot(Canvas canvas) {
        canvas.drawCircle(getCenterX(), getCenterY(), mDotRadius, mDotPaint);
    }

    private void drawPulse(Canvas canvas) {
        canvas.drawCircle(getCenterX(), getCenterY(), mPulseRadius, mPulsePaint);
    }

    private float getCenterX() {
        return getWidth() * mFractionCenterX;
    }

    private float getCenterY() {
        return getHeight() * mFractionCenterY;
    }

    @Override
    public void startAnimation() {
        startPhase();
    }

    @Override
    public void stopAnimation() {
        removeCallbacks(mStartPhaseRunnable);
        if (mRadiusAnimator != null) {
            mRadiusAnimator.cancel();
        }
        if (mAlphaAnimator != null) {
            mAlphaAnimator.cancel();
        }
    }

    @Override
    public void pauseAnimation() {
        stopAnimation();
    }

    private void startPhase() {
        startRadiusAnimation();
        startAlphaAnimation();
    }

    private void startRadiusAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, mMaxPulseRadius);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPulseRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {

            boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRadiusAnimator = null;
                if (!mCancelled) {
                    postDelayed(mStartPhaseRunnable, DELAY_BETWEEN_PHASE);
                }
            }
        });
        animator.setDuration(1000);
        animator.setInterpolator(mLinearOutSlowInInterpolator);
        animator.start();
        mRadiusAnimator = animator;
    }

    private void startAlphaAnimation() {
        mPulsePaint.setAlpha((int) (255f * MAX_PULSE_ALPHA));
        ValueAnimator animator = ValueAnimator.ofFloat(MAX_PULSE_ALPHA, 0f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPulsePaint.setAlpha((int) (255f * (float) animation.getAnimatedValue()));
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAlphaAnimator = null;
            }
        });
        animator.setDuration(750);
        animator.setInterpolator(mFastOutSlowInInterpolator);
        animator.setStartDelay(250);
        animator.start();
        mAlphaAnimator = animator;
    }

    private final Runnable mStartPhaseRunnable = new Runnable() {
        @Override
        public void run() {
            startPhase();
        }
    };
}
