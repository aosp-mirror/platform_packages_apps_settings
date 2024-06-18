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
import android.graphics.PointF;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.core.InstrumentedFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps keep track of enrollment state and animates the progress bar accordingly.
 */
public class UdfpsEnrollHelper extends InstrumentedFragment {
    private static final String TAG = "UdfpsEnrollHelper";

    private static final String SCALE_OVERRIDE =
            "com.android.systemui.biometrics.UdfpsEnrollHelper.scale";
    private static final float SCALE = 0.5f;

    private static final String NEW_COORDS_OVERRIDE =
            "com.android.systemui.biometrics.UdfpsNewCoords";

    interface Listener {
        void onEnrollmentProgress(int remaining, int totalSteps);

        void onEnrollmentHelp(int remaining, int totalSteps);

        void onAcquired(boolean animateIfLastStepGood);

        void onPointerDown(int sensorId);

        void onPointerUp(int sensorId);
    }

    @NonNull
    private final Context mContext;
    @NonNull
    private final FingerprintManager mFingerprintManager;
    private final boolean mAccessibilityEnabled;
    @NonNull
    private final List<PointF> mGuidedEnrollmentPoints;

    private int mTotalSteps = -1;
    private int mRemainingSteps = -1;

    // Note that this is actually not equal to "mTotalSteps - mRemainingSteps", because the
    // interface makes no promises about monotonically increasing by one each time.
    private int mLocationsEnrolled = 0;

    private int mCenterTouchCount = 0;

    private int mPace = 1;

    @Nullable
    UdfpsEnrollHelper.Listener mListener;

    public UdfpsEnrollHelper(@NonNull Context context,
            @NonNull FingerprintManager fingerprintManager) {

        mContext = context;
        mFingerprintManager = fingerprintManager;

        final AccessibilityManager am = context.getSystemService(AccessibilityManager.class);
        mAccessibilityEnabled = am.isEnabled();

        mGuidedEnrollmentPoints = new ArrayList<>();

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

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * Called when a enroll progress update
     */
    public void onEnrollmentProgress(int totalSteps, int remaining) {
        if (mTotalSteps == -1) {
            mTotalSteps = totalSteps;
        }

        if (remaining != mRemainingSteps) {
            mLocationsEnrolled++;
            if (isCenterEnrollmentStage()) {
                mCenterTouchCount++;
            }
        }

        if (mRemainingSteps > remaining) {
            mPace = mRemainingSteps - remaining;
        }
        mRemainingSteps = remaining;

        if (mListener != null && mTotalSteps != -1) {
            mListener.onEnrollmentProgress(remaining, mTotalSteps);
        }
    }

    /**
     * Called when a receive error has been encountered during enrollment.
     */
    public void onEnrollmentHelp() {
        if (mListener != null) {
            mListener.onEnrollmentHelp(mRemainingSteps, mTotalSteps);
        }
    }

    /**
     * Called when a fingerprint image has been acquired, but wasn't processed yet.
     */
    public void onAcquired(boolean isAcquiredGood) {
        if (mListener != null) {
            mListener.onAcquired(isAcquiredGood && animateIfLastStep());
        }
    }

    /**
     * Called when pointer down
     */
    public void onPointerDown(int sensorId) {
        if (mListener != null) {
            mListener.onPointerDown(sensorId);
        }
    }

    /**
     * Called when pointer up
     */
    public void onPointerUp(int sensorId) {
        if (mListener != null) {
            mListener.onPointerUp(sensorId);
        }
    }

    void setListener(UdfpsEnrollHelper.Listener listener) {
        mListener = listener;

        // Only notify during setListener if enrollment is already in progress, so the progress
        // bar can be updated. If enrollment has not started yet, the progress bar will be empty
        // anyway.
        if (mListener != null && mTotalSteps != -1) {
            mListener.onEnrollmentProgress(mRemainingSteps, mTotalSteps);
        }
    }

    boolean isCenterEnrollmentStage() {
        if (mTotalSteps == -1 || mRemainingSteps == -1) {
            return true;
        }
        return mTotalSteps - mRemainingSteps < getStageThresholdSteps(mTotalSteps, 0);
    }

    boolean isTipEnrollmentStage() {
        if (mTotalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        final int progressSteps = mTotalSteps - mRemainingSteps;
        return progressSteps >= getStageThresholdSteps(mTotalSteps, 1)
                && progressSteps < getStageThresholdSteps(mTotalSteps, 2);
    }

    boolean isEdgeEnrollmentStage() {
        if (mTotalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        return mTotalSteps - mRemainingSteps >= getStageThresholdSteps(mTotalSteps, 2);
    }

    @NonNull
    PointF getNextGuidedEnrollmentPoint() {
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

    boolean animateIfLastStep() {
        if (mListener == null) {
            Log.e(TAG, "animateIfLastStep, null listener");
            return false;
        }

        return mRemainingSteps <= mPace && mRemainingSteps >= 0;
    }

    private int getStageThresholdSteps(int totalSteps, int stageIndex) {
        return Math.round(totalSteps * mFingerprintManager.getEnrollStageThreshold(stageIndex));
    }

    private boolean isGuidedEnrollmentStage() {
        if (mAccessibilityEnabled || mTotalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        final int progressSteps = mTotalSteps - mRemainingSteps;
        return progressSteps >= getStageThresholdSteps(mTotalSteps, 0)
                && progressSteps < getStageThresholdSteps(mTotalSteps, 1);
    }
}
