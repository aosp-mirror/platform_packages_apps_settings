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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.RotationUtils;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.systemui.biometrics.UdfpsUtils;
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams;

/**
 * View corresponding with udfps_enroll_view.xml
 */
public class UdfpsEnrollView extends FrameLayout {
    private static final String TAG = "UdfpsEnrollView";
    @NonNull
    private final UdfpsEnrollDrawable mFingerprintDrawable;
    @NonNull
    private final UdfpsEnrollProgressBarDrawable mFingerprintProgressDrawable;
    @NonNull
    private final Handler mHandler;

    @NonNull
    private ImageView mFingerprintProgressView;
    private UdfpsUtils mUdfpsUtils;

    private int mProgressBarRadius;

    private Rect mSensorRect;
    private UdfpsOverlayParams mOverlayParams;
    private FingerprintSensorPropertiesInternal mSensorProperties;

    private int mTotalSteps = -1;
    private int mRemainingSteps = -1;
    private int mLocationsEnrolled = 0;
    private int mCenterTouchCount = 0;

    public UdfpsEnrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mFingerprintDrawable = new UdfpsEnrollDrawable(mContext, attrs);
        mFingerprintProgressDrawable = new UdfpsEnrollProgressBarDrawable(context, attrs);
        mHandler = new Handler(Looper.getMainLooper());
        mUdfpsUtils = new UdfpsUtils();
    }

    @Override
    protected void onFinishInflate() {
        ImageView fingerprintView = findViewById(R.id.udfps_enroll_animation_fp_view);
        fingerprintView.setImageDrawable(mFingerprintDrawable);
        mFingerprintProgressView = findViewById(R.id.udfps_enroll_animation_fp_progress_view);
        mFingerprintProgressView.setImageDrawable(mFingerprintProgressDrawable);
    }

    /**
     * Receive enroll progress information from FingerprintEnrollEnrollingUdfpsFragment
     */
    public void onEnrollmentProgress(int remaining, int totalSteps) {
        if (mTotalSteps == -1) {
            mTotalSteps = totalSteps;
        }
        mRemainingSteps = remaining;
        mHandler.post(() -> {
            mFingerprintProgressDrawable.onEnrollmentProgress(remaining, totalSteps);
            mFingerprintDrawable.onEnrollmentProgress(remaining, totalSteps);
        });
    }

    /**
     * Receive enroll help information from FingerprintEnrollEnrollingUdfpsFragment
     */
    public void onEnrollmentHelp() {
        mHandler.post(
                () -> mFingerprintProgressDrawable.onEnrollmentHelp(mRemainingSteps, mTotalSteps));
    }

    /**
     * Receive onAcquired from FingerprintEnrollEnrollingUdfpsFragment
     */
    public void onAcquired(boolean isAcquiredGood) {
        final boolean animateIfLastStepGood =
                isAcquiredGood && (mRemainingSteps <= 2 && mRemainingSteps >= 0);
        mHandler.post(() -> {
            onFingerUp();
            if (animateIfLastStepGood) mFingerprintProgressDrawable.onLastStepAcquired();
        });
    }

    /**
     * Receive onPointerDown from FingerprintEnrollEnrollingUdfpsFragment
     */
    public void onPointerDown(int sensorId) {
        onFingerDown();
    }

    /**
     * Receive onPointerUp from FingerprintEnrollEnrollingUdfpsFragment
     */
    public void onPointerUp(int sensorId) {
        onFingerUp();
    }

    private final ViewTreeObserver.OnDrawListener mOnDrawListener = this::updateOverlayParams;

    /**
     * setup SensorProperties
     */
    public void setSensorProperties(FingerprintSensorPropertiesInternal properties) {
        mSensorProperties = properties;
        ((ViewGroup) getParent()).getViewTreeObserver().addOnDrawListener(mOnDrawListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        final ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            final ViewTreeObserver observer = parent.getViewTreeObserver();
            if (observer != null) {
                observer.removeOnDrawListener(mOnDrawListener);
            }
        }
        super.onDetachedFromWindow();
    }

    private void onSensorRectUpdated() {
        updateDimensions();

        // Updates sensor rect in relation to the overlay view
        mSensorRect.set(getPaddingX(), getPaddingY(),
                (mOverlayParams.getSensorBounds().width() + getPaddingX()),
                (mOverlayParams.getSensorBounds().height() + getPaddingY()));
        mFingerprintDrawable.onSensorRectUpdated(new RectF(mSensorRect));
    }

    private void updateDimensions() {
        // Original sensorBounds assume portrait mode.
        final Rect rotatedBounds = new Rect(mOverlayParams.getSensorBounds());
        int rotation = mOverlayParams.getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            RotationUtils.rotateBounds(
                    rotatedBounds,
                    mOverlayParams.getNaturalDisplayWidth(),
                    mOverlayParams.getNaturalDisplayHeight(),
                    rotation
            );
        }

        RelativeLayout parent = ((RelativeLayout) getParent());
        if (parent == null) {
            Log.e(TAG, "Fail to updateDimensions for " + this + ", parent null");
            return;
        }
        final int[] coords = parent.getLocationOnScreen();
        final int parentLeft = coords[0];
        final int parentTop = coords[1];
        final int parentRight = parentLeft + parent.getWidth();
        final int parentBottom = parentTop + parent.getHeight();


        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(getWidth(),
                getHeight());
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.rightMargin = parentRight - rotatedBounds.right - getPaddingX();
            params.topMargin = rotatedBounds.top - parentTop - getPaddingY();
        } else {
            if (rotation == Surface.ROTATION_90) {
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.rightMargin = parentRight - rotatedBounds.right - getPaddingX();
                params.bottomMargin = parentBottom - rotatedBounds.bottom - getPaddingY();
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.bottomMargin = parentBottom - rotatedBounds.bottom - getPaddingY();
                params.leftMargin = rotatedBounds.left - parentLeft - getPaddingX();
            }
        }
        params.height = rotatedBounds.height() + 2 * getPaddingX();
        params.width = rotatedBounds.width() + 2 * getPaddingY();
        setLayoutParams(params);
    }

    private void onFingerDown() {
        mFingerprintDrawable.setShouldSkipDraw(true);
        mFingerprintDrawable.invalidateSelf();
    }

    private void onFingerUp() {
        mFingerprintDrawable.setShouldSkipDraw(false);
        mFingerprintDrawable.invalidateSelf();
    }

    private int getPaddingX() {
        return mProgressBarRadius;
    }

    private int getPaddingY() {
        return mProgressBarRadius;
    }

    private void updateOverlayParams() {

        if (mSensorProperties == null) {
            android.util.Log.e(TAG, "There is no sensor info!");
            return;
        }

        DisplayInfo displayInfo = new DisplayInfo();
        if (getDisplay() == null) {
            android.util.Log.e(TAG, "Can not get display");
            return;
        }
        getDisplay().getDisplayInfo(displayInfo);
        Rect udfpsBounds = mSensorProperties.getLocation().getRect();
        float scaleFactor = mUdfpsUtils.getScaleFactor(displayInfo);
        udfpsBounds.scale(scaleFactor);

        final Rect overlayBounds = new Rect(
                0, /* left */
                displayInfo.getNaturalHeight() / 2, /* top */
                displayInfo.getNaturalWidth(), /* right */
                displayInfo.getNaturalHeight() /* botom */);

        mOverlayParams = new UdfpsOverlayParams(
                udfpsBounds,
                overlayBounds,
                displayInfo.getNaturalWidth(),
                displayInfo.getNaturalHeight(),
                scaleFactor,
                displayInfo.rotation);


        post(() -> {
            mProgressBarRadius =
                    (int) (mOverlayParams.getScaleFactor() * getContext().getResources().getInteger(
                            R.integer.config_udfpsEnrollProgressBar));
            mSensorRect = new Rect(mOverlayParams.getSensorBounds());

            onSensorRectUpdated();
        });

    }
}

