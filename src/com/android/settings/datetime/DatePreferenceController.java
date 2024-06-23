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

import static android.app.time.Capabilities.CAPABILITY_POSSESSED;

import android.app.DatePickerDialog;
import android.app.time.TimeCapabilities;
import android.app.time.TimeManager;
import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.TimeDetector;
import android.app.timedetector.TimeDetectorHelper;
import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.DatePicker;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedPreference;

import java.util.Calendar;

public class DatePreferenceController extends BasePreferenceController
        implements DatePickerDialog.OnDateSetListener {

    public interface DatePreferenceHost extends UpdateTimeAndDateCallback {
        void showDatePicker();
    }

    public static final int DIALOG_DATEPICKER = 0;

    private static final String TAG = "DatePreferenceController";

    private DatePreferenceHost mHost;
    private final TimeManager mTimeManager;

    public DatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    public void setHost(DatePreferenceHost host) {
        mHost = host;
    }

    @Override
    public int getAvailabilityStatus() {
        return isEnabled() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference instanceof RestrictedPreference
                && ((RestrictedPreference) preference).isDisabledByAdmin()) {
            return;
        }
        preference.setEnabled(isEnabled());
    }

    @Override
    public CharSequence getSummary() {
        Calendar now = Calendar.getInstance();
        return DateFormat.getLongDateFormat(mContext).format(now.getTime());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            return false;
        }
        mHost.showDatePicker();
        return true;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        setDate(year, month, day);
        mHost.updateTimeAndDateDisplay(mContext);
    }

    /**
     * Builds a {@link DatePickerDialog} that can be used to request the current date from the user.
     */
    public DatePickerDialog buildDatePicker(
            Context parentContext, TimeDetectorHelper timeDetectorHelper) {
        final Calendar calendar = Calendar.getInstance();
        final DatePickerDialog dialog = new DatePickerDialog(
                parentContext,
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Limit the dates the user can pick to a sensible range.
        DatePicker datePicker = dialog.getDatePicker();

        calendar.clear();
        int minYear = timeDetectorHelper.getManualDateSelectionYearMin();
        calendar.set(minYear, Calendar.JANUARY, 1);
        datePicker.setMinDate(calendar.getTimeInMillis());

        int maxYear = timeDetectorHelper.getManualDateSelectionYearMax();
        calendar.clear();
        calendar.set(maxYear, Calendar.DECEMBER, 31);
        datePicker.setMaxDate(calendar.getTimeInMillis());
        return dialog;
    }

    @VisibleForTesting
    void setDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        TimeDetector timeDetector = mContext.getSystemService(TimeDetector.class);
        ManualTimeSuggestion manualTimeSuggestion =
                TimeDetector.createManualTimeSuggestion(when, "Settings: Set date");
        boolean success = timeDetector.suggestManualTime(manualTimeSuggestion);
        if (!success) {
            // This implies the system server is applying tighter bounds than the settings app or
            // the date/time cannot be set for other reasons, e.g. perhaps "auto time" is turned on.
            Log.w(TAG, "Unable to set date with suggestion=" + manualTimeSuggestion);
        }
    }

    /**
     * Returns whether selecting the preference should prompt for the user to enter the date
     * manually. Exposed as public so that the time controller can easily share the same logic as
     * the rules are identical for time.
     */
    public boolean isEnabled() {
        TimeCapabilities timeZoneCapabilities =
                mTimeManager.getTimeCapabilitiesAndConfig().getCapabilities();
        int suggestManualTimeCapability = timeZoneCapabilities.getSetManualTimeCapability();
        return suggestManualTimeCapability == CAPABILITY_POSSESSED;
    }
}
