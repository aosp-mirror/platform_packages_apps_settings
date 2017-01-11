/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * DonutView represents a donut graph. It visualizes a certain percentage of fullness with a
 * corresponding label with the fullness on the inside (i.e. "50%" inside of the donut.
 */
public class DonutView extends View {
    private static final int TOP = -90;
    private float mStrokeWidth;
    private int mPercent;
    private Paint mBackgroundCircle;
    private Paint mFilledArc;
    private TextPaint mTextPaint;

    public DonutView(Context context) {
        super(context);
    }

    public DonutView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        mStrokeWidth = 10f * density;

        mBackgroundCircle = new Paint();
        mBackgroundCircle.setAntiAlias(true);
        mBackgroundCircle.setStrokeCap(Paint.Cap.BUTT);
        mBackgroundCircle.setStyle(Paint.Style.STROKE);
        mBackgroundCircle.setStrokeWidth(mStrokeWidth);
        mBackgroundCircle.setColor(getResources().getColor(R.color.donut_background_grey));

        mFilledArc = new Paint();
        mFilledArc.setAntiAlias(true);
        mFilledArc.setStrokeCap(Paint.Cap.BUTT);
        mFilledArc.setStyle(Paint.Style.STROKE);
        mFilledArc.setStrokeWidth(mStrokeWidth);
        mFilledArc.setColor(Utils.getColorAccent(getContext()));

        mTextPaint = new TextPaint();
        mTextPaint.setColor(Utils.getColorAccent(getContext()));
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(18f * density);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(0 + mStrokeWidth, 0 + mStrokeWidth, getWidth() - mStrokeWidth,
                getHeight() - mStrokeWidth, TOP, 360, false, mBackgroundCircle);

        canvas.drawArc(0 + mStrokeWidth, 0 + mStrokeWidth, getWidth() - mStrokeWidth,
                getHeight() - mStrokeWidth, TOP, (360 * mPercent / 100), false, mFilledArc);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        String percentString =
                String.format(getContext().getString(R.string.storage_percent_used), mPercent);
        // drawText uses the Y dimension as the floor of the text, so we do this to center.
        canvas.drawText(percentString, centerX,
                centerY + getTextHeight(mTextPaint) / 2 - mTextPaint.descent(),
                mTextPaint);
    }

    /**
     * Set a percentage full to have the donut graph.
     */
    public void setPercentage(int percent) {
        mPercent = percent;
        invalidate();
    }

    private float getTextHeight(TextPaint paint) {
        // Technically, this should be the cap height, but I can live with the descent - ascent.
        return paint.descent() - paint.ascent();
    }
}
