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

package com.android.settings.biometrics.fingerprint;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.util.AttributeSet;
import android.util.Log;
import android.util.RotationUtils;
import android.view.DisplayInfo;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.systemui.biometrics.UdfpsUtils;
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams;

/**
 * View corresponding with fingerprint_check_enrolled_dialog.xml
 */
public class UdfpsCheckEnrolledView extends RelativeLayout {
    private static final String TAG = "UdfpsCheckEnrolledView";
    @NonNull
    private final UdfpsFingerprintDrawable mFingerprintDrawable;
    private ImageView mFingerprintView;
    private UdfpsUtils mUdfpsUtils;

    private @Nullable Rect mSensorRect;
    private @Nullable UdfpsOverlayParams mOverlayParams;
    private @Nullable FingerprintSensorPropertiesInternal mSensorProperties;


    public UdfpsCheckEnrolledView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mFingerprintDrawable = new UdfpsFingerprintDrawable(mContext, attrs);
        mUdfpsUtils = new UdfpsUtils();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFingerprintView = findViewById(R.id.udfps_fingerprint_sensor_view);
        mFingerprintView.setImageDrawable(mFingerprintDrawable);
    }

    /**
     * setup SensorProperties
     */
    public void setSensorProperties(@Nullable FingerprintSensorPropertiesInternal properties) {
        mSensorProperties = properties;
        updateOverlayParams();
    }

    private void onSensorRectUpdated() {
        updateDimensions();

        if (mSensorRect == null || mOverlayParams == null) {
            Log.e(TAG, "Fail to onSensorRectUpdated, mSensorRect/mOverlayParams null");
            return;
        }

        // Updates sensor rect in relation to the overlay view
        mSensorRect.set(0, 0,
                mOverlayParams.getSensorBounds().width(),
                mOverlayParams.getSensorBounds().height());
        mFingerprintDrawable.onSensorRectUpdated(new RectF(mSensorRect));
    }

    private void updateDimensions() {
        if (mOverlayParams == null) {
            Log.e(TAG, "Fail to updateDimensions for " + this + ", mOverlayParams null");
            return;
        }
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

        // Update container view LayoutParams
        RelativeLayout.LayoutParams checkEnrolledViewLp =
                new RelativeLayout.LayoutParams(getWidth(), getHeight());
        checkEnrolledViewLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        if (rotation == Surface.ROTATION_90) {
            checkEnrolledViewLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            checkEnrolledViewLp.width =
                    rotatedBounds.width() + 2 * (parentRight - rotatedBounds.right);
        } else {
            checkEnrolledViewLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            checkEnrolledViewLp.width = rotatedBounds.width() + 2 * rotatedBounds.left;
        }
        setLayoutParams(checkEnrolledViewLp);

        // Update fingerprint view LayoutParams
        RelativeLayout.LayoutParams fingerprintViewLp = new RelativeLayout.LayoutParams(
                rotatedBounds.width(), rotatedBounds.height());
        fingerprintViewLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        fingerprintViewLp.topMargin = rotatedBounds.top - parentTop;
        if (rotation == Surface.ROTATION_90) {
            fingerprintViewLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            fingerprintViewLp.rightMargin = parentRight - rotatedBounds.right;
        } else {
            fingerprintViewLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            fingerprintViewLp.leftMargin = rotatedBounds.left - parentLeft;
        }
        mFingerprintView.setLayoutParams(fingerprintViewLp);
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
                displayInfo.rotation,
                mSensorProperties.sensorType);

        post(() -> {
            if (mOverlayParams == null) {
                Log.e(TAG, "Fail to updateOverlayParams, mOverlayParams null");
                return;
            }
            mSensorRect = new Rect(mOverlayParams.getSensorBounds());
            onSensorRectUpdated();
        });
    }
}
