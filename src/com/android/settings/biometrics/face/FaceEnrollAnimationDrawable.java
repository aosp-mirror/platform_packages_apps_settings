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

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.android.settings.biometrics.BiometricEnrollSidecar;

/**
 * A drawable containing the circle cutout as well as the animations.
 */
public class FaceEnrollAnimationDrawable extends Drawable
        implements BiometricEnrollSidecar.Listener {

    // Tune this parameter so the UI looks nice - and so that we don't have to draw the animations
    // outside our bounds. A fraction of each rotating dot should be overlapping the camera preview.
    private static final int BORDER_BOUNDS = 20;

    private final Context mContext;
    private final ParticleCollection.Listener mListener;
    private Rect mBounds;
    private final Paint mSquarePaint;
    private final Paint mCircleCutoutPaint;

    private ParticleCollection mParticleCollection;

    private TimeAnimator mTimeAnimator;

    private final ParticleCollection.Listener mAnimationListener
            = new ParticleCollection.Listener() {
        @Override
        public void onEnrolled() {
            if (mTimeAnimator != null && mTimeAnimator.isStarted()) {
                mTimeAnimator.end();
                mListener.onEnrolled();
            }
        }
    };

    public FaceEnrollAnimationDrawable(Context context, ParticleCollection.Listener listener) {
        mContext = context;
        mListener = listener;

        mSquarePaint = new Paint();
        mSquarePaint.setColor(Color.WHITE);
        mSquarePaint.setAntiAlias(true);

        mCircleCutoutPaint = new Paint();
        mCircleCutoutPaint.setColor(Color.TRANSPARENT);
        mCircleCutoutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCircleCutoutPaint.setAntiAlias(true);
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        mParticleCollection.onEnrollmentHelp(helpMsgId, helpString);
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        mParticleCollection.onEnrollmentError(errMsgId, errString);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        mParticleCollection.onEnrollmentProgressChange(steps, remaining);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mBounds = bounds;
        mParticleCollection =
                new ParticleCollection(mContext, mAnimationListener, bounds, BORDER_BOUNDS);

        if (mTimeAnimator == null) {
            mTimeAnimator = new TimeAnimator();
            mTimeAnimator.setTimeListener((animation, totalTimeMs, deltaTimeMs) -> {
                mParticleCollection.update(totalTimeMs, deltaTimeMs);
                FaceEnrollAnimationDrawable.this.invalidateSelf();
            });
            mTimeAnimator.start();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (mBounds == null) {
            return;
        }
        canvas.save();

        // Draw a rectangle covering the whole view
        canvas.drawRect(0, 0, mBounds.width(), mBounds.height(), mSquarePaint);

        // Clear a circle in the middle for the camera preview
        canvas.drawCircle(mBounds.exactCenterX(), mBounds.exactCenterY(),
                mBounds.height() / 2 - BORDER_BOUNDS, mCircleCutoutPaint);

        // Draw the animation
        mParticleCollection.draw(canvas);

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
