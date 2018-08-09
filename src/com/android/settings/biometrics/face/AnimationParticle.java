/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.android.settings.R;

import java.util.List;

/**
 * Class containing the state for an individual feedback dot / path. The dots are assigned colors
 * based on their index.
 */
public class AnimationParticle {

    private static final String TAG = "AnimationParticle";

    private static final int MIN_STROKE_WIDTH = 10;
    private static final int MAX_STROKE_WIDTH = 20; // Be careful that this doesn't get clipped
    private static final int FINAL_RING_STROKE_WIDTH = 15;

    private static final float ROTATION_SPEED_NORMAL = 0.8f; // radians per second, 1 = ~57 degrees
    private static final float ROTATION_ACCELERATION_SPEED = 2.0f;
    private static final float PULSE_SPEED_NORMAL = 1 * 2 * (float) Math.PI; // 1 cycle per second
    private static final float RING_SWEEP_GROW_RATE_PRIMARY = 480; // degrees per second
    private static final float RING_SWEEP_GROW_RATE_SECONDARY = 240; // degrees per second
    private static final float RING_SIZE_FINALIZATION_TIME = 0.1f; // seconds

    private final Rect mBounds; // bounds for the canvas
    private final int mBorderWidth; // amount of padding from the edges
    private final ArgbEvaluator mEvaluator;
    private final int mErrorColor;
    private final int mIndex;
    private final Listener mListener;

    private final Paint mPaint;
    private final int mAssignedColor;
    private final float mOffsetTimeSec; // stagger particle size to make a wave effect

    private int mLastAnimationState;
    private int mAnimationState;
    private float mCurrentSize = MIN_STROKE_WIDTH;
    private float mCurrentAngle; // 0 is to the right, in radians
    private float mRotationSpeed = ROTATION_SPEED_NORMAL; // speed of dot rotation
    private float mSweepAngle = 0; // ring sweep, degrees per second
    private float mSweepRate = RING_SWEEP_GROW_RATE_SECONDARY; // acceleration
    private float mRingAdjustRate; // rate at which ring should grow/shrink to final size
    private float mRingCompletionTime; // time at which ring should be completed

    public interface Listener {
        void onRingCompleted(int index);
    }

    public AnimationParticle(Context context, Listener listener, Rect bounds, int borderWidth,
            int index, int totalParticles, List<Integer> colors) {
        mBounds = bounds;
        mBorderWidth = borderWidth;
        mEvaluator = new ArgbEvaluator();
        mErrorColor = context.getResources()
                .getColor(R.color.face_anim_particle_error, context.getTheme());
        mIndex = index;
        mListener = listener;

        mCurrentAngle = (float) index / totalParticles * 2 * (float) Math.PI;
        mOffsetTimeSec = (float) index / totalParticles
                * (1 / ROTATION_SPEED_NORMAL) * 2 * (float) Math.PI;

        mPaint = new Paint();
        mAssignedColor = colors.get(index % colors.size());
        mPaint.setColor(mAssignedColor);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mCurrentSize);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void updateState(int animationState) {
        if (mAnimationState == animationState) {
            Log.w(TAG, "Already in state " + animationState);
            return;
        }
        if (animationState == ParticleCollection.STATE_COMPLETE) {
            mPaint.setStyle(Paint.Style.STROKE);
        }
        mLastAnimationState = mAnimationState;
        mAnimationState = animationState;
    }

    // There are two types of particles, secondary and primary. Primary particles accelerate faster
    // during the "completed" animation. Particles are secondary by default.
    public void setAsPrimary() {
        mSweepRate = RING_SWEEP_GROW_RATE_PRIMARY;
    }

    public void update(long t, long dt) {
        if (mAnimationState != ParticleCollection.STATE_COMPLETE) {
            updateDot(t, dt);
        } else {
            updateRing(t, dt);
        }
    }

    private void updateDot(long t, long dt) {
        final float dtSec = 0.001f * dt;
        final float tSec = 0.001f * t;

        final float multiplier = mRotationSpeed / ROTATION_SPEED_NORMAL;

        // Calculate rotation speed / angle
        if ((mAnimationState == ParticleCollection.STATE_STOPPED_COLORFUL
                || mAnimationState == ParticleCollection.STATE_STOPPED_GRAY)
                && mRotationSpeed > 0) {
            // Linear slow down for now
            mRotationSpeed = Math.max(mRotationSpeed - ROTATION_ACCELERATION_SPEED * dtSec, 0);
        } else if (mAnimationState == ParticleCollection.STATE_STARTED
                && mRotationSpeed < ROTATION_SPEED_NORMAL) {
            // Linear speed up for now
            mRotationSpeed += ROTATION_ACCELERATION_SPEED * dtSec;
        }

        mCurrentAngle += dtSec * mRotationSpeed;

        // Calculate dot / ring size; linearly proportional with rotation speed
        mCurrentSize =
                (MAX_STROKE_WIDTH - MIN_STROKE_WIDTH) / 2
                * (float) Math.sin(tSec * PULSE_SPEED_NORMAL + mOffsetTimeSec)
                + (MAX_STROKE_WIDTH + MIN_STROKE_WIDTH) / 2;
        mCurrentSize = (mCurrentSize - MIN_STROKE_WIDTH) * multiplier + MIN_STROKE_WIDTH;

        // Calculate paint color; linearly proportional to rotation speed
        int color = mAssignedColor;
        if (mAnimationState == ParticleCollection.STATE_STOPPED_GRAY) {
            color = (int) mEvaluator.evaluate(1 - multiplier, mAssignedColor, mErrorColor);
        } else if (mLastAnimationState == ParticleCollection.STATE_STOPPED_GRAY) {
            color = (int) mEvaluator.evaluate(1 - multiplier, mAssignedColor, mErrorColor);
        }

        mPaint.setColor(color);
        mPaint.setStrokeWidth(mCurrentSize);
    }

    private void updateRing(long t, long dt) {
        final float dtSec = 0.001f * dt;
        final float tSec = 0.001f * t;

        // Store the start time, since we need to guarantee all rings reach final size at same time
        // independent of current size. The magic 0 check is safe.
        if (mRingAdjustRate == 0) {
            mRingAdjustRate =
                    (FINAL_RING_STROKE_WIDTH - mCurrentSize) / RING_SIZE_FINALIZATION_TIME;
            if (mRingCompletionTime == 0) {
                mRingCompletionTime = tSec + RING_SIZE_FINALIZATION_TIME;
            }
        }

        // Accelerate to attack speed.. jk, back to normal speed
        if (mRotationSpeed < ROTATION_SPEED_NORMAL) {
            mRotationSpeed += ROTATION_ACCELERATION_SPEED * dtSec;
        }

        // For arcs, this is the "start"
        mCurrentAngle += dtSec * mRotationSpeed;

        // Update the sweep angle until it fills entire circle
        if (mSweepAngle < 360) {
            final float sweepGrowth = mSweepRate * dtSec;
            mSweepAngle = mSweepAngle + sweepGrowth;
            mSweepRate = mSweepRate + sweepGrowth;
        }
        if (mSweepAngle > 360) {
            mSweepAngle = 360;
            mListener.onRingCompleted(mIndex);
        }

        // Animate stroke width to final size.
        if (tSec < RING_SIZE_FINALIZATION_TIME) {
            mCurrentSize = mCurrentSize + mRingAdjustRate * dtSec;
            mPaint.setStrokeWidth(mCurrentSize);
        } else {
            // There should be small to no discontinuity in this if/else
            mCurrentSize = FINAL_RING_STROKE_WIDTH;
            mPaint.setStrokeWidth(mCurrentSize);
        }

    }

    public void draw(Canvas canvas) {
        if (mAnimationState != ParticleCollection.STATE_COMPLETE) {
            drawDot(canvas);
        } else {
            drawRing(canvas);
        }
    }

    // Draws a dot at the current position on the circumference of the path.
    private void drawDot(Canvas canvas) {
        final float w = mBounds.right - mBounds.exactCenterX() - mBorderWidth;
        final float h = mBounds.bottom - mBounds.exactCenterY() - mBorderWidth;
        canvas.drawCircle(
                mBounds.exactCenterX() + w * (float) Math.cos(mCurrentAngle),
                mBounds.exactCenterY() + h * (float) Math.sin(mCurrentAngle),
                mCurrentSize,
                mPaint);
    }

    private void drawRing(Canvas canvas) {
        RectF arc = new RectF(
                mBorderWidth, mBorderWidth,
                mBounds.width() - mBorderWidth, mBounds.height() - mBorderWidth);
        Path path = new Path();
        path.arcTo(arc, (float) Math.toDegrees(mCurrentAngle), mSweepAngle);
        canvas.drawPath(path, mPaint);
    }
}
