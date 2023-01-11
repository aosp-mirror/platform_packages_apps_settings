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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.RotationUtils;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

/**
 * View corresponding with udfps_enroll_view.xml
 */
public class UdfpsEnrollView extends FrameLayout implements UdfpsEnrollHelper.Listener {
    @NonNull
    private final UdfpsEnrollDrawable mFingerprintDrawable;
    @NonNull
    private final UdfpsEnrollProgressBarDrawable mFingerprintProgressDrawable;
    @NonNull
    private final Handler mHandler;

    @NonNull
    private ImageView mFingerprintProgressView;

    private int mProgressBarRadius;

    // sensorRect may be bigger than the sensor. True sensor dimensions are defined in
    // overlayParams.sensorBounds
    private Rect mSensorRect;
    private UdfpsOverlayParams mOverlayParams;

    public UdfpsEnrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mFingerprintDrawable = new UdfpsEnrollDrawable(mContext, attrs);
        mFingerprintProgressDrawable = new UdfpsEnrollProgressBarDrawable(context, attrs);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onFinishInflate() {
        ImageView fingerprintView = findViewById(R.id.udfps_enroll_animation_fp_view);
        fingerprintView.setImageDrawable(mFingerprintDrawable);
        mFingerprintProgressView = findViewById(R.id.udfps_enroll_animation_fp_progress_view);
        mFingerprintProgressView.setImageDrawable(mFingerprintProgressDrawable);
    }

    // Implements UdfpsEnrollHelper.Listener
    @Override
    public void onEnrollmentProgress(int remaining, int totalSteps) {
        mHandler.post(() -> {
            mFingerprintProgressDrawable.onEnrollmentProgress(remaining, totalSteps);
            mFingerprintDrawable.onEnrollmentProgress(remaining, totalSteps);
        });
    }

    @Override
    public void onEnrollmentHelp(int remaining, int totalSteps) {
        mHandler.post(() -> mFingerprintProgressDrawable.onEnrollmentHelp(remaining, totalSteps));
    }

    @Override
    public void onAcquired(boolean animateIfLastStepGood) {
        mHandler.post(() -> {
            if (animateIfLastStepGood) mFingerprintProgressDrawable.onLastStepAcquired();
        });
    }

    void setOverlayParams(UdfpsOverlayParams params) {
        mOverlayParams = params;

        post(() -> {
            mProgressBarRadius =
                    (int) (mOverlayParams.getScaleFactor() * getContext().getResources().getInteger(
                            R.integer.config_udfpsEnrollProgressBar));
            mSensorRect = mOverlayParams.getSensorBounds();

            onSensorRectUpdated();
        });
    }

    void setEnrollHelper(UdfpsEnrollHelper enrollHelper) {
        mFingerprintDrawable.setEnrollHelper(enrollHelper);
        enrollHelper.setListener(this);
    }

    private void onSensorRectUpdated() {
        updateDimensions();
        updateAccessibilityViewLocation();

        // Updates sensor rect in relation to the overlay view
        mSensorRect.set(getPaddingX(), getPaddingY(),
                (mOverlayParams.getSensorBounds().width() + getPaddingX()),
                (mOverlayParams.getSensorBounds().height() + getPaddingY()));
        mFingerprintDrawable.onSensorRectUpdated(new RectF(mSensorRect));
    }

    private void updateDimensions() {
        // Original sensorBounds assume portrait mode.
        Rect rotatedBounds = mOverlayParams.getSensorBounds();
        int rotation = mOverlayParams.getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            RotationUtils.rotateBounds(
                    rotatedBounds,
                    mOverlayParams.getNaturalDisplayWidth(),
                    mOverlayParams.getNaturalDisplayHeight(),
                    rotation
            );
        }

        // Use parent view's and rotatedBound's absolute coordinates to decide the margins of
        // UdfpsEnrollView, so that its center keeps consistent with sensor rect's.
        ViewGroup parentView = (ViewGroup) getParent();
        int[] coords = parentView.getLocationOnScreen();
        int parentLeft = coords[0];
        int parentTop = coords[1];
        int parentRight = parentLeft + parentView.getWidth();
        int parentBottom = parentTop + parentView.getHeight();
        MarginLayoutParams marginLayoutParams = (MarginLayoutParams) getLayoutParams();
        FrameLayout.LayoutParams params = (LayoutParams) getLayoutParams();

        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                params.gravity = Gravity.RIGHT | Gravity.TOP;
                marginLayoutParams.rightMargin = parentRight - rotatedBounds.right - getPaddingX();
                marginLayoutParams.topMargin = rotatedBounds.top - parentTop - getPaddingY();
                break;
            case Surface.ROTATION_90:
                params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                marginLayoutParams.rightMargin = parentRight - rotatedBounds.right - getPaddingX();
                marginLayoutParams.bottomMargin =
                        parentBottom - rotatedBounds.bottom - getPaddingY();
                break;
            case Surface.ROTATION_270:
                params.gravity = Gravity.LEFT | Gravity.BOTTOM;
                marginLayoutParams.leftMargin = rotatedBounds.left - parentLeft - getPaddingX();
                marginLayoutParams.bottomMargin =
                        parentBottom - rotatedBounds.bottom - getPaddingY();
                break;
        }

        params.height = rotatedBounds.height() + 2 * getPaddingX();
        params.width = rotatedBounds.width() + 2 * getPaddingY();
        setLayoutParams(params);
    }

    private void updateAccessibilityViewLocation() {
        View fingerprintAccessibilityView = findViewById(R.id.udfps_enroll_accessibility_view);
        ViewGroup.LayoutParams params = fingerprintAccessibilityView.getLayoutParams();
        params.width = mOverlayParams.getSensorBounds().width();
        params.height = mOverlayParams.getSensorBounds().height();
        fingerprintAccessibilityView.setLayoutParams(params);
        fingerprintAccessibilityView.requestLayout();
    }

    private void onFingerDown() {
        if (mOverlayParams.isOptical()) {
            mFingerprintDrawable.setShouldSkipDraw(true);
            mFingerprintDrawable.invalidateSelf();
        }
    }

    private void onFingerUp() {
        if (mOverlayParams.isOptical()) {
            mFingerprintDrawable.setShouldSkipDraw(false);
            mFingerprintDrawable.invalidateSelf();
        }
    }

    private int getPaddingX() {
        return mProgressBarRadius;
    }

    private int getPaddingY() {
        return mProgressBarRadius;
    }
}
