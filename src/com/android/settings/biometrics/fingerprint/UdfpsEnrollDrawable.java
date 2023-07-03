/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.PathParser;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

/**
 * UDFPS fingerprint drawable that is shown when enrolling
 */
public class UdfpsEnrollDrawable extends Drawable {
    private static final String TAG = "UdfpsAnimationEnroll";

    private static final long TARGET_ANIM_DURATION_LONG = 800L;
    private static final long TARGET_ANIM_DURATION_SHORT = 600L;
    // 1 + SCALE_MAX is the maximum that the moving target will animate to
    private static final float SCALE_MAX = 0.25f;
    private static final float DEFAULT_STROKE_WIDTH = 3f;

    @NonNull
    private final Drawable mMovingTargetFpIcon;
    @NonNull
    private final Paint mSensorOutlinePaint;
    @NonNull
    private final Paint mBlueFill;
    @NonNull
    private final ShapeDrawable mFingerprintDrawable;

    private int mAlpha;
    private boolean mSkipDraw = false;

    @Nullable
    private RectF mSensorRect;
    @Nullable
    private UdfpsEnrollHelper mEnrollHelper;

    // Moving target animator set
    @Nullable
    AnimatorSet mTargetAnimatorSet;
    // Moving target location
    float mCurrentX;
    float mCurrentY;
    // Moving target size
    float mCurrentScale = 1.f;

    @NonNull
    private final Animator.AnimatorListener mTargetAnimListener;

    private boolean mShouldShowTipHint = false;
    private boolean mShouldShowEdgeHint = false;

    private int mEnrollIcon;
    private int mMovingTargetFill;

    UdfpsEnrollDrawable(@NonNull Context context, @Nullable AttributeSet attrs) {
        mFingerprintDrawable = defaultFactory(context);

        loadResources(context, attrs);
        mSensorOutlinePaint = new Paint(0 /* flags */);
        mSensorOutlinePaint.setAntiAlias(true);
        mSensorOutlinePaint.setColor(mMovingTargetFill);
        mSensorOutlinePaint.setStyle(Paint.Style.FILL);

        mBlueFill = new Paint(0 /* flags */);
        mBlueFill.setAntiAlias(true);
        mBlueFill.setColor(mMovingTargetFill);
        mBlueFill.setStyle(Paint.Style.FILL);

        mMovingTargetFpIcon = context.getResources()
                .getDrawable(R.drawable.ic_enrollment_fingerprint, null);
        mMovingTargetFpIcon.setTint(mEnrollIcon);
        mMovingTargetFpIcon.mutate();

        mFingerprintDrawable.setTint(mEnrollIcon);

        setAlpha(255);
        mTargetAnimListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                updateTipHintVisibility();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };
    }

    /** The [sensorRect] coordinates for the sensor area. */
    void onSensorRectUpdated(@NonNull RectF sensorRect) {
        int margin = ((int) sensorRect.height()) / 8;
        Rect bounds = new Rect((int) (sensorRect.left) + margin, (int) (sensorRect.top) + margin,
                (int) (sensorRect.right) - margin, (int) (sensorRect.bottom) - margin);
        updateFingerprintIconBounds(bounds);
        mSensorRect = sensorRect;
    }

    void setEnrollHelper(@NonNull UdfpsEnrollHelper helper) {
        mEnrollHelper = helper;
    }

    void setShouldSkipDraw(boolean skipDraw) {
        if (mSkipDraw == skipDraw) {
            return;
        }
        mSkipDraw = skipDraw;
        invalidateSelf();
    }

    void updateFingerprintIconBounds(@NonNull Rect bounds) {
        mFingerprintDrawable.setBounds(bounds);
        invalidateSelf();
        mMovingTargetFpIcon.setBounds(bounds);
        invalidateSelf();
    }

    void onEnrollmentProgress(int remaining, int totalSteps) {
        if (mEnrollHelper == null) {
            return;
        }

        if (!mEnrollHelper.isCenterEnrollmentStage()) {
            if (mTargetAnimatorSet != null && mTargetAnimatorSet.isRunning()) {
                mTargetAnimatorSet.end();
            }

            final PointF point = mEnrollHelper.getNextGuidedEnrollmentPoint();
            if (mCurrentX != point.x || mCurrentY != point.y) {
                final ValueAnimator x = ValueAnimator.ofFloat(mCurrentX, point.x);
                x.addUpdateListener(animation -> {
                    mCurrentX = (float) animation.getAnimatedValue();
                    invalidateSelf();
                });

                final ValueAnimator y = ValueAnimator.ofFloat(mCurrentY, point.y);
                y.addUpdateListener(animation -> {
                    mCurrentY = (float) animation.getAnimatedValue();
                    invalidateSelf();
                });

                final boolean isMovingToCenter = point.x == 0f && point.y == 0f;
                final long duration = isMovingToCenter
                        ? TARGET_ANIM_DURATION_SHORT
                        : TARGET_ANIM_DURATION_LONG;

                final ValueAnimator scale = ValueAnimator.ofFloat(0, (float) Math.PI);
                scale.setDuration(duration);
                scale.addUpdateListener(animation -> {
                    // Grow then shrink
                    mCurrentScale = 1
                            + SCALE_MAX * (float) Math.sin((float) animation.getAnimatedValue());
                    invalidateSelf();
                });

                mTargetAnimatorSet = new AnimatorSet();

                mTargetAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                mTargetAnimatorSet.setDuration(duration);
                mTargetAnimatorSet.addListener(mTargetAnimListener);
                mTargetAnimatorSet.playTogether(x, y, scale);
                mTargetAnimatorSet.start();
            } else {
                updateTipHintVisibility();
            }
        } else {
            updateTipHintVisibility();
        }

        updateEdgeHintVisibility();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mSkipDraw) {
            return;
        }

        // Draw moving target
        if (mEnrollHelper != null && !mEnrollHelper.isCenterEnrollmentStage()) {
            canvas.save();
            canvas.translate(mCurrentX, mCurrentY);

            if (mSensorRect != null) {
                canvas.scale(mCurrentScale, mCurrentScale,
                        mSensorRect.centerX(), mSensorRect.centerY());
                canvas.drawOval(mSensorRect, mBlueFill);
            }

            mMovingTargetFpIcon.draw(canvas);
            canvas.restore();
        } else {
            if (mSensorRect != null) {
                canvas.drawOval(mSensorRect, mSensorOutlinePaint);
            }
            mFingerprintDrawable.draw(canvas);
            mFingerprintDrawable.setAlpha(getAlpha());
            mSensorOutlinePaint.setAlpha(getAlpha());
        }

    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mFingerprintDrawable.setAlpha(alpha);
        mSensorOutlinePaint.setAlpha(alpha);
        mBlueFill.setAlpha(alpha);
        mMovingTargetFpIcon.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    private void updateTipHintVisibility() {
        final boolean shouldShow = mEnrollHelper != null && mEnrollHelper.isTipEnrollmentStage();
        // With the new update, we will git rid of most of this code, and instead
        // we will change the fingerprint icon.
        if (mShouldShowTipHint == shouldShow) {
            return;
        }
        mShouldShowTipHint = shouldShow;
    }

    private void updateEdgeHintVisibility() {
        final boolean shouldShow = mEnrollHelper != null && mEnrollHelper.isEdgeEnrollmentStage();
        if (mShouldShowEdgeHint == shouldShow) {
            return;
        }
        mShouldShowEdgeHint = shouldShow;
    }

    private ShapeDrawable defaultFactory(Context context) {
        String fpPath = context.getResources().getString(R.string.config_udfpsIcon);
        ShapeDrawable drawable = new ShapeDrawable(
                new PathShape(PathParser.createPathFromPathData(fpPath), 72f, 72f)
        );
        drawable.mutate();
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setStrokeCap(Paint.Cap.ROUND);
        drawable.getPaint().setStrokeWidth(DEFAULT_STROKE_WIDTH);
        return drawable;
    }

    private void loadResources(Context context, @Nullable AttributeSet attrs) {
        final TypedArray ta = context.obtainStyledAttributes(attrs,
                R.styleable.BiometricsEnrollView, R.attr.biometricsEnrollStyle,
                R.style.BiometricsEnrollStyle);
        mEnrollIcon = ta.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollIcon, 0);
        mMovingTargetFill = ta.getColor(
                R.styleable.BiometricsEnrollView_biometricsMovingTargetFill, 0);
        ta.recycle();
    }

}
