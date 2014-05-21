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

import android.content.Intent;
import android.graphics.DashPathEffect;
import android.os.BatteryManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TimeUtils;
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
import android.telephony.ServiceState;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import libcore.icu.LocaleData;

import java.util.ArrayList;
import java.util.Calendar;

public class BatteryHistoryChart extends View {
    static final boolean DEBUG = false;
    static final String TAG = "BatteryHistoryChart";

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
    final Paint mTimeRemainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint mChargingPaint = new Paint();
    final Paint mScreenOnPaint = new Paint();
    final Paint mGpsOnPaint = new Paint();
    final Paint mWifiRunningPaint = new Paint();
    final Paint mCpuRunningPaint = new Paint();
    final Paint mDateLinePaint = new Paint();
    final ChartData mPhoneSignalChart = new ChartData();
    final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    final TextPaint mHeaderTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    final Paint mDebugRectPaint = new Paint();

    final Path mBatLevelPath = new Path();
    final Path mBatGoodPath = new Path();
    final Path mBatWarnPath = new Path();
    final Path mBatCriticalPath = new Path();
    final Path mTimeRemainPath = new Path();
    final Path mChargingPath = new Path();
    final Path mScreenOnPath = new Path();
    final Path mGpsOnPath = new Path();
    final Path mWifiRunningPath = new Path();
    final Path mCpuRunningPath = new Path();
    final Path mDateLinePath = new Path();
    
    BatteryStats mStats;
    Intent mBatteryBroadcast;
    long mStatsPeriod;
    String mMaxPercentLabelString;
    String mMinPercentLabelString;
    String mDurationString;
    String mChargeLabelString;
    String mChargeDurationString;
    String mDrainString;
    String mChargingLabel;
    String mScreenOnLabel;
    String mGpsOnLabel;
    String mWifiRunningLabel;
    String mCpuRunningLabel;
    String mPhoneSignalLabel;
    
    int mTextAscent;
    int mTextDescent;
    int mHeaderTextAscent;
    int mHeaderTextDescent;
    int mMaxPercentLabelStringWidth;
    int mMinPercentLabelStringWidth;
    int mDurationStringWidth;
    int mChargeLabelStringWidth;
    int mChargeDurationStringWidth;
    int mDrainStringWidth;

    boolean mLargeMode;

    int mLastWidth = -1;
    int mLastHeight = -1;

    int mLineWidth;
    int mThinLineWidth;
    int mChargingOffset;
    int mScreenOnOffset;
    int mGpsOnOffset;
    int mWifiRunningOffset;
    int mCpuRunningOffset;
    int mPhoneSignalOffset;
    int mLevelOffset;
    int mLevelTop;
    int mLevelBottom;
    int mLevelLeft;
    int mLevelRight;
    static final int PHONE_SIGNAL_X_MASK = CHART_DATA_X_MASK;
    static final int PHONE_SIGNAL_BIN_MASK = CHART_DATA_BIN_MASK;
    static final int PHONE_SIGNAL_BIN_SHIFT = CHART_DATA_BIN_SHIFT;
    
    int mNumHist;
    long mHistStart;
    long mHistDataEnd;
    long mHistEnd;
    long mStartWallTime;
    long mEndDataWallTime;
    long mEndWallTime;
    boolean mDischarging;
    int mBatLow;
    int mBatHigh;
    boolean mHaveWifi;
    boolean mHaveGps;
    boolean mHavePhoneSignal;

    final ArrayList<TimeLabel> mTimeLabels = new ArrayList<TimeLabel>();
    final ArrayList<DateLabel> mDateLabels = new ArrayList<DateLabel>();

    static class TextAttrs {
        ColorStateList textColor = null;
        int textSize = 15;
        int typefaceIndex = -1;
        int styleIndex = -1;

        void retrieve(Context context, TypedArray from, int index) {
            TypedArray appearance = null;
            int ap = from.getResourceId(index, -1);
            if (ap != -1) {
                appearance = context.obtainStyledAttributes(ap,
                                    com.android.internal.R.styleable.TextAppearance);
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
        }

        void apply(Context context, TextPaint paint) {
            paint.density = context.getResources().getDisplayMetrics().density;
            paint.setCompatibilityScaling(
                    context.getResources().getCompatibilityInfo().applicationScale);

            paint.setColor(textColor.getDefaultColor());
            paint.setTextSize(textSize);

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

            setTypeface(paint, tf, styleIndex);
        }

        public void setTypeface(TextPaint paint, Typeface tf, int style) {
            if (style > 0) {
                if (tf == null) {
                    tf = Typeface.defaultFromStyle(style);
                } else {
                    tf = Typeface.create(tf, style);
                }

                paint.setTypeface(tf);
                // now compute what (if any) algorithmic styling is needed
                int typefaceStyle = tf != null ? tf.getStyle() : 0;
                int need = style & ~typefaceStyle;
                paint.setFakeBoldText((need & Typeface.BOLD) != 0);
                paint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
            } else {
                paint.setFakeBoldText(false);
                paint.setTextSkewX(0);
                paint.setTypeface(tf);
            }
        }
    }

    static class TimeLabel {
        final int x;
        final String label;
        final int width;

        TimeLabel(Context context, TextPaint paint, int x, Calendar cal, boolean use24hr) {
            this.x = x;
            if (use24hr) {
                label = context.getString(R.string.battery_stats_hour_24_label,
                        cal.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = cal.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                if (cal.get(Calendar.AM_PM) == Calendar.AM) {
                    label = context.getString(R.string.battery_stats_hour_am_label, hour);
                } else {
                    label = context.getString(R.string.battery_stats_hour_pm_label, hour);
                }
            }
            width = (int)paint.measureText(label);
        }
    }

    static class DateLabel {
        final int x;
        final String label;
        final int width;

        DateLabel(Context context, TextPaint paint, int x, Calendar cal, boolean dayFirst) {
            this.x = x;
            if (dayFirst) {
                label = context.getString(R.string.battery_stats_date_day_first_label,
                        cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH));
            } else {
                label = context.getString(R.string.battery_stats_date_month_first_label,
                        cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH));
            }
            width = (int)paint.measureText(label);
        }
    }

    public BatteryHistoryChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mThinLineWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2, getResources().getDisplayMetrics());

        mBatteryBackgroundPaint.setColor(0xff263238);
        mBatteryBackgroundPaint.setStyle(Paint.Style.FILL);
        mBatteryGoodPaint.setARGB(128, 0, 255, 0);
        mBatteryGoodPaint.setStyle(Paint.Style.STROKE);
        mBatteryWarnPaint.setARGB(128, 255, 255, 0);
        mBatteryWarnPaint.setStyle(Paint.Style.STROKE);
        mBatteryCriticalPaint.setARGB(192, 255, 0, 0);
        mBatteryCriticalPaint.setStyle(Paint.Style.STROKE);
        mTimeRemainPaint.setColor(0xffA5B1B7);
        mTimeRemainPaint.setStyle(Paint.Style.FILL);
        mChargingPaint.setARGB(255, 0, 128, 0);
        mChargingPaint.setStyle(Paint.Style.STROKE);
        mScreenOnPaint.setStyle(Paint.Style.STROKE);
        mGpsOnPaint.setStyle(Paint.Style.STROKE);
        mWifiRunningPaint.setStyle(Paint.Style.STROKE);
        mCpuRunningPaint.setStyle(Paint.Style.STROKE);
        mPhoneSignalChart.setColors(new int[] {
                0x00000000, 0xffa00000, 0xffa07000, 0xffa0a000,
                0xff80a000, 0xff409000, 0xff008000
        });
        mDebugRectPaint.setARGB(255, 255, 0, 0);
        mDebugRectPaint.setStyle(Paint.Style.STROKE);

        TypedArray a =
            context.obtainStyledAttributes(
                attrs, R.styleable.BatteryHistoryChart, 0, 0);

        final TextAttrs mainTextAttrs = new TextAttrs();
        final TextAttrs headTextAttrs = new TextAttrs();
        mainTextAttrs.retrieve(context, a, R.styleable.BatteryHistoryChart_android_textAppearance);
        headTextAttrs.retrieve(context, a, R.styleable.BatteryHistoryChart_headerAppearance);

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
                    mainTextAttrs.textColor = a.getColorStateList(attr);
                    headTextAttrs.textColor = a.getColorStateList(attr);
                    break;

                case R.styleable.BatteryHistoryChart_android_textSize:
                    mainTextAttrs.textSize = a.getDimensionPixelSize(attr, mainTextAttrs.textSize);
                    headTextAttrs.textSize = a.getDimensionPixelSize(attr, headTextAttrs.textSize);
                    break;

                case R.styleable.BatteryHistoryChart_android_typeface:
                    mainTextAttrs.typefaceIndex = a.getInt(attr, mainTextAttrs.typefaceIndex);
                    headTextAttrs.typefaceIndex = a.getInt(attr, headTextAttrs.typefaceIndex);
                    break;

                case R.styleable.BatteryHistoryChart_android_textStyle:
                    mainTextAttrs.styleIndex = a.getInt(attr, mainTextAttrs.styleIndex);
                    headTextAttrs.styleIndex = a.getInt(attr, headTextAttrs.styleIndex);
                    break;
            }
        }
        
        a.recycle();
        
        mainTextAttrs.apply(context, mTextPaint);
        headTextAttrs.apply(context, mHeaderTextPaint);

        mDateLinePaint.set(mTextPaint);
        mDateLinePaint.setStyle(Paint.Style.STROKE);
        int hairlineWidth = mThinLineWidth/2;
        if (hairlineWidth < 1) {
            hairlineWidth = 1;
        }
        mDateLinePaint.setStrokeWidth(hairlineWidth);
        mDateLinePaint.setPathEffect(new DashPathEffect(new float[] {
                mThinLineWidth * 2, mThinLineWidth * 2 }, 0));

        if (shadowcolor != 0) {
            mTextPaint.setShadowLayer(r, dx, dy, shadowcolor);
            mHeaderTextPaint.setShadowLayer(r, dx, dy, shadowcolor);
        }
    }

    void setStats(BatteryStats stats, Intent broadcast) {
        mStats = stats;
        mBatteryBroadcast = broadcast;

        final long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;

        long uSecTime = mStats.computeBatteryRealtime(elapsedRealtimeUs,
                BatteryStats.STATS_SINCE_CHARGED);
        mStatsPeriod = uSecTime;
        mChargingLabel = getContext().getString(R.string.battery_stats_charging_label);
        mScreenOnLabel = getContext().getString(R.string.battery_stats_screen_on_label);
        mGpsOnLabel = getContext().getString(R.string.battery_stats_gps_on_label);
        mWifiRunningLabel = getContext().getString(R.string.battery_stats_wifi_running_label);
        mCpuRunningLabel = getContext().getString(R.string.battery_stats_wake_lock_label);
        mPhoneSignalLabel = getContext().getString(R.string.battery_stats_phone_signal_label);

        mMaxPercentLabelString = getContext().getResources().getString(
                R.string.percentage, 100);
        mMinPercentLabelString = getContext().getResources().getString(
                R.string.percentage, 0);

        int batteryLevel = com.android.settings.Utils.getBatteryLevel(mBatteryBroadcast);
        final int status = mBatteryBroadcast.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        long remainingTimeUs = 0;
        mDischarging = true;
        if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            final long drainTime = mStats.computeBatteryTimeRemaining(elapsedRealtimeUs);
            if (drainTime > 0) {
                remainingTimeUs = drainTime;
                String timeString = Formatter.formatShortElapsedTime(getContext(),
                        drainTime / 1000);
                mChargeLabelString = getContext().getResources().getString(
                        R.string.power_discharging_duration, batteryLevel, timeString);
            } else {
                mChargeLabelString = getContext().getResources().getString(
                        R.string.power_discharging, batteryLevel);
            }
        } else {
            final long chargeTime = mStats.computeChargeTimeRemaining(elapsedRealtimeUs);
            final String statusLabel = com.android.settings.Utils.getBatteryStatus(getResources(),
                    mBatteryBroadcast);
            if (chargeTime > 0 && status != BatteryManager.BATTERY_STATUS_FULL) {
                mDischarging = false;
                remainingTimeUs = chargeTime;
                String timeString = Formatter.formatShortElapsedTime(getContext(),
                        chargeTime / 1000);
                mChargeLabelString = getContext().getResources().getString(
                        R.string.power_charging_duration, batteryLevel, statusLabel, timeString);
            } else {
                mChargeLabelString = getContext().getResources().getString(
                        R.string.power_charging, batteryLevel, statusLabel);
            }
        }
        mDrainString = "";
        mChargeDurationString = "";

        int pos = 0;
        int lastInteresting = 0;
        byte lastLevel = -1;
        mBatLow = 0;
        mBatHigh = 100;
        mStartWallTime = 0;
        mEndDataWallTime = 0;
        mEndWallTime = 0;
        mHistStart = 0;
        mHistEnd = 0;
        long lastWallTime = 0;
        long lastRealtime = 0;
        int aggrStates = 0;
        boolean first = true;
        if (stats.startIteratingHistoryLocked()) {
            final HistoryItem rec = new HistoryItem();
            while (stats.getNextHistoryLocked(rec)) {
                pos++;
                if (first) {
                    first = false;
                    mHistStart = rec.time;
                }
                if (rec.cmd == HistoryItem.CMD_CURRENT_TIME
                        || rec.cmd == HistoryItem.CMD_RESET) {
                    lastWallTime = rec.currentTime;
                    lastRealtime = rec.time;
                    if (mStartWallTime == 0) {
                        mStartWallTime = lastWallTime;
                    }
                }
                if (rec.isDeltaData()) {
                    if (rec.batteryLevel != lastLevel || pos == 1) {
                        lastLevel = rec.batteryLevel;
                    }
                    lastInteresting = pos;
                    mHistDataEnd = rec.time;
                    aggrStates |= rec.states;
                }
            }
        }
        mHistEnd = mHistDataEnd + (remainingTimeUs/1000);
        mEndDataWallTime = lastWallTime + mHistDataEnd - lastRealtime;
        mEndWallTime = mEndDataWallTime + (remainingTimeUs/1000);
        mNumHist = lastInteresting;
        mHaveGps = (aggrStates&HistoryItem.STATE_GPS_ON_FLAG) != 0;
        mHaveWifi = (aggrStates&HistoryItem.STATE_WIFI_RUNNING_FLAG) != 0;
        if (!com.android.settings.Utils.isWifiOnly(getContext())) {
            mHavePhoneSignal = true;
        }
        if (mHistEnd <= mHistStart) mHistEnd = mHistStart+1;

        //String durationString = Utils.formatElapsedTime(getContext(), mStatsPeriod / 1000, true);
        //mDurationString = getContext().getString(R.string.battery_stats_on_battery,
        //        durationString);
        mDurationString = Utils.formatElapsedTime(getContext(), mHistEnd - mHistStart, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMaxPercentLabelStringWidth = (int)mTextPaint.measureText(mMaxPercentLabelString);
        mMinPercentLabelStringWidth = (int)mTextPaint.measureText(mMinPercentLabelString);
        mDurationStringWidth = (int)mTextPaint.measureText(mDurationString);
        mDrainStringWidth = (int)mHeaderTextPaint.measureText(mDrainString);
        mChargeLabelStringWidth = (int)mHeaderTextPaint.measureText(mChargeLabelString);
        mChargeDurationStringWidth = (int)mHeaderTextPaint.measureText(mChargeDurationString);
        mTextAscent = (int)mTextPaint.ascent();
        mTextDescent = (int)mTextPaint.descent();
        mHeaderTextAscent = (int)mHeaderTextPaint.ascent();
        mHeaderTextDescent = (int)mHeaderTextPaint.descent();
    }

    void finishPaths(int w, int h, int levelh, int startX, int y, Path curLevelPath,
            int lastX, boolean lastCharging, boolean lastScreenOn, boolean lastGpsOn,
            boolean lastWifiRunning, boolean lastCpuRunning, Path lastPath) {
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
        if (lastCpuRunning) {
            mCpuRunningPath.lineTo(w, h - mCpuRunningOffset);
        }
        if (mHavePhoneSignal) {
            mPhoneSignalChart.finish(w);
        }
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(getContext());
    }

    private boolean isDayFirst() {
        String value = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.DATE_FORMAT);
        if (value == null) {
            LocaleData d = LocaleData.get(mContext.getResources().getConfiguration().locale);
            value = d.shortDateFormat4;
        }
        return value.indexOf('M') > value.indexOf('d');
    }

    /*
    private void buildTime() {
        java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(context);
        final Calendar now = Calendar.getInstance();
        mDummyDate.setTimeZone(now.getTimeZone());
        // We use December 31st because it's unambiguous when demonstrating the date format.
        // We use 13:00 so we can demonstrate the 12/24 hour options.
        mDummyDate.set(now.get(Calendar.YEAR), 11, 31, 13, 0, 0);
        Date dummyDate = mDummyDate.getTime();
        mTimePref.setSummary(DateFormat.getTimeFormat(getActivity()).format(now.getTime()));
        mTimeZone.setSummary(getTimeZoneText(now.getTimeZone(), true));
        mDatePref.setSummary(shortDateFormat.format(now.getTime()));
        mDateFormat.setSummary(shortDateFormat.format(dummyDate));
        mTime24Pref.setSummary(DateFormat.getTimeFormat(getActivity()).format(dummyDate));

    }
    */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (DEBUG) Log.d(TAG, "onSizeChanged: " + oldw + "x" + oldh + " to " + w + "x" + h);

        if (mLastWidth == w && mLastHeight == h) {
            return;
        }

        int textHeight = mTextDescent - mTextAscent;
        int headerTextHeight = mHeaderTextDescent - mHeaderTextAscent;
        if (h > (textHeight*12)) {
            mLargeMode = true;
            if (h > (textHeight*15)) {
                // Plenty of room for the chart.
                mLineWidth = textHeight/2;
            } else {
                // Compress lines to make more room for chart.
                mLineWidth = textHeight/3;
            }
            mScreenOnPaint.setARGB(255, 32, 64, 255);
            mGpsOnPaint.setARGB(255, 32, 64, 255);
            mWifiRunningPaint.setARGB(255, 32, 64, 255);
            mCpuRunningPaint.setARGB(255, 32, 64, 255);
        } else {
            mLargeMode = false;
            mLineWidth = mThinLineWidth;
            mScreenOnPaint.setARGB(255, 0, 0, 255);
            mGpsOnPaint.setARGB(255, 0, 0, 255);
            mWifiRunningPaint.setARGB(255, 0, 0, 255);
            mCpuRunningPaint.setARGB(255, 0, 0, 255);
        }
        if (mLineWidth <= 0) mLineWidth = 1;

        mLevelTop = headerTextHeight*2 - mTextAscent;
        mLevelLeft = mMaxPercentLabelStringWidth + mThinLineWidth*3;
        mLevelRight = w;
        int levelWidth = mLevelRight-mLevelLeft;

        mTextPaint.setStrokeWidth(mThinLineWidth);
        mBatteryGoodPaint.setStrokeWidth(mThinLineWidth);
        mBatteryWarnPaint.setStrokeWidth(mThinLineWidth);
        mBatteryCriticalPaint.setStrokeWidth(mThinLineWidth);
        mChargingPaint.setStrokeWidth(mLineWidth);
        mScreenOnPaint.setStrokeWidth(mLineWidth);
        mGpsOnPaint.setStrokeWidth(mLineWidth);
        mWifiRunningPaint.setStrokeWidth(mLineWidth);
        mCpuRunningPaint.setStrokeWidth(mLineWidth);
        mDebugRectPaint.setStrokeWidth(1);

        int fullBarOffset = textHeight + mLineWidth;

        if (mLargeMode) {
            mChargingOffset = mLineWidth;
            mScreenOnOffset = mChargingOffset + fullBarOffset;
            mCpuRunningOffset = mScreenOnOffset + fullBarOffset;
            mWifiRunningOffset = mCpuRunningOffset + fullBarOffset;
            mGpsOnOffset = mWifiRunningOffset + (mHaveWifi ? fullBarOffset : 0);
            mPhoneSignalOffset = mGpsOnOffset + (mHaveGps ? fullBarOffset : 0);
            mLevelOffset = mPhoneSignalOffset + (mHavePhoneSignal ? fullBarOffset : 0)
                    + mLineWidth*2 + mLineWidth/2;
            if (mHavePhoneSignal) {
                mPhoneSignalChart.init(w);
            }
        } else {
            mScreenOnOffset = mGpsOnOffset = mWifiRunningOffset
                    = mCpuRunningOffset = mChargingOffset = mPhoneSignalOffset = 0;
            mLevelOffset = fullBarOffset + mThinLineWidth*4;
            if (mHavePhoneSignal) {
                mPhoneSignalChart.init(0);
            }
        }

        mBatLevelPath.reset();
        mBatGoodPath.reset();
        mBatWarnPath.reset();
        mTimeRemainPath.reset();
        mBatCriticalPath.reset();
        mScreenOnPath.reset();
        mGpsOnPath.reset();
        mWifiRunningPath.reset();
        mCpuRunningPath.reset();
        mChargingPath.reset();

        final long walltimeStart = mStartWallTime;
        final long walltimeChange = mEndWallTime-walltimeStart;
        long curWalltime = 0;
        long lastRealtime = 0;

        final int batLow = mBatLow;
        final int batChange = mBatHigh-mBatLow;

        final int levelh = h - mLevelOffset - mLevelTop;
        mLevelBottom = mLevelTop + levelh;

        int x = mLevelLeft, y = 0, startX = mLevelLeft, lastX = -1, lastY = -1;
        int i = 0;
        Path curLevelPath = null;
        Path lastLinePath = null;
        boolean lastCharging = false, lastScreenOn = false, lastGpsOn = false;
        boolean lastWifiRunning = false, lastCpuRunning = false;
        final int N = mNumHist;
        if (mStats.startIteratingHistoryLocked()) {
            final HistoryItem rec = new HistoryItem();
            while (mStats.getNextHistoryLocked(rec) && i < N) {
                if (rec.cmd == HistoryItem.CMD_CURRENT_TIME || rec.cmd == HistoryItem.CMD_RESET) {
                    curWalltime = rec.currentTime;
                    lastRealtime = rec.time;
                } else if (curWalltime != 0) {
                    curWalltime += rec.time-lastRealtime;
                    lastRealtime = rec.time;
                }
                if (curWalltime != 0 && rec.isDeltaData()) {
                    x = mLevelLeft + (int)(((curWalltime-walltimeStart)*levelWidth)/walltimeChange);
                    if (false) {
                        StringBuilder sb = new StringBuilder(128);
                        sb.append("walloff=");
                        TimeUtils.formatDuration(curWalltime - walltimeStart, sb);
                        sb.append(" wallchange=");
                        TimeUtils.formatDuration(walltimeChange, sb);
                        sb.append(" x=");
                        sb.append(x);
                        Log.d("foo", sb.toString());
                    }
                    y = mLevelTop + levelh - ((rec.batteryLevel-batLow)*(levelh-1))/batChange;

                    if (lastX != x) {
                        // We have moved by at least a pixel.
                        if (lastY != y) {
                            // Don't plot changes within a pixel.
                            Path path;
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

                    if (mLargeMode) {
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

                        final boolean cpuRunning =
                            (rec.states&HistoryItem.STATE_CPU_RUNNING_FLAG) != 0;
                        if (cpuRunning != lastCpuRunning) {
                            if (cpuRunning) {
                                mCpuRunningPath.moveTo(x, h - mCpuRunningOffset);
                            } else {
                                mCpuRunningPath.lineTo(x, h - mCpuRunningOffset);
                            }
                            lastCpuRunning = cpuRunning;
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
                    }

                } else if (rec.cmd != HistoryItem.CMD_OVERFLOW
                        && rec.cmd != HistoryItem.CMD_CURRENT_TIME) {
                    if (curLevelPath != null) {
                        finishPaths(x+1, h, levelh, startX, lastY, curLevelPath, lastX,
                                lastCharging, lastScreenOn, lastGpsOn, lastWifiRunning,
                                lastCpuRunning, lastLinePath);
                        lastX = lastY = -1;
                        curLevelPath = null;
                        lastLinePath = null;
                        lastCharging = lastScreenOn = lastGpsOn = lastCpuRunning = false;
                    }
                }
                
                i++;
            }
        }

        // Figure out where the actual data ends on the screen.
        x = mLevelLeft + (int)(((mEndDataWallTime-walltimeStart)*levelWidth)/walltimeChange);

        finishPaths(x, h, levelh, startX, lastY, curLevelPath, lastX,
                lastCharging, lastScreenOn, lastGpsOn, lastWifiRunning,
                lastCpuRunning, lastLinePath);

        if (x < w) {
            // If we reserved room for the remaining time, create a final path to draw
            // that part of the UI.
            mTimeRemainPath.moveTo(x, lastY);
            int fullY = mLevelTop + levelh - ((100-batLow)*(levelh-1))/batChange;
            int emptyY = mLevelTop + levelh - ((0-batLow)*(levelh-1))/batChange;
            if (mDischarging) {
                mTimeRemainPath.lineTo(mLevelRight, emptyY);
            } else {
                mTimeRemainPath.lineTo(mLevelRight, fullY);
                mTimeRemainPath.lineTo(mLevelRight, emptyY);
            }
            mTimeRemainPath.lineTo(x, emptyY);
            mTimeRemainPath.close();
        }

        // Create the time labels at the bottom.
        mTimeLabels.clear();
        boolean is24hr = is24Hour();
        Calendar calStart = Calendar.getInstance();
        calStart.setTimeInMillis(mStartWallTime);
        calStart.set(Calendar.MILLISECOND, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MINUTE, 0);
        long startRoundTime = calStart.getTimeInMillis();
        if (startRoundTime < mStartWallTime) {
            calStart.set(Calendar.HOUR_OF_DAY, calStart.get(Calendar.HOUR_OF_DAY)+1);
            startRoundTime = calStart.getTimeInMillis();
        }
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTimeInMillis(mEndWallTime);
        calEnd.set(Calendar.MILLISECOND, 0);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MINUTE, 0);
        long endRoundTime = calEnd.getTimeInMillis();
        if (startRoundTime < endRoundTime) {
            addTimeLabel(calStart, mLevelLeft, mLevelRight, is24hr);
            Calendar calMid = Calendar.getInstance();
            calMid.setTimeInMillis(mStartWallTime+((mEndWallTime-mStartWallTime)/2));
            calMid.set(Calendar.MILLISECOND, 0);
            calMid.set(Calendar.SECOND, 0);
            calMid.set(Calendar.MINUTE, 0);
            long calMidMillis = calMid.getTimeInMillis();
            if (calMidMillis > startRoundTime && calMidMillis < endRoundTime) {
                addTimeLabel(calMid, mLevelLeft, mLevelRight, is24hr);
            }
            addTimeLabel(calEnd, mLevelLeft, mLevelRight, is24hr);
        }

        // Create the date labels if the chart includes multiple days
        mDateLabels.clear();
        if (calStart.get(Calendar.DAY_OF_YEAR) != calEnd.get(Calendar.DAY_OF_YEAR) ||
                calStart.get(Calendar.YEAR) != calEnd.get(Calendar.YEAR)) {
            boolean isDayFirst = isDayFirst();
            calStart.set(Calendar.HOUR_OF_DAY, 0);
            startRoundTime = calStart.getTimeInMillis();
            if (startRoundTime < mStartWallTime) {
                calStart.set(Calendar.DAY_OF_YEAR, calStart.get(Calendar.DAY_OF_YEAR) + 1);
                startRoundTime = calStart.getTimeInMillis();
            }
            calEnd.set(Calendar.HOUR_OF_DAY, 0);
            endRoundTime = calEnd.getTimeInMillis();
            if (startRoundTime < endRoundTime) {
                addDateLabel(calStart, mLevelLeft, mLevelRight, isDayFirst);
            }
            addDateLabel(calEnd, mLevelLeft, mLevelRight, isDayFirst);
        }
    }

    void addTimeLabel(Calendar cal, int levelLeft, int levelRight, boolean is24hr) {
        final long walltimeStart = mStartWallTime;
        final long walltimeChange = mEndWallTime-walltimeStart;
        mTimeLabels.add(new TimeLabel(getContext(), mTextPaint,
                levelLeft + (int)(((cal.getTimeInMillis()-walltimeStart)*(levelRight-levelLeft))
                        / walltimeChange),
                cal, is24hr));
    }

    void addDateLabel(Calendar cal, int levelLeft, int levelRight, boolean isDayFirst) {
        final long walltimeStart = mStartWallTime;
        final long walltimeChange = mEndWallTime-walltimeStart;
        mDateLabels.add(new DateLabel(getContext(), mTextPaint,
                levelLeft + (int)(((cal.getTimeInMillis()-walltimeStart)*(levelRight-levelLeft))
                        / walltimeChange),
                cal, isDayFirst));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();
        final int height = getHeight();

        if (DEBUG) Log.d(TAG, "onDraw: " + width + "x" + height);

        final boolean layoutRtl = isLayoutRtl();
        final int textStartX = layoutRtl ? width : 0;
        final int textEndX = layoutRtl ? 0 : width;
        final Paint.Align textAlignLeft = layoutRtl ? Paint.Align.RIGHT : Paint.Align.LEFT;
        final Paint.Align textAlignRight = layoutRtl ? Paint.Align.LEFT : Paint.Align.RIGHT;

        if (DEBUG) {
            canvas.drawRect(1, 1, width, height, mDebugRectPaint);
        }

        if (DEBUG) Log.d(TAG, "Drawing level path.");
        canvas.drawPath(mBatLevelPath, mBatteryBackgroundPaint);
        if (!mTimeRemainPath.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Drawing time remain path.");
            canvas.drawPath(mTimeRemainPath, mTimeRemainPaint);
        }
        int durationHalfWidth = mDurationStringWidth / 2;
        if (layoutRtl) durationHalfWidth = -durationHalfWidth;
        if (mTimeLabels.size() > 0) {
            int y = mLevelBottom - mTextAscent + (mThinLineWidth*4);
            int ytick = mLevelBottom+mThinLineWidth+(mThinLineWidth/2);
            mTextPaint.setTextAlign(Paint.Align.LEFT);
            int lastX = 0;
            for (int i=0; i<mTimeLabels.size(); i++) {
                TimeLabel label = mTimeLabels.get(i);
                if (i == 0) {
                    int x = label.x - label.width/2;
                    if (x < 0) {
                        x = 0;
                    }
                    if (DEBUG) Log.d(TAG, "Drawing left label: " + label.label + " @ " + x);
                    canvas.drawText(label.label, x, y, mTextPaint);
                    canvas.drawLine(label.x, ytick, label.x, ytick+mThinLineWidth, mTextPaint);
                    lastX = x + label.width;
                } else if (i < (mTimeLabels.size()-1)) {
                    int x = label.x - label.width/2;
                    if (x < (lastX+mTextAscent)) {
                        continue;
                    }
                    TimeLabel nextLabel = mTimeLabels.get(i+1);
                    if (x > (width-nextLabel.width-mTextAscent)) {
                        continue;
                    }
                    if (DEBUG) Log.d(TAG, "Drawing middle label: " + label.label + " @ " + x);
                    canvas.drawText(label.label, x, y, mTextPaint);
                    canvas.drawLine(label.x, ytick, label.x, ytick + mThinLineWidth, mTextPaint);
                    lastX = x + label.width;
                } else {
                    int x = label.x - label.width/2;
                    if ((x+label.width) >= width) {
                        x = width-1-label.width;
                    }
                    if (DEBUG) Log.d(TAG, "Drawing right label: " + label.label + " @ " + x);
                    canvas.drawText(label.label, x, y, mTextPaint);
                    canvas.drawLine(label.x, ytick, label.x, ytick+mThinLineWidth, mTextPaint);
                }
            }
        }

        if (false) {
            // Old code for printing label.
            mTextPaint.setTextAlign(textAlignLeft);
            if (mLargeMode) {
                canvas.drawText(mDurationString, (width / 2) - durationHalfWidth,
                        mLevelBottom - mTextAscent + mThinLineWidth, mTextPaint);
            } else {
                canvas.drawText(mDurationString, (width / 2) - durationHalfWidth,
                        mLevelTop + ((height-mLevelTop) / 2) - ((mTextDescent - mTextAscent) / 2)
                                - mTextAscent, mTextPaint);
            }
        }

        int headerTop = -mHeaderTextAscent + (mHeaderTextDescent-mHeaderTextAscent)/3;
        mHeaderTextPaint.setTextAlign(textAlignLeft);
        if (DEBUG) Log.d(TAG, "Drawing charge label string: " + mChargeLabelString);
        canvas.drawText(mChargeLabelString, textStartX, headerTop, mHeaderTextPaint);
        durationHalfWidth = mChargeDurationStringWidth / 2;
        if (layoutRtl) durationHalfWidth = -durationHalfWidth;
        int headerCenter = ((width-mChargeDurationStringWidth-mDrainStringWidth)/2)
                + (layoutRtl ? mDrainStringWidth : mChargeLabelStringWidth);
        if (DEBUG) Log.d(TAG, "Drawing charge duration string: " + mChargeDurationString);
        canvas.drawText(mChargeDurationString, headerCenter - durationHalfWidth, headerTop,
                mHeaderTextPaint);
        mHeaderTextPaint.setTextAlign(textAlignRight);
        if (DEBUG) Log.d(TAG, "Drawing drain string: " + mDrainString);
        canvas.drawText(mDrainString, textEndX, headerTop, mHeaderTextPaint);

        if (!mBatGoodPath.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Drawing good battery path");
            canvas.drawPath(mBatGoodPath, mBatteryGoodPaint);
        }
        if (!mBatWarnPath.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Drawing warn battery path");
            canvas.drawPath(mBatWarnPath, mBatteryWarnPaint);
        }
        if (!mBatCriticalPath.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Drawing critical battery path");
            canvas.drawPath(mBatCriticalPath, mBatteryCriticalPaint);
        }
        if (mHavePhoneSignal) {
            if (DEBUG) Log.d(TAG, "Drawing phone signal path");
            int top = height-mPhoneSignalOffset - (mLineWidth/2);
            mPhoneSignalChart.draw(canvas, top, mLineWidth);
        }
        if (!mScreenOnPath.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Drawing screen on path");
            canvas.drawPath(mScreenOnPath, mScreenOnPaint);
        }
        if (!mChargingPath.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Drawing charging path");
            canvas.drawPath(mChargingPath, mChargingPaint);
        }
        if (mHaveGps) {
            if (!mGpsOnPath.isEmpty()) {
                if (DEBUG) Log.d(TAG, "Drawing gps path");
                canvas.drawPath(mGpsOnPath, mGpsOnPaint);
            }
        }
        if (mHaveWifi) {
            if (!mWifiRunningPath.isEmpty()) {
                if (DEBUG) Log.d(TAG, "Drawing wifi path");
                canvas.drawPath(mWifiRunningPath, mWifiRunningPaint);
            }
        }
        if (!mCpuRunningPath.isEmpty()) {
            if (DEBUG) Log.d(TAG, "Drawing running path");
            canvas.drawPath(mCpuRunningPath, mCpuRunningPaint);
        }

        if (mLargeMode) {
            if (DEBUG) Log.d(TAG, "Drawing large mode labels");
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
            canvas.drawText(mCpuRunningLabel, textStartX,
                    height - mCpuRunningOffset - mTextDescent, mTextPaint);
            canvas.drawText(mChargingLabel, textStartX,
                    height - mChargingOffset - mTextDescent, mTextPaint);
            canvas.drawText(mScreenOnLabel, textStartX,
                    height - mScreenOnOffset - mTextDescent, mTextPaint);
        }

        canvas.drawLine(mLevelLeft-mThinLineWidth, mLevelTop, mLevelLeft-mThinLineWidth,
                mLevelBottom+(mThinLineWidth/2), mTextPaint);
        if (mLargeMode) {
            for (int i=0; i<10; i++) {
                int y = mLevelTop + mThinLineWidth/2 + ((mLevelBottom-mLevelTop)*i)/10;
                canvas.drawLine(mLevelLeft-mThinLineWidth*2-mThinLineWidth/2, y,
                        mLevelLeft-mThinLineWidth-mThinLineWidth/2, y, mTextPaint);
            }
        }
        if (DEBUG) Log.d(TAG, "Drawing max percent, origw=" + mMaxPercentLabelStringWidth
                + ", noww=" + (int)mTextPaint.measureText(mMaxPercentLabelString));
        canvas.drawText(mMaxPercentLabelString, 0, mLevelTop, mTextPaint);
        canvas.drawText(mMinPercentLabelString,
                mMaxPercentLabelStringWidth-mMinPercentLabelStringWidth,
                mLevelBottom - mThinLineWidth, mTextPaint);
        canvas.drawLine(0, mLevelBottom+mThinLineWidth, width,
                mLevelBottom+mThinLineWidth, mTextPaint);

        if (mDateLabels.size() > 0) {
            int ytop = mLevelTop + mTextAscent;
            int ybottom = mLevelBottom;
            int lastLeft = mLevelRight;
            mTextPaint.setTextAlign(Paint.Align.LEFT);
            for (int i=mDateLabels.size()-1; i>=0; i--) {
                DateLabel label = mDateLabels.get(i);
                int left = label.x - mThinLineWidth;
                int x = label.x + mThinLineWidth*2;
                if ((x+label.width) >= lastLeft) {
                    x = label.x - mThinLineWidth*2 - label.width;
                    left = x - mThinLineWidth;
                    if (left >= lastLeft) {
                        // okay we give up.
                        continue;
                    }
                }
                if (left < mLevelLeft) {
                    // Won't fit on left, give up.
                    continue;
                }
                mDateLinePath.reset();
                mDateLinePath.moveTo(label.x, ytop);
                mDateLinePath.lineTo(label.x, ybottom);
                canvas.drawPath(mDateLinePath, mDateLinePaint);
                canvas.drawText(label.label, x, ytop - mTextAscent, mTextPaint);
            }
        }
    }
}
