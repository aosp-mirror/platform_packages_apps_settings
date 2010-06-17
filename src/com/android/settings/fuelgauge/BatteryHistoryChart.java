/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import com.android.settings.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.BatteryStats;
import android.os.SystemClock;
import android.os.BatteryStats.HistoryItem;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class BatteryHistoryChart extends View {
    static final int SANS = 1;
    static final int SERIF = 2;
    static final int MONOSPACE = 3;

    static final int BATTERY_WARN = 29;
    static final int BATTERY_CRITICAL = 14;
    
    final Paint mBatteryBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mBatteryGoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mBatteryWarnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mBatteryCriticalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    
    int mFontSize;
    
    BatteryStats mStats;
    long mStatsPeriod;
    String mDurationString;
    
    int mTextAscent;
    int mTextDescent;
    int mDurationStringWidth;
    
    int mNumHist;
    long mHistStart;
    long mHistEnd;
    int mBatLow;
    int mBatHigh;
    
    final Path mBatLevelPath = new Path();
    int[] mBatLevelX;
    int[] mBatLevelY;
    byte[] mBatLevelValue;
    int mNumBatLevel;
    
    public BatteryHistoryChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mBatteryBackgroundPaint.setARGB(255, 128, 128, 128);
        mBatteryBackgroundPaint.setStyle(Paint.Style.FILL);
        mBatteryGoodPaint.setARGB(128, 0, 255, 0);
        int lineWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2, getResources().getDisplayMetrics());
        if (lineWidth <= 0) lineWidth = 1;
        mBatteryGoodPaint.setStrokeWidth(lineWidth);
        mBatteryWarnPaint.setARGB(128, 255, 255, 0);
        mBatteryWarnPaint.setStrokeWidth(lineWidth);
        mBatteryCriticalPaint.setARGB(192, 255, 0, 0);
        mBatteryCriticalPaint.setStrokeWidth(lineWidth);
        
        mTextPaint.density = getResources().getDisplayMetrics().density;
        mTextPaint.setCompatibilityScaling(
                getResources().getCompatibilityInfo().applicationScale);
        
        TypedArray a =
            context.obtainStyledAttributes(
                attrs, R.styleable.BatteryHistoryChart, 0, 0);
        
        ColorStateList textColor = null;
        int textSize = 15;
        int typefaceIndex = -1;
        int styleIndex = -1;
        
        TypedArray appearance = null;
        int ap = a.getResourceId(R.styleable.BatteryHistoryChart_android_textAppearance, -1);
        if (ap != -1) {
            appearance = context.obtainStyledAttributes(ap,
                                com.android.internal.R.styleable.
                                TextAppearance);
        }
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);

                switch (attr) {
                case com.android.internal.R.styleable.TextAppearance_textColor:
                    textColor = appearance.getColorStateList(attr);
                    break;

                case com.android.internal.R.styleable.TextAppearance_textSize:
                    textSize = appearance.getDimensionPixelSize(attr, textSize);
                    break;

                case com.android.internal.R.styleable.TextAppearance_typeface:
                    typefaceIndex = appearance.getInt(attr, -1);
                    break;

                case com.android.internal.R.styleable.TextAppearance_textStyle:
                    styleIndex = appearance.getInt(attr, -1);
                    break;
                }
            }

            appearance.recycle();
        }
        
        int shadowcolor = 0;
        float dx=0, dy=0, r=0;
        
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);

            switch (attr) {
                case R.styleable.BatteryHistoryChart_android_shadowColor:
                    shadowcolor = a.getInt(attr, 0);
                    break;

                case R.styleable.BatteryHistoryChart_android_shadowDx:
                    dx = a.getFloat(attr, 0);
                    break;

                case R.styleable.BatteryHistoryChart_android_shadowDy:
                    dy = a.getFloat(attr, 0);
                    break;

                case R.styleable.BatteryHistoryChart_android_shadowRadius:
                    r = a.getFloat(attr, 0);
                    break;

                case R.styleable.BatteryHistoryChart_android_textColor:
                    textColor = a.getColorStateList(attr);
                    break;

                case R.styleable.BatteryHistoryChart_android_textSize:
                    textSize = a.getDimensionPixelSize(attr, textSize);
                    break;

                case R.styleable.BatteryHistoryChart_android_typeface:
                    typefaceIndex = a.getInt(attr, typefaceIndex);
                    break;

                case R.styleable.BatteryHistoryChart_android_textStyle:
                    styleIndex = a.getInt(attr, styleIndex);
                    break;
            }
        }
        
        mTextPaint.setColor(textColor.getDefaultColor());
        mTextPaint.setTextSize(textSize);
        
        Typeface tf = null;
        switch (typefaceIndex) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;

            case SERIF:
                tf = Typeface.SERIF;
                break;

            case MONOSPACE:
                tf = Typeface.MONOSPACE;
                break;
        }
        
        setTypeface(tf, styleIndex);
        
        if (shadowcolor != 0) {
            mTextPaint.setShadowLayer(r, dx, dy, shadowcolor);
        }
    }
    
    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            mTextPaint.setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            mTextPaint.setTypeface(tf);
        }
    }
    
    void setStats(BatteryStats stats) {
        mStats = stats;
        
        long uSecTime = mStats.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000,
                BatteryStats.STATS_SINCE_CHARGED);
        mStatsPeriod = uSecTime;
        String durationString = Utils.formatElapsedTime(getContext(), mStatsPeriod / 1000);
        mDurationString = getContext().getString(R.string.battery_stats_on_battery,
                durationString);
        
        BatteryStats.HistoryItem rec = stats.getHistory();
        if (rec != null) {
            mHistStart = rec.time;
            mBatLow = mBatHigh = rec.batteryLevel;
        }
        int pos = 0;
        int lastInteresting = 0;
        byte lastLevel = -1;
        mBatLow = 0;
        mBatHigh = 100;
        while (rec != null) {
            pos++;
            if (rec.cmd == HistoryItem.CMD_UPDATE && (rec.batteryLevel != lastLevel
                    || pos == 1)) {
                lastLevel = rec.batteryLevel;
                lastInteresting = pos;
                mHistEnd = rec.time;
            }
            rec = rec.next;
        }
        mNumHist = lastInteresting;
        
        if (mHistEnd <= mHistStart) mHistEnd = mHistStart+1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mDurationStringWidth = (int)mTextPaint.measureText(mDurationString);
        mTextAscent = (int)mTextPaint.ascent();
        mTextDescent = (int)mTextPaint.descent();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        mBatLevelPath.reset();
        mBatLevelX = new int[w+5];
        mBatLevelY = new int[w+5];
        mBatLevelValue = new byte[w+5];
        mNumBatLevel = 0;
        
        final long timeStart = mHistStart;
        final long timeChange = mHistEnd-mHistStart;
        
        final int batLow = mBatLow;
        final int batChange = mBatHigh-mBatLow;
        
        BatteryStats.HistoryItem rec = mStats.getHistory();
        int x = 0, y = 0, lastX = -1, lastY = -1, lastBatX = -1, lastBatY = -1;
        byte lastBatValue = 0;
        int i = 0, num = 0;
        boolean first = true;
        final int N = mNumHist;
        while (rec != null && i < N) {
            if (rec.cmd == BatteryStats.HistoryItem.CMD_UPDATE) {
                x = (int)(((rec.time-timeStart)*w)/timeChange);
                y = h-1 - ((rec.batteryLevel-batLow)*(h-3))/batChange;
                if (first) {
                    first = false;
                    mBatLevelPath.moveTo(x, y);
                    mBatLevelX[mNumBatLevel] = x;
                    mBatLevelY[mNumBatLevel] = y;
                    mBatLevelValue[mNumBatLevel] = lastBatValue = rec.batteryLevel;
                    mNumBatLevel++;
                    lastX = lastBatX = x;
                    lastY = lastBatY = y;
                } else {
                    if (lastX != x) {
                        // We have moved by at least a pixel.
                        if (lastY == y) {
                            // Battery level is still the same; don't plot,
                            // but remember it.
                            lastBatX = x;
                            lastBatY = y;
                        } else {
                            if (lastBatX >= 0) {
                                // Level stayed the same up to here; put in line.
                                mBatLevelPath.lineTo(lastBatX, lastBatY);
                                mBatLevelX[mNumBatLevel] = lastBatX;
                                mBatLevelY[mNumBatLevel] = lastBatY;
                                mBatLevelValue[mNumBatLevel] = lastBatValue;
                                mNumBatLevel++;
                                num++;
                            }
                            mBatLevelPath.lineTo(x, y);
                            mBatLevelX[mNumBatLevel] = x;
                            mBatLevelY[mNumBatLevel] = y;
                            mBatLevelValue[mNumBatLevel] = lastBatValue = rec.batteryLevel;
                            mNumBatLevel++;
                            num++;
                            lastX = x;
                            lastY = y;
                            lastBatX = -1;
                        }
                    }
                }
            }
            rec = rec.next;
            i++;
        }
        
        if (num == 0 || lastBatX >= 0) {
            mBatLevelPath.lineTo(w, y);
            mBatLevelX[mNumBatLevel] = w;
            mBatLevelY[mNumBatLevel] = y;
            mBatLevelValue[mNumBatLevel] = lastBatValue;
            mNumBatLevel++;
        }
        mBatLevelPath.lineTo(w, h);
        mBatLevelPath.lineTo(0, h);
        mBatLevelPath.close();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        final int width = getWidth();
        final int height = getHeight();
        
        canvas.drawPath(mBatLevelPath, mBatteryBackgroundPaint);
        canvas.drawText(mDurationString, (width/2) - (mDurationStringWidth/2),
                (height/2) - ((mTextDescent-mTextAscent)/2) - mTextAscent, mTextPaint);
        
        int lastX = mBatLevelX[0];
        int lastY = mBatLevelY[0];
        for (int i=1; i<mNumBatLevel; i++) {
            int x = mBatLevelX[i];
            int y = mBatLevelY[i];
            Paint paint;
            byte value = mBatLevelValue[i];
            if (value <= BATTERY_CRITICAL) paint = mBatteryCriticalPaint;
            else if (value <= BATTERY_WARN) paint = mBatteryWarnPaint;
            else paint = mBatteryGoodPaint;
            canvas.drawLine(lastX, lastY, x, y, paint);
            lastX = x;
            lastY = y;
        }
    }
}
