/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import android.app.TimePickerDialog;
import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.TimeDetector;
import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Calendar;

public class TimePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, TimePickerDialog.OnTimeSetListener {

    public interface TimePreferenceHost extends UpdateTimeAndDateCallback {
        void showTimePicker();
    }

    public static final int DIALOG_TIMEPICKER = 1;

    private static final String TAG = "TimePreferenceController";
    private static final String KEY_TIME = "time";

    private final DatePreferenceController mDatePreferenceController;
    private final TimePreferenceHost mHost;


    public TimePreferenceController(Context context,
            TimePreferenceHost callback,
            DatePreferenceController datePreferenceController) {
        super(context);
        mHost = callback;
        mDatePreferenceController = datePreferenceController;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        final Calendar now = Calendar.getInstance();
        preference.setSummary(DateFormat.getTimeFormat(mContext).format(now.getTime()));
        if (!((RestrictedPreference) preference).isDisabledByAdmin()) {
            boolean enableManualTimeSelection = mDatePreferenceController.isEnabled();
            preference.setEnabled(enableManualTimeSelection);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(KEY_TIME, preference.getKey())) {
            return false;
        }
        mHost.showTimePicker();
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TIME;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (mContext != null) {
            setTime(hourOfDay, minute);
            mHost.updateTimeAndDateDisplay(mContext);
        }
        // We don't need to call timeUpdated() here because the TIME_CHANGED
        // broadcast is sent by the AlarmManager as a side effect of setting the
        // SystemClock time.
    }

    /**
     * Builds a {@link TimePickerDialog} that can be used to request the current time from the user.
     */
    public TimePickerDialog buildTimePicker(Context parentContext) {
        final Calendar calendar = Calendar.getInstance();
        return new TimePickerDialog(
                parentContext,
                this,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(parentContext));
    }

    void setTime(int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = c.getTimeInMillis();

        TimeDetector timeDetector = mContext.getSystemService(TimeDetector.class);
        ManualTimeSuggestion manualTimeSuggestion =
                TimeDetector.createManualTimeSuggestion(when, "Settings: Set time");
        boolean success = timeDetector.suggestManualTime(manualTimeSuggestion);
        if (!success) {
            // This implies the system server is applying tighter bounds than the settings app or
            // the date/time cannot be set for other reasons, e.g. perhaps "auto time" is turned on.
            Log.w(TAG, "Unable to set time with suggestion=" + manualTimeSuggestion);
        }
    }
}
