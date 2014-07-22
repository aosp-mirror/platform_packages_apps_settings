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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

import java.util.Locale;

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

    private int mLabelSize;
    private int mLabelColor;

    private Layout mLabelStart;
    private Layout mLabelMid;
    private Layout mLabelEnd;

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

        final int taId = a.getResourceId(R.styleable.ChartGridView_android_textAppearance, -1);
        final TypedArray ta = context.obtainStyledAttributes(taId,
                com.android.internal.R.styleable.TextAppearance);
        mLabelSize = ta.getDimensionPixelSize(
                com.android.internal.R.styleable.TextAppearance_textSize, 0);
        ta.recycle();

        final ColorStateList labelColor = a.getColorStateList(
                R.styleable.ChartGridView_android_textColor);
        mLabelColor = labelColor.getDefaultColor();

        a.recycle();
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");
    }

    void setBounds(long start, long end) {
        final Context context = getContext();
        final long mid = (start + end) / 2;
        mLabelStart = makeLabel(formatDateRange(context, start, start));
        mLabelMid = makeLabel(formatDateRange(context, mid, mid));
        mLabelEnd = makeLabel(formatDateRange(context, end, end));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight() - getPaddingBottom();

        final Drawable secondary = mSecondary;
        if (secondary != null) {
            final int secondaryHeight = secondary.getIntrinsicHeight();

            final float[] vertTicks = mVert.getTickPoints();
            for (float y : vertTicks) {
                final int bottom = (int) Math.min(y + secondaryHeight, height);
                secondary.setBounds(0, (int) y, width, bottom);
                secondary.draw(canvas);
            }
        }

        final Drawable primary = mPrimary;
        if (primary != null) {
            final int primaryWidth = primary.getIntrinsicWidth();
            final int primaryHeight = primary.getIntrinsicHeight();

            final float[] horizTicks = mHoriz.getTickPoints();
            for (float x : horizTicks) {
                final int right = (int) Math.min(x + primaryWidth, width);
                primary.setBounds((int) x, 0, right, height);
                primary.draw(canvas);
            }
        }

        mBorder.setBounds(0, 0, width, height);
        mBorder.draw(canvas);

        final int padding = mLabelStart != null ? mLabelStart.getHeight() / 8 : 0;

        final Layout start = mLabelStart;
        if (start != null) {
            final int saveCount = canvas.save();
            canvas.translate(0, height + padding);
            start.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        final Layout mid = mLabelMid;
        if (mid != null) {
            final int saveCount = canvas.save();
            canvas.translate((width - mid.getWidth()) / 2, height + padding);
            mid.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        final Layout end = mLabelEnd;
        if (end != null) {
            final int saveCount = canvas.save();
            canvas.translate(width - end.getWidth(), height + padding);
            end.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    private Layout makeLabel(CharSequence text) {
        final Resources res = getResources();
        final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.density = res.getDisplayMetrics().density;
        paint.setCompatibilityScaling(res.getCompatibilityInfo().applicationScale);
        paint.setColor(mLabelColor);
        paint.setTextSize(mLabelSize);

        return new StaticLayout(text, paint,
                (int) Math.ceil(Layout.getDesiredWidth(text, paint)),
                Layout.Alignment.ALIGN_NORMAL, 1.f, 0, true);
    }
}
