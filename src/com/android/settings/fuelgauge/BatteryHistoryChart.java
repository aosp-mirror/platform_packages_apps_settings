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
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.SystemClock;
import android.os.BatteryStats.HistoryItem;
import android.telephony.ServiceState;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class BatteryHistoryChart extends View {
    static final int CHART_DATA_X_MASK = 0x0000ffff;
    static final int CHART_DATA_BIN_MASK = 0xffff0000;
    static final int CHART_DATA_BIN_SHIFT = 16;

    static class ChartData {
        int[] mColors;
        Paint[] mPaints;

        int mNumTicks;
        int[] mTicks;
        int mLastBin;

        void setColors(int[] colors) {
            mColors = colors;
            mPaints = new Paint[colors.length];
            for (int i=0; i<colors.length; i++) {
                mPaints[i] = new Paint();
                mPaints[i].setColor(colors[i]);
                mPaints[i].setStyle(Paint.Style.FILL);
            }
        }

        void init(int width) {
            if (width > 0) {
                mTicks = new int[width*2];
            } else {
                mTicks = null;
            }
            mNumTicks = 0;
            mLastBin = 0;
        }

        void addTick(int x, int bin) {
            if (bin != mLastBin && mNumTicks < mTicks.length) {
                mTicks[mNumTicks] = x | bin << CHART_DATA_BIN_SHIFT;
                mNumTicks++;
                mLastBin = bin;
            }
        }

        void finish(int width) {
            if (mLastBin != 0) {
                addTick(width, 0);
            }
        }

        void draw(Canvas canvas, int top, int height) {
            int lastBin=0, lastX=0;
            int bottom = top + height;
            for (int i=0; i<mNumTicks; i++) {
                int tick = mTicks[i];
                int x = tick&CHART_DATA_X_MASK;
                int bin = (tick&CHART_DATA_BIN_MASK) >> CHART_DATA_BIN_SHIFT;
                if (lastBin != 0) {
                    canvas.drawRect(lastX, top, x, bottom, mPaints[lastBin]);
                }
                lastBin = bin;
                lastX = x;
            }

        }
    }

    static final int SANS = 1;
    static final int SERIF = 2;
    static final int MONOSPACE = 3;

    static final int BATTERY_WARN = 29;
    static final int BATTERY_CRITICAL = 14;
    
    // First value if for phone off; first value is "scanning"; following values
    // are battery stats signal strength buckets.
    static final int NUM_PHONE_SIGNALS = 7;

    final Paint mBatteryBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mBatteryGoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mBatteryWarnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mBatteryCriticalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mDockBatteryGoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mDockBatteryWarnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mDockBatteryCriticalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mChargingPaint = new Paint();
    final Paint mScreenOnPaint = new Paint();
    final Paint mGpsOnPaint = new Paint();
    final Paint mWifiRunningPaint = new Paint();
    final Paint mWakeLockPaint = new Paint();
    final ChartData mPhoneSignalChart = new ChartData();
    final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    
    final Path mBatLevelPath = new Path();
    final Path mBatGoodPath = new Path();
    final Path mBatWarnPath = new Path();
    final Path mBatCriticalPath = new Path();
    final Path mDockBatGoodPath = new Path();
    final Path mDockBatWarnPath = new Path();
    final Path mDockBatCriticalPath = new Path();
    final Path mChargingPath = new Path();
    final Path mScreenOnPath = new Path();
    final Path mGpsOnPath = new Path();
    final Path mWifiRunningPath = new Path();
    final Path mWakeLockPath = new Path();
    
    int mFontSize;
    
    BatteryStats mStats;
    BatteryStats mDockStats;
    long mStatsPeriod;
    String mDurationString;
    String mTotalDurationString;
    String mChargingLabel;
    String mScreenOnLabel;
    String mGpsOnLabel;
    String mWifiRunningLabel;
    String mWakeLockLabel;
    String mPhoneSignalLabel;
    
    int mTextAscent;
    int mTextDescent;
    int mDurationStringWidth;
    int mTotalDurationStringWidth;

    boolean mLargeMode;

    int mLineWidth;
    int mThinLineWidth;
    int mChargingOffset;
    int mScreenOnOffset;
    int mGpsOnOffset;
    int mWifiRunningOffset;
    int mWakeLockOffset;
    int mPhoneSignalOffset;
    int mLevelOffset;
    int mLevelTop;
    int mLevelBottom;
    static final int PHONE_SIGNAL_X_MASK = CHART_DATA_X_MASK;
    static final int PHONE_SIGNAL_BIN_MASK = CHART_DATA_BIN_MASK;
    static final int PHONE_SIGNAL_BIN_SHIFT = CHART_DATA_BIN_SHIFT;

    int mNumHist;
    int mDockNumHist;
    long mHistStart;
    long mHistEnd;
    long mDockHistStart;
    long mDockHistEnd;
    int mBatLow;
    int mBatHigh;
    boolean mHaveWifi;
    boolean mHaveGps;
    boolean mHavePhoneSignal;
    
    private final boolean mDockBatterySupported;

    public BatteryHistoryChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        BatteryManager mBatteryService = (BatteryManager) context.getSystemService(
                Context.BATTERY_SERVICE);
        mDockBatterySupported = mBatteryService.isDockBatterySupported();

        mBatteryBackgroundPaint.setARGB(255, 128, 128, 128);
        mBatteryBackgroundPaint.setStyle(Paint.Style.FILL);
        mBatteryGoodPaint.setARGB(128, 0, 255, 0);
        mBatteryGoodPaint.setStyle(Paint.Style.STROKE);
        mBatteryWarnPaint.setARGB(128, 255, 255, 0);
        mBatteryWarnPaint.setStyle(Paint.Style.STROKE);
        mBatteryCriticalPaint.setARGB(192, 255, 0, 0);
        mBatteryCriticalPaint.setStyle(Paint.Style.STROKE);
        if (mDockBatterySupported) {
            mDockBatteryGoodPaint = new Paint(mBatteryGoodPaint);
            mDockBatteryGoodPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
            mDockBatteryWarnPaint = new Paint(mBatteryWarnPaint);
            mDockBatteryWarnPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
            mDockBatteryCriticalPaint = new Paint(mBatteryCriticalPaint);
            mDockBatteryCriticalPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
        }

        mChargingPaint.setARGB(255, 0, 128, 0);
        mChargingPaint.setStyle(Paint.Style.STROKE);
        mScreenOnPaint.setStyle(Paint.Style.STROKE);
        mGpsOnPaint.setStyle(Paint.Style.STROKE);
        mWifiRunningPaint.setStyle(Paint.Style.STROKE);
        mWakeLockPaint.setStyle(Paint.Style.STROKE);
        mPhoneSignalChart.setColors(new int[] {
                0x00000000, 0xffa00000, 0xffa0a000, 0xff808020,
                0xff808040, 0xff808060, 0xff008000
        });
        
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
        
        a.recycle();
        
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
        String durationString = Utils.formatElapsedTime(getContext(), mStatsPeriod / 1000, true);
        mDurationString = getContext().getString(R.string.battery_stats_on_battery,
                durationString);
        mChargingLabel = getContext().getString(R.string.battery_stats_charging_label);
        mScreenOnLabel = getContext().getString(R.string.battery_stats_screen_on_label);
        mGpsOnLabel = getContext().getString(R.string.battery_stats_gps_on_label);
        mWifiRunningLabel = getContext().getString(R.string.battery_stats_wifi_running_label);
        mWakeLockLabel = getContext().getString(R.string.battery_stats_wake_lock_label);
        mPhoneSignalLabel = getContext().getString(R.string.battery_stats_phone_signal_label);
        
        int pos = 0;
        int lastInteresting = 0;
        byte lastLevel = -1;
        mBatLow = 0;
        mBatHigh = 100;
        int aggrStates = 0;
        boolean first = true;
        if (stats.startIteratingHistoryLocked()) {
            final HistoryItem rec = new HistoryItem();
            while (stats.getNextHistoryLocked(rec)) {
                pos++;
                if (rec.cmd == HistoryItem.CMD_UPDATE) {
                    if (first) {
                        first = false;
                        mHistStart = rec.time;
                    }
                    if (rec.batteryLevel != lastLevel || pos == 1) {
                        lastLevel = rec.batteryLevel;
                    }
                    lastInteresting = pos;
                    mHistEnd = rec.time;
                    aggrStates |= rec.states;
                }
            }
        }
        mNumHist = lastInteresting;
        mHaveGps = (aggrStates&HistoryItem.STATE_GPS_ON_FLAG) != 0;
        mHaveWifi = (aggrStates&HistoryItem.STATE_WIFI_RUNNING_FLAG) != 0;
        if (!com.android.settings.Utils.isWifiOnly(getContext())) {
            mHavePhoneSignal = true;
        }
        if (mHistEnd <= mHistStart) mHistEnd = mHistStart+1;
        mTotalDurationString = Utils.formatElapsedTime(getContext(), mHistEnd - mHistStart, true);
    }

    void setDockStats(BatteryStats stats) {
        if (mDockBatterySupported) {
            mDockStats = stats;

            int pos = 0;
            int lastInteresting = 0;
            byte lastLevel = -1;
            boolean first = true;
            if (stats.startIteratingHistoryLocked()) {
                final HistoryItem rec = new HistoryItem();
                while (stats.getNextHistoryLocked(rec)) {
                    pos++;
                    if (rec.cmd == HistoryItem.CMD_UPDATE) {
                        if (first) {
                            first = false;
                            mDockHistStart = rec.time;
                        }
                        if (rec.batteryLevel != lastLevel || pos == 1) {
                            lastLevel = rec.batteryLevel;
                        }
                        lastInteresting = pos;
                        mDockHistEnd = rec.time;
                    }
                }
            }
            mDockNumHist = lastInteresting;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mDurationStringWidth = (int)mTextPaint.measureText(mDurationString);
        mTotalDurationStringWidth = (int)mTextPaint.measureText(mTotalDurationString);
        mTextAscent = (int)mTextPaint.ascent();
        mTextDescent = (int)mTextPaint.descent();
    }

    void finishPaths(int w, int h, int levelh, int startX, int y, Path curLevelPath,
            int lastX, boolean lastCharging, boolean lastScreenOn, boolean lastGpsOn,
            boolean lastWifiRunning, boolean lastWakeLock, Path lastPath) {
        if (curLevelPath != null) {
            if (lastX >= 0 && lastX < w) {
                if (lastPath != null) {
                    lastPath.lineTo(w, y);
                }
                curLevelPath.lineTo(w, y);
            }
            curLevelPath.lineTo(w, mLevelTop+levelh);
            curLevelPath.lineTo(startX, mLevelTop+levelh);
            curLevelPath.close();
        }
        
        if (lastCharging) {
            mChargingPath.lineTo(w, h-mChargingOffset);
        }
        if (lastScreenOn) {
            mScreenOnPath.lineTo(w, h-mScreenOnOffset);
        }
        if (lastGpsOn) {
            mGpsOnPath.lineTo(w, h-mGpsOnOffset);
        }
        if (lastWifiRunning) {
            mWifiRunningPath.lineTo(w, h-mWifiRunningOffset);
        }
        if (lastWakeLock) {
            mWakeLockPath.lineTo(w, h-mWakeLockOffset);
        }
        if (mHavePhoneSignal) {
            mPhoneSignalChart.finish(w);
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        int textHeight = mTextDescent - mTextAscent;
        mThinLineWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2, getResources().getDisplayMetrics());
        if (h > (textHeight*6)) {
            mLargeMode = true;
            if (h > (textHeight*15)) {
                // Plenty of room for the chart.
                mLineWidth = textHeight/2;
            } else {
                // Compress lines to make more room for chart.
                mLineWidth = textHeight/3;
            }
            mLevelTop = textHeight + mLineWidth;
            mScreenOnPaint.setARGB(255, 32, 64, 255);
            mGpsOnPaint.setARGB(255, 32, 64, 255);
            mWifiRunningPaint.setARGB(255, 32, 64, 255);
            mWakeLockPaint.setARGB(255, 32, 64, 255);
        } else {
            mLargeMode = false;
            mLineWidth = mThinLineWidth;
            mLevelTop = 0;
            mScreenOnPaint.setARGB(255, 0, 0, 255);
            mGpsOnPaint.setARGB(255, 0, 0, 255);
            mWifiRunningPaint.setARGB(255, 0, 0, 255);
            mWakeLockPaint.setARGB(255, 0, 0, 255);
        }
        if (mLineWidth <= 0) mLineWidth = 1;
        mTextPaint.setStrokeWidth(mThinLineWidth);
        mBatteryGoodPaint.setStrokeWidth(mThinLineWidth);
        mBatteryWarnPaint.setStrokeWidth(mThinLineWidth);
        mBatteryCriticalPaint.setStrokeWidth(mThinLineWidth);
        if (mDockBatterySupported) {
            mDockBatteryGoodPaint.setStrokeWidth(mThinLineWidth);
            mDockBatteryWarnPaint.setStrokeWidth(mThinLineWidth);
            mDockBatteryCriticalPaint.setStrokeWidth(mThinLineWidth);
        }
        mChargingPaint.setStrokeWidth(mLineWidth);
        mScreenOnPaint.setStrokeWidth(mLineWidth);
        mGpsOnPaint.setStrokeWidth(mLineWidth);
        mWifiRunningPaint.setStrokeWidth(mLineWidth);
        mWakeLockPaint.setStrokeWidth(mLineWidth);

        if (mLargeMode) {
            int barOffset = textHeight + mLineWidth;
            mChargingOffset = mLineWidth;
            mScreenOnOffset = mChargingOffset + barOffset;
            mWakeLockOffset = mScreenOnOffset + barOffset;
            mWifiRunningOffset = mWakeLockOffset + barOffset;
            mGpsOnOffset = mWifiRunningOffset + (mHaveWifi ? barOffset : 0);
            mPhoneSignalOffset = mGpsOnOffset + (mHaveGps ? barOffset : 0);
            mLevelOffset = mPhoneSignalOffset + (mHavePhoneSignal ? barOffset : 0)
                    + ((mLineWidth*3)/2);
            if (mHavePhoneSignal) {
                mPhoneSignalChart.init(w);
            }
        } else {
            mScreenOnOffset = mGpsOnOffset = mWifiRunningOffset
                    = mWakeLockOffset = mLineWidth;
            mChargingOffset = mLineWidth*2;
            mPhoneSignalOffset = 0;
            mLevelOffset = mLineWidth*3;
            if (mHavePhoneSignal) {
                mPhoneSignalChart.init(0);
            }
        }

        mBatLevelPath.reset();
        mBatGoodPath.reset();
        mBatWarnPath.reset();
        mBatCriticalPath.reset();
        if (mDockBatterySupported) {
            mDockBatGoodPath.reset();
            mDockBatWarnPath.reset();
            mDockBatCriticalPath.reset();
        }
        mScreenOnPath.reset();
        mGpsOnPath.reset();
        mWifiRunningPath.reset();
        mWakeLockPath.reset();
        mChargingPath.reset();

        long timeStart = mHistStart;
        long timeChange = mHistEnd-mHistStart;
        long timeEnd = mHistEnd;
        if (mDockBatterySupported) {
            timeStart = Math.min(timeStart, mDockHistStart) ;
            timeChange = Math.max(timeChange, mDockHistEnd-mDockHistStart);
            timeEnd = Math.max(timeEnd, mDockHistEnd);
        }

        final int batLow = mBatLow;
        final int batChange = mBatHigh-mBatLow;
        
        final int levelh = h - mLevelOffset - mLevelTop;
        mLevelBottom = mLevelTop + levelh;
        
        int x = 0, y = 0, startX = 0, lastX = -1, lastY = -1;
        int i = 0;
        Path curLevelPath = null;
        Path lastLinePath = null;
        boolean lastCharging = false, lastScreenOn = false, lastGpsOn = false;
        boolean lastWifiRunning = false, lastWakeLock = false;
        int N = mNumHist;
        if (mStats.startIteratingHistoryLocked()) {
            final HistoryItem rec = new HistoryItem();
            Path path = mBatCriticalPath;
            while (mStats.getNextHistoryLocked(rec) && i < N) {
                if (rec.cmd == BatteryStats.HistoryItem.CMD_UPDATE) {
                    long time = (i == 0 && rec.time != timeStart) ? timeStart : rec.time;
                    x = (int)(((time-timeStart)*w)/timeChange);
                    y = mLevelTop + levelh - ((rec.batteryLevel-batLow)*(levelh-1))/batChange;

                    if (lastX != x) {
                        // We have moved by at least a pixel.
                        if (lastY != y) {
                            // Don't plot changes within a pixel.
                            byte value = rec.batteryLevel;
                            if (value <= BATTERY_CRITICAL) path = mBatCriticalPath;
                            else if (value <= BATTERY_WARN) path = mBatWarnPath;
                            else path = mBatGoodPath;

                            if (path != lastLinePath) {
                                if (lastLinePath != null) {
                                    lastLinePath.lineTo(x, y);
                                }
                                path.moveTo(x, y);
                                lastLinePath = path;
                            } else {
                                path.lineTo(x, y);
                            }

                            if (curLevelPath == null) {
                                curLevelPath = mBatLevelPath;
                                curLevelPath.moveTo(x, y);
                                startX = x;
                            } else {
                                curLevelPath.lineTo(x, y);
                            }
                            lastX = x;
                            lastY = y;
                        }
                    }

                    final boolean charging =
                        (rec.states&HistoryItem.STATE_BATTERY_PLUGGED_FLAG) != 0;
                    if (charging != lastCharging) {
                        if (charging) {
                            mChargingPath.moveTo(x, h-mChargingOffset);
                        } else {
                            mChargingPath.lineTo(x, h-mChargingOffset);
                        }
                        lastCharging = charging;
                    }

                    final boolean screenOn =
                        (rec.states&HistoryItem.STATE_SCREEN_ON_FLAG) != 0;
                    if (screenOn != lastScreenOn) {
                        if (screenOn) {
                            mScreenOnPath.moveTo(x, h-mScreenOnOffset);
                        } else {
                            mScreenOnPath.lineTo(x, h-mScreenOnOffset);
                        }
                        lastScreenOn = screenOn;
                    }

                    final boolean gpsOn =
                        (rec.states&HistoryItem.STATE_GPS_ON_FLAG) != 0;
                    if (gpsOn != lastGpsOn) {
                        if (gpsOn) {
                            mGpsOnPath.moveTo(x, h-mGpsOnOffset);
                        } else {
                            mGpsOnPath.lineTo(x, h-mGpsOnOffset);
                        }
                        lastGpsOn = gpsOn;
                    }

                    final boolean wifiRunning =
                        (rec.states&HistoryItem.STATE_WIFI_RUNNING_FLAG) != 0;
                    if (wifiRunning != lastWifiRunning) {
                        if (wifiRunning) {
                            mWifiRunningPath.moveTo(x, h-mWifiRunningOffset);
                        } else {
                            mWifiRunningPath.lineTo(x, h-mWifiRunningOffset);
                        }
                        lastWifiRunning = wifiRunning;
                    }

                    final boolean wakeLock =
                        (rec.states&HistoryItem.STATE_WAKE_LOCK_FLAG) != 0;
                    if (wakeLock != lastWakeLock) {
                        if (wakeLock) {
                            mWakeLockPath.moveTo(x, h-mWakeLockOffset);
                        } else {
                            mWakeLockPath.lineTo(x, h-mWakeLockOffset);
                        }
                        lastWakeLock = wakeLock;
                    }

                    if (mLargeMode && mHavePhoneSignal) {
                        int bin;
                        if (((rec.states&HistoryItem.STATE_PHONE_STATE_MASK)
                                >> HistoryItem.STATE_PHONE_STATE_SHIFT)
                                == ServiceState.STATE_POWER_OFF) {
                            bin = 0;
                        } else if ((rec.states&HistoryItem.STATE_PHONE_SCANNING_FLAG) != 0) {
                            bin = 1;
                        } else {
                            bin = (rec.states&HistoryItem.STATE_SIGNAL_STRENGTH_MASK)
                                    >> HistoryItem.STATE_SIGNAL_STRENGTH_SHIFT;
                            bin += 2;
                        }
                        mPhoneSignalChart.addTick(x, bin);
                    }

                } else if (rec.cmd != BatteryStats.HistoryItem.CMD_OVERFLOW) {
                    if (curLevelPath != null) {
                        finishPaths(x+1, h, levelh, startX, lastY, curLevelPath, lastX,
                                lastCharging, lastScreenOn, lastGpsOn, lastWifiRunning,
                                lastWakeLock, lastLinePath);
                        lastX = lastY = -1;
                        curLevelPath = null;
                        lastLinePath = null;
                        lastCharging = lastScreenOn = lastGpsOn = lastWakeLock = false;
                    }
                }

                i++;
            }

            // Just fill the line to the end of the graph adding the last known state
            if (rec.time != timeEnd) {
                x = (int)((timeEnd*w)/timeChange);
                path.lineTo(x, lastY);
            }
        }

        finishPaths(w, h, levelh, startX, lastY, curLevelPath, lastX,
                lastCharging, lastScreenOn, lastGpsOn, lastWifiRunning,
                lastWakeLock, lastLinePath);

        // Now draw the dock battery stats
        // We use the history start and end times from the normal battery
        N = mDockNumHist;
        x = 0; y = 0; startX = 0; lastX = -1; lastY = -1;
        i = 0;
        curLevelPath = null;
        lastLinePath = null;
        byte lastStatus = -1, lastLevel = -1;
        if (mDockBatterySupported && mDockStats.startIteratingHistoryLocked()) {
            final HistoryItem rec = new HistoryItem();
            Path path = mDockBatCriticalPath;
            while (mDockStats.getNextHistoryLocked(rec) && i < N) {
                if (rec.cmd == BatteryStats.HistoryItem.CMD_UPDATE) {
                    long time = (i == 0 && rec.time != timeStart) ? timeStart : rec.time;
                    byte level = rec.batteryLevel;
                    boolean skipPath = false;
                    if (lastLevel != -1 && lastStatus == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                        level = lastLevel;
                        skipPath = true;
                    }

                    x = (int)(((time-timeStart)*w)/timeChange);
                    y = mLevelTop + levelh - ((level-batLow)*(levelh-1))/batChange;

                    if (lastX != x) {
                        // We have moved by at least a pixel.
                        if (lastY != y) {
                            // Don't plot changes within a pixel.
                            byte value = level;
                            if (value <= BATTERY_CRITICAL) path = mDockBatCriticalPath;
                            else if (value <= BATTERY_WARN) path = mDockBatWarnPath;
                            else path = mDockBatGoodPath;

                            if (skipPath || rec.batteryStatus ==
                                    BatteryManager.BATTERY_STATUS_UNKNOWN) {
                                // Dock battery is not present or was not present
                                path.moveTo(x, y);
                            } else {
                                if (path != lastLinePath) {
                                    if (lastLinePath != null) {
                                        lastLinePath.lineTo(x, y);
                                    }
                                    path.moveTo(x, y);
                                    lastLinePath = path;
                                } else {
                                    path.lineTo(x, y);
                                }
                            }

                            lastX = x;
                            lastY = y;
                        }
                    }

                } else if (rec.cmd != BatteryStats.HistoryItem.CMD_OVERFLOW) {
                    if (curLevelPath != null) {
                        lastX = lastY = -1;
                        lastLinePath = null;
                    }
                }

                if (rec.batteryStatus != BatteryManager.BATTERY_STATUS_UNKNOWN) {
                    lastLevel = rec.batteryLevel;
                }
                lastStatus = rec.batteryStatus;
                i++;
            }

            // Just fill the line to the end of the graph adding the last known state (only if
            // the dock battery is present)
            if (lastStatus != BatteryManager.BATTERY_STATUS_UNKNOWN && rec.time != timeEnd) {
                byte value = rec.batteryLevel;
                if (value <= BATTERY_CRITICAL) path = mDockBatCriticalPath;
                else if (value <= BATTERY_WARN) path = mDockBatWarnPath;
                else path = mDockBatGoodPath;

                x = (int)((timeEnd*w)/timeChange);
                path.lineTo(x, lastY);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();
        final int height = getHeight();
        final boolean layoutRtl = isLayoutRtl();
        final int textStartX = layoutRtl ? width : 0;
        mTextPaint.setTextAlign(layoutRtl ? Paint.Align.RIGHT : Paint.Align.LEFT);

        canvas.drawPath(mBatLevelPath, mBatteryBackgroundPaint);
        if (mLargeMode) {
            int durationHalfWidth = mTotalDurationStringWidth / 2;
            if (layoutRtl) durationHalfWidth = -durationHalfWidth;
            canvas.drawText(mDurationString, textStartX, -mTextAscent + (mLineWidth / 2),
                    mTextPaint);
            canvas.drawText(mTotalDurationString, (width / 2) - durationHalfWidth,
                    mLevelBottom - mTextAscent + mThinLineWidth, mTextPaint);
        } else {
            int durationHalfWidth = mDurationStringWidth / 2;
            if (layoutRtl) durationHalfWidth = -durationHalfWidth;
            canvas.drawText(mDurationString, (width / 2) - durationHalfWidth,
                    (height / 2) - ((mTextDescent - mTextAscent) / 2) - mTextAscent, mTextPaint);
        }
        if (!mBatGoodPath.isEmpty()) {
            canvas.drawPath(mBatGoodPath, mBatteryGoodPaint);
        }
        if (!mBatWarnPath.isEmpty()) {
            canvas.drawPath(mBatWarnPath, mBatteryWarnPaint);
        }
        if (!mBatCriticalPath.isEmpty()) {
            canvas.drawPath(mBatCriticalPath, mBatteryCriticalPaint);
        }
        if (mDockBatterySupported) {
            if (!mDockBatGoodPath.isEmpty()) {
                canvas.drawPath(mDockBatGoodPath, mDockBatteryGoodPaint);
            }
            if (!mDockBatWarnPath.isEmpty()) {
                canvas.drawPath(mDockBatWarnPath, mDockBatteryWarnPaint);
            }
            if (!mDockBatCriticalPath.isEmpty()) {
                canvas.drawPath(mDockBatCriticalPath, mDockBatteryCriticalPaint);
            }
        }
        if (mHavePhoneSignal) {
            int top = height-mPhoneSignalOffset - (mLineWidth/2);
            mPhoneSignalChart.draw(canvas, top, mLineWidth);
        }
        if (!mScreenOnPath.isEmpty()) {
            canvas.drawPath(mScreenOnPath, mScreenOnPaint);
        }
        if (!mChargingPath.isEmpty()) {
            canvas.drawPath(mChargingPath, mChargingPaint);
        }
        if (mHaveGps) {
            if (!mGpsOnPath.isEmpty()) {
                canvas.drawPath(mGpsOnPath, mGpsOnPaint);
            }
        }
        if (mHaveWifi) {
            if (!mWifiRunningPath.isEmpty()) {
                canvas.drawPath(mWifiRunningPath, mWifiRunningPaint);
            }
        }
        if (!mWakeLockPath.isEmpty()) {
            canvas.drawPath(mWakeLockPath, mWakeLockPaint);
        }

        if (mLargeMode) {
            if (mHavePhoneSignal) {
                canvas.drawText(mPhoneSignalLabel, textStartX,
                        height - mPhoneSignalOffset - mTextDescent, mTextPaint);
            }
            if (mHaveGps) {
                canvas.drawText(mGpsOnLabel, textStartX,
                        height - mGpsOnOffset - mTextDescent, mTextPaint);
            }
            if (mHaveWifi) {
                canvas.drawText(mWifiRunningLabel, textStartX,
                        height - mWifiRunningOffset - mTextDescent, mTextPaint);
            }
            canvas.drawText(mWakeLockLabel, textStartX,
                    height - mWakeLockOffset - mTextDescent, mTextPaint);
            canvas.drawText(mChargingLabel, textStartX,
                    height - mChargingOffset - mTextDescent, mTextPaint);
            canvas.drawText(mScreenOnLabel, textStartX,
                    height - mScreenOnOffset - mTextDescent, mTextPaint);
            canvas.drawLine(0, mLevelBottom+(mThinLineWidth/2), width,
                    mLevelBottom+(mThinLineWidth/2), mTextPaint);
            canvas.drawLine(0, mLevelTop, 0,
                    mLevelBottom+(mThinLineWidth/2), mTextPaint);
            for (int i=0; i<10; i++) {
                int y = mLevelTop + ((mLevelBottom-mLevelTop)*i)/10;
                canvas.drawLine(0, y, mThinLineWidth*2, y, mTextPaint);
            }
        }
    }
}
