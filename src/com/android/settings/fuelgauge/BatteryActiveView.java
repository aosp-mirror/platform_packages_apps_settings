/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;

public class BatteryActiveView extends View {

    private final Paint mPaint = new Paint();
    private BatteryActiveProvider mProvider;

    public BatteryActiveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setProvider(BatteryActiveProvider provider) {
        mProvider = provider;
        if (getWidth() != 0) {
            postInvalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (getWidth() != 0) {
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mProvider == null) {
            return;
        }
        SparseIntArray array = mProvider.getColorArray();
        float period = mProvider.getPeriod();
        for (int i = 0; i < array.size() - 1; i++) {
            drawColor(canvas, array.keyAt(i), array.keyAt(i + 1), array.valueAt(i), period);
        }
    }

    private void drawColor(Canvas canvas, int start, int end, int color, float period) {
        if (color == 0) {
            return;
        }
        mPaint.setColor(color);
        canvas.drawRect(
                start / period * getWidth(), 0, end / period * getWidth(), getHeight(), mPaint);
    }

    public interface BatteryActiveProvider {
        boolean hasData();

        long getPeriod();

        SparseIntArray getColorArray();
    }
}
