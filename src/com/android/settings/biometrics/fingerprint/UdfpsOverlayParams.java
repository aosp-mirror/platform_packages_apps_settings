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

import android.graphics.Rect;

import androidx.annotation.NonNull;

/**
 * Collection of parameters that define an under-display fingerprint sensor (UDFPS) overlay.
 *
 * [sensorBounds] coordinates of the bounding box around the sensor in natural orientation, in
 * pixels, for the current resolution.
 *
 * [overlayBounds] coordinates of the UI overlay in natural orientation, in pixels, for the current
 * resolution.
 *
 * [naturalDisplayWidth] width of the physical display in natural orientation, in pixels, for the
 * current resolution.
 *
 * [naturalDisplayHeight] height of the physical display in natural orientation, in pixels, for the
 * current resolution.
 *
 * [scaleFactor] ratio of a dimension in the current resolution to the corresponding dimension in
 * the native resolution.
 *
 * [rotation] current rotation of the display.
 */
public final class UdfpsOverlayParams {
    @NonNull
    private final Rect mSensorBounds;
    @NonNull
    private final Rect mOverlayBounds;
    private final int mNaturalDisplayWidth;
    private final int mNaturalDisplayHeight;
    private final float mScaleFactor;
    private final int mRotation;
    private final boolean mIsOptical;

    public UdfpsOverlayParams(@NonNull Rect sensorBounds, @NonNull Rect overlayBounds,
            int naturalDisplayWidth, int naturalDisplayHeight, float scaleFactor, int rotation,
            boolean isOptical) {
        mSensorBounds = sensorBounds;
        mOverlayBounds = overlayBounds;
        mNaturalDisplayWidth = naturalDisplayWidth;
        mNaturalDisplayHeight = naturalDisplayHeight;
        mScaleFactor = scaleFactor;
        mRotation = rotation;
        mIsOptical = isOptical;
    }

    @NonNull
    public Rect getSensorBounds() {
        return mSensorBounds;
    }

    @NonNull
    public Rect getOverlayBounds() {
        return mOverlayBounds;
    }

    public int getNaturalDisplayWidth() {
        return mNaturalDisplayWidth;
    }

    public int getNaturalDisplayHeight() {
        return mNaturalDisplayHeight;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public int getRotation() {
        return mRotation;
    }

    public boolean isOptical() {
        return mIsOptical;
    }
}
