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

import static com.android.settings.DataUsageSummary.formatDateRange;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

/**
 * Background of {@link ChartView} that renders grid lines as requested by
 * {@link ChartAxis#getTickPoints()}.
 */
public class ChartGridView extends View {

    private ChartAxis mHoriz;
    private ChartAxis mVert;

    private Drawable mPrimary;
    private Drawable mSecondary;
    private Drawable mBorder;
    private int mLabelColor;

    private Layout mLayoutStart;
    private Layout mLayoutEnd;

    public ChartGridView(Context context) {
        this(context, null, 0);
    }

    public ChartGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotDraw(false);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ChartGridView, defStyle, 0);

        mPrimary = a.getDrawable(R.styleable.ChartGridView_primaryDrawable);
        mSecondary = a.getDrawable(R.styleable.ChartGridView_secondaryDrawable);
        mBorder = a.getDrawable(R.styleable.ChartGridView_borderDrawable);
        mLabelColor = a.getColor(R.styleable.ChartGridView_labelColor, Color.RED);

        a.recycle();
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");
    }

    void setBounds(long start, long end) {
        final Context context = getContext();
        mLayoutStart = makeLayout(formatDateRange(context, start, start));
        mLayoutEnd = makeLayout(formatDateRange(context, end, end));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();

        final Drawable secondary = mSecondary;
        final int secondaryHeight = mSecondary.getIntrinsicHeight();

        final float[] vertTicks = mVert.getTickPoints();
        for (float y : vertTicks) {
            final int bottom = (int) Math.min(y + secondaryHeight, height);
            secondary.setBounds(0, (int) y, width, bottom);
            secondary.draw(canvas);
        }

        final Drawable primary = mPrimary;
        final int primaryWidth = mPrimary.getIntrinsicWidth();
        final int primaryHeight = mPrimary.getIntrinsicHeight();

        final float[] horizTicks = mHoriz.getTickPoints();
        for (float x : horizTicks) {
            final int right = (int) Math.min(x + primaryWidth, width);
            primary.setBounds((int) x, 0, right, height);
            primary.draw(canvas);
        }

        mBorder.setBounds(0, 0, width, height);
        mBorder.draw(canvas);

        final int padding = mLayoutStart != null ? mLayoutStart.getHeight() / 8 : 0;

        final Layout start = mLayoutStart;
        if (start != null) {
            canvas.save();
            canvas.translate(0, height + padding);
            start.draw(canvas);
            canvas.restore();
        }

        final Layout end = mLayoutEnd;
        if (end != null) {
            canvas.save();
            canvas.translate(width - end.getWidth(), height + padding);
            end.draw(canvas);
            canvas.restore();
        }
    }

    private Layout makeLayout(CharSequence text) {
        final Resources res = getResources();
        final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.density = res.getDisplayMetrics().density;
        paint.setCompatibilityScaling(res.getCompatibilityInfo().applicationScale);
        paint.setColor(mLabelColor);
        paint.setTextSize(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, res.getDisplayMetrics()));

        return new StaticLayout(text, paint,
                (int) Math.ceil(Layout.getDesiredWidth(text, paint)),
                Layout.Alignment.ALIGN_NORMAL, 1.f, 0, true);
    }

}
