/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.mahdi;

import java.util.Calendar;
import java.util.Date;

import android.app.TimePickerDialog;
import android.content.Context;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.android.settings.R;

public class TimeRangePreference extends Preference implements
    View.OnClickListener {

    private static final String TAG = TimeRangePreference.class.getSimpleName();
    private static final int DIALOG_START_TIME = 1;
    private static final int DIALOG_END_TIME = 2;

    private TextView mStartTimeText;
    private TextView mEndTimeText;
    private int mStartTime;
    private int mEndTime;

    /**
     * @param context
     * @param attrs
     */
    public TimeRangePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * @param context
     * @param stime
     * @param etime
     */
    public TimeRangePreference(Context context, int stime, int etime) {
        super(context);
        mStartTime = stime;
        mEndTime = etime;
        init();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View startTimeLayout = view.findViewById(R.id.start_time);
        if ((startTimeLayout != null) && startTimeLayout instanceof LinearLayout) {
            startTimeLayout.setOnClickListener(this);
        }

        View endTimeLayout = view.findViewById(R.id.end_time);
        if ((endTimeLayout != null) && endTimeLayout instanceof LinearLayout) {
            endTimeLayout.setOnClickListener(this);
        }

        mStartTimeText = (TextView) view.findViewById(R.id.start_time_text);
        mEndTimeText = (TextView) view.findViewById(R.id.end_time_text);

        updatePreferenceViews();
    }

    private void init() {
        setLayoutResource(R.layout.preference_time_range);
    }

    private void updatePreferenceViews() {
        if (mStartTimeText != null) {
            mStartTimeText.setText(returnTime(mStartTime));
        }
        if (mEndTimeText != null) {
            mEndTimeText.setText(returnTime(mEndTime));
        }
    }

    public void setStartTime(int time) {
        mStartTime = time;
        updatePreferenceViews();
    }

    public void setEndTime(int time) {
        mEndTime = time;
        updatePreferenceViews();
    }

    public void setTimeRange(int stime, int etime) {
        mStartTime = stime;
        mEndTime = etime;
        updatePreferenceViews();
    }

    public int getStartTime() {
        return(mStartTime);
    }

    public int getEndTime() {
        return(mEndTime);
    }

    @Override
    public void onClick(android.view.View v) {
        if (v != null) {
            if (R.id.start_time == v.getId()) {
                TimePicker(DIALOG_START_TIME);
            } else if (R.id.end_time == v.getId()) {
                TimePicker(DIALOG_END_TIME);
            }
        }
    }

    private void TimePicker(final int key) {
        int hour;
        int minutes;
        int value = (key == DIALOG_START_TIME ? mStartTime : mEndTime);

        if (value < 0) {
            Calendar calendar = Calendar.getInstance();
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minutes = calendar.get(Calendar.MINUTE);
        } else {
            hour = value / 60;
            minutes = value % 60;
        }

        Context context = getContext();
        TimePickerDialog dlg = new TimePickerDialog(context,
        new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker v, int hours, int minutes) {
                int time = hours * 60 + minutes;
                if (key == DIALOG_START_TIME) {
                    mStartTime = time;
                    mStartTimeText.setText(returnTime(time));
                } else {
                    mEndTime = time;
                    mEndTimeText.setText(returnTime(time));
                }
                callChangeListener(this);
            };
        }, hour, minutes, DateFormat.is24HourFormat(context));
        dlg.show();
    }

    private String returnTime(int t) {
        if (t < 0) {
            return "";
        }

        int hr = t;
        int mn = t;

        hr = hr / 60;
        mn = mn % 60;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, mn);
        Date date = cal.getTime();
        return DateFormat.getTimeFormat(getContext()).format(date);
    }
}
