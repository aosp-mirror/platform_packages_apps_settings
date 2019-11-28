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

import android.app.Activity;
import android.app.TimePickerDialog;
import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.TimeDetector;
import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
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

    private static final String KEY_TIME = "time";

    private final AutoTimePreferenceController mAutoTimePreferenceController;
    private final TimePreferenceHost mHost;


    public TimePreferenceController(Context context,
            TimePreferenceHost callback,
            AutoTimePreferenceController autoTimePreferenceController) {
        super(context);
        mHost = callback;
        mAutoTimePreferenceController = autoTimePreferenceController;
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
            preference.setEnabled(!mAutoTimePreferenceController.isEnabled());
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

    public TimePickerDialog buildTimePicker(Activity activity) {
        final Calendar calendar = Calendar.getInstance();
        return new TimePickerDialog(
                activity,
                this,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(activity));
    }

    void setTime(int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = Math.max(c.getTimeInMillis(), TimePreferenceHost.MIN_DATE);

        if (when / 1000 < Integer.MAX_VALUE) {
            TimeDetector timeDetector = mContext.getSystemService(TimeDetector.class);
            ManualTimeSuggestion manualTimeSuggestion =
                    TimeDetector.createManualTimeSuggestion(when, "Settings: Set time");
            timeDetector.suggestManualTime(manualTimeSuggestion);
        }
    }
}
