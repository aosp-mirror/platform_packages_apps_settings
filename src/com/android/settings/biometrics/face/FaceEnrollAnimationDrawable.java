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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A drawable containing the circle cutout.
 */
public class FaceEnrollAnimationDrawable extends Drawable {

    private Rect mBounds;
    private final Paint mSquarePaint;
    private final Paint mCircleCutoutPaint;

    public FaceEnrollAnimationDrawable() {
        mSquarePaint = new Paint();
        mSquarePaint.setColor(Color.WHITE);
        mSquarePaint.setAntiAlias(true);

        mCircleCutoutPaint = new Paint();
        mCircleCutoutPaint.setColor(Color.TRANSPARENT);
        mCircleCutoutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCircleCutoutPaint.setAntiAlias(true);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mBounds = bounds;
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
                mBounds.height() / 2, mCircleCutoutPaint);

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
