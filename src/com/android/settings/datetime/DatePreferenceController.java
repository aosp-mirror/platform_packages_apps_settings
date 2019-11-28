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
import android.app.DatePickerDialog;
import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.TimeDetector;
import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.DatePicker;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Calendar;

public class DatePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, DatePickerDialog.OnDateSetListener {

    public interface DatePreferenceHost extends UpdateTimeAndDateCallback {
        void showDatePicker();
    }

    public static final int DIALOG_DATEPICKER = 0;

    private static final String KEY_DATE = "date";

    private final DatePreferenceHost mHost;
    private final AutoTimePreferenceController mAutoTimePreferenceController;

    public DatePreferenceController(Context context, DatePreferenceHost host,
            AutoTimePreferenceController autoTimePreferenceController) {
        super(context);
        mHost = host;
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
        preference.setSummary(DateFormat.getLongDateFormat(mContext).format(now.getTime()));
        if (!((RestrictedPreference) preference).isDisabledByAdmin()) {
            preference.setEnabled(!mAutoTimePreferenceController.isEnabled());
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_DATE)) {
            return false;
        }
        mHost.showDatePicker();
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DATE;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        setDate(year, month, day);
        mHost.updateTimeAndDateDisplay(mContext);
    }

    public DatePickerDialog buildDatePicker(Activity activity) {
        final Calendar calendar = Calendar.getInstance();
        final DatePickerDialog d = new DatePickerDialog(
                activity,
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        // The system clock can't represent dates outside this range.
        calendar.clear();
        calendar.set(2007, Calendar.JANUARY, 1);
        d.getDatePicker().setMinDate(calendar.getTimeInMillis());
        calendar.clear();
        calendar.set(2037, Calendar.DECEMBER, 31);
        d.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        return d;
    }

    @VisibleForTesting
    void setDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = Math.max(c.getTimeInMillis(), DatePreferenceHost.MIN_DATE);

        if (when / 1000 < Integer.MAX_VALUE) {
            TimeDetector timeDetector = mContext.getSystemService(TimeDetector.class);
            ManualTimeSuggestion manualTimeSuggestion =
                    TimeDetector.createManualTimeSuggestion(when, "Settings: Set date");
            timeDetector.suggestManualTime(manualTimeSuggestion);
        }
    }
}
