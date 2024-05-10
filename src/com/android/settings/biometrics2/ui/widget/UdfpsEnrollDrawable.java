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

package com.android.settings.biometrics2.ui.widget;

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
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.PathParser;
import android.util.TypedValue;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

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
    private static final float SCALE = 0.5f;
    private static final String SCALE_OVERRIDE =
            "com.android.systemui.biometrics.UdfpsEnrollHelper.scale";
    private static final String NEW_COORDS_OVERRIDE =
            "com.android.systemui.biometrics.UdfpsNewCoords";

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

    private int mTotalSteps = -1;
    private int mRemainingSteps = -1;
    private int mLocationsEnrolled = 0;
    private int mCenterTouchCount = 0;

    private FingerprintManager mFingerprintManager;

    private boolean mAccessibilityEnabled;
    private Context mContext;
    private final List<PointF> mGuidedEnrollmentPoints;

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
        mContext = context;
        mFingerprintManager = context.getSystemService(FingerprintManager.class);
        final AccessibilityManager am = context.getSystemService(AccessibilityManager.class);
        mAccessibilityEnabled = am.isEnabled();
        mGuidedEnrollmentPoints = new ArrayList<>();
        initEnrollPoint(context);
    }

    /** The [sensorRect] coordinates for the sensor area. */
    void onSensorRectUpdated(@NonNull RectF sensorRect) {
        int margin = ((int) sensorRect.height()) / 8;
        Rect bounds = new Rect((int) (sensorRect.left) + margin, (int) (sensorRect.top) + margin,
                (int) (sensorRect.right) - margin, (int) (sensorRect.bottom) - margin);
        updateFingerprintIconBounds(bounds);
        mSensorRect = sensorRect;
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

    void onEnrollmentProgress(final int remaining, final int totalSteps) {
        if (mTotalSteps == -1) {
            mTotalSteps = totalSteps;
        }

        if (remaining != mRemainingSteps) {
            mLocationsEnrolled++;
            if (isCenterEnrollmentStage()) {
                mCenterTouchCount++;
            }
        }
        mRemainingSteps = remaining;

        if (!isCenterEnrollmentStage()) {
            if (mTargetAnimatorSet != null && mTargetAnimatorSet.isRunning()) {
                mTargetAnimatorSet.end();
            }

            final PointF point = getNextGuidedEnrollmentPoint();
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
        if (!isCenterEnrollmentStage()) {
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
        final boolean shouldShow = isTipEnrollmentStage();
        // With the new update, we will git rid of most of this code, and instead
        // we will change the fingerprint icon.
        if (mShouldShowTipHint == shouldShow) {
            return;
        }
        mShouldShowTipHint = shouldShow;
    }

    private void updateEdgeHintVisibility() {
        final boolean shouldShow = isEdgeEnrollmentStage();
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

    private boolean isCenterEnrollmentStage() {
        if (mTotalSteps == -1 || mRemainingSteps == -1) {
            return true;
        }
        return mTotalSteps - mRemainingSteps < getStageThresholdSteps(mTotalSteps, 0);
    }

    private int getStageThresholdSteps(int totalSteps, int stageIndex) {
        return Math.round(totalSteps * mFingerprintManager.getEnrollStageThreshold(stageIndex));
    }

    private PointF getNextGuidedEnrollmentPoint() {
        if (mAccessibilityEnabled || !isGuidedEnrollmentStage()) {
            return new PointF(0f, 0f);
        }

        float scale = SCALE;
        if (Build.IS_ENG || Build.IS_USERDEBUG) {
            scale = Settings.Secure.getFloatForUser(mContext.getContentResolver(),
                    SCALE_OVERRIDE, SCALE,
                    UserHandle.USER_CURRENT);
        }
        final int index = mLocationsEnrolled - mCenterTouchCount;
        final PointF originalPoint = mGuidedEnrollmentPoints
                .get(index % mGuidedEnrollmentPoints.size());
        return new PointF(originalPoint.x * scale, originalPoint.y * scale);
    }

    private boolean isGuidedEnrollmentStage() {
        if (mAccessibilityEnabled || mTotalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        final int progressSteps = mTotalSteps - mRemainingSteps;
        return progressSteps >= getStageThresholdSteps(mTotalSteps, 0)
                && progressSteps < getStageThresholdSteps(mTotalSteps, 1);
    }

    private boolean isTipEnrollmentStage() {
        if (mTotalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        final int progressSteps = mTotalSteps - mRemainingSteps;
        return progressSteps >= getStageThresholdSteps(mTotalSteps, 1)
                && progressSteps < getStageThresholdSteps(mTotalSteps, 2);
    }

    private boolean isEdgeEnrollmentStage() {
        if (mTotalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        return mTotalSteps - mRemainingSteps >= getStageThresholdSteps(mTotalSteps, 2);
    }

    private void initEnrollPoint(Context context) {
        // Number of pixels per mm
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1,
                context.getResources().getDisplayMetrics());
        boolean useNewCoords = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                NEW_COORDS_OVERRIDE, 0,
                UserHandle.USER_CURRENT) != 0;
        if (useNewCoords && (Build.IS_ENG || Build.IS_USERDEBUG)) {
            Log.v(TAG, "Using new coordinates");
            mGuidedEnrollmentPoints.add(new PointF(-0.15f * px, -1.02f * px));
            mGuidedEnrollmentPoints.add(new PointF(-0.15f * px, 1.02f * px));
            mGuidedEnrollmentPoints.add(new PointF(0.29f * px, 0.00f * px));
            mGuidedEnrollmentPoints.add(new PointF(2.17f * px, -2.35f * px));
            mGuidedEnrollmentPoints.add(new PointF(1.07f * px, -3.96f * px));
            mGuidedEnrollmentPoints.add(new PointF(-0.37f * px, -4.31f * px));
            mGuidedEnrollmentPoints.add(new PointF(-1.69f * px, -3.29f * px));
            mGuidedEnrollmentPoints.add(new PointF(-2.48f * px, -1.23f * px));
            mGuidedEnrollmentPoints.add(new PointF(-2.48f * px, 1.23f * px));
            mGuidedEnrollmentPoints.add(new PointF(-1.69f * px, 3.29f * px));
            mGuidedEnrollmentPoints.add(new PointF(-0.37f * px, 4.31f * px));
            mGuidedEnrollmentPoints.add(new PointF(1.07f * px, 3.96f * px));
            mGuidedEnrollmentPoints.add(new PointF(2.17f * px, 2.35f * px));
            mGuidedEnrollmentPoints.add(new PointF(2.58f * px, 0.00f * px));
        } else {
            Log.v(TAG, "Using old coordinates");
            mGuidedEnrollmentPoints.add(new PointF(2.00f * px, 0.00f * px));
            mGuidedEnrollmentPoints.add(new PointF(0.87f * px, -2.70f * px));
            mGuidedEnrollmentPoints.add(new PointF(-1.80f * px, -1.31f * px));
            mGuidedEnrollmentPoints.add(new PointF(-1.80f * px, 1.31f * px));
            mGuidedEnrollmentPoints.add(new PointF(0.88f * px, 2.70f * px));
            mGuidedEnrollmentPoints.add(new PointF(3.94f * px, -1.06f * px));
            mGuidedEnrollmentPoints.add(new PointF(2.90f * px, -4.14f * px));
            mGuidedEnrollmentPoints.add(new PointF(-0.52f * px, -5.95f * px));
            mGuidedEnrollmentPoints.add(new PointF(-3.33f * px, -3.33f * px));
            mGuidedEnrollmentPoints.add(new PointF(-3.99f * px, -0.35f * px));
            mGuidedEnrollmentPoints.add(new PointF(-3.62f * px, 2.54f * px));
            mGuidedEnrollmentPoints.add(new PointF(-1.49f * px, 5.57f * px));
            mGuidedEnrollmentPoints.add(new PointF(2.29f * px, 4.92f * px));
            mGuidedEnrollmentPoints.add(new PointF(3.82f * px, 1.78f * px));
        }
    }

}
