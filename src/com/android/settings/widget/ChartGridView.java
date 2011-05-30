/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

import com.google.common.base.Preconditions;

/**
 * Background of {@link ChartView} that renders grid lines as requested by
 * {@link ChartAxis#getTickPoints()}.
 */
public class ChartGridView extends View {

    private final ChartAxis mHoriz;
    private final ChartAxis mVert;

    private final Paint mPaintHoriz;
    private final Paint mPaintVert;

    public ChartGridView(Context context, ChartAxis horiz, ChartAxis vert) {
        super(context);

        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");

        setWillNotDraw(false);

        // TODO: convert these colors to resources
        mPaintHoriz = new Paint();
        mPaintHoriz.setColor(Color.parseColor("#667bb5"));
        mPaintHoriz.setStrokeWidth(2.0f);
        mPaintHoriz.setStyle(Style.STROKE);
        mPaintHoriz.setAntiAlias(true);

        mPaintVert = new Paint();
        mPaintVert.setColor(Color.parseColor("#28262c"));
        mPaintVert.setStrokeWidth(1.0f);
        mPaintVert.setStyle(Style.STROKE);
        mPaintVert.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();

        final float[] vertTicks = mVert.getTickPoints();
        for (float y : vertTicks) {
            canvas.drawLine(0, y, width, y, mPaintVert);
        }

        final float[] horizTicks = mHoriz.getTickPoints();
        for (float x : horizTicks) {
            canvas.drawLine(x, 0, x, height, mPaintHoriz);
        }

        canvas.drawRect(0, 0, width, height, mPaintHoriz);
    }
}
