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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.PathParser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

/**
 * UDFPS fingerprint drawable
 */
public class UdfpsFingerprintDrawable extends Drawable {
    private static final String TAG = "UdfpsFingerprintDrawable";

    private static final float DEFAULT_STROKE_WIDTH = 3f;

    @NonNull
    private final Paint mSensorOutlinePaint;
    @NonNull
    private final ShapeDrawable mFingerprintDrawable;
    private int mAlpha;

    @Nullable
    private RectF mSensorRect;
    private int mEnrollIcon;
    private int mOutlineColor;

    UdfpsFingerprintDrawable(@NonNull Context context, @Nullable AttributeSet attrs) {
        mFingerprintDrawable = defaultFactory(context);

        loadResources(context, attrs);
        mSensorOutlinePaint = new Paint(0 /* flags */);
        mSensorOutlinePaint.setAntiAlias(true);
        mSensorOutlinePaint.setColor(mOutlineColor);
        mSensorOutlinePaint.setStyle(Paint.Style.FILL);

        mFingerprintDrawable.setTint(mEnrollIcon);

        setAlpha(255);
    }

    /** The [sensorRect] coordinates for the sensor area. */
    void onSensorRectUpdated(@NonNull RectF sensorRect) {
        int margin = ((int) sensorRect.height()) / 8;
        Rect bounds = new Rect((int) (sensorRect.left) + margin, (int) (sensorRect.top) + margin,
                (int) (sensorRect.right) - margin, (int) (sensorRect.bottom) - margin);
        updateFingerprintIconBounds(bounds);
        mSensorRect = sensorRect;
    }

    void updateFingerprintIconBounds(@NonNull Rect bounds) {
        mFingerprintDrawable.setBounds(bounds);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mSensorRect != null) {
            canvas.drawOval(mSensorRect, mSensorOutlinePaint);
        }
        mFingerprintDrawable.draw(canvas);
        mFingerprintDrawable.setAlpha(getAlpha());
        mSensorOutlinePaint.setAlpha(getAlpha());
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mFingerprintDrawable.setAlpha(alpha);
        mSensorOutlinePaint.setAlpha(alpha);
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
        return PixelFormat.OPAQUE;
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
        mOutlineColor = ta.getColor(
                R.styleable.BiometricsEnrollView_biometricsMovingTargetFill, 0);
        ta.recycle();
    }
}
