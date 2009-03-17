/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeSettings 
        extends PreferenceActivity 
        implements OnSharedPreferenceChangeListener,
                TimePickerDialog.OnTimeSetListener , DatePickerDialog.OnDateSetListener {

    private static final String HOURS_12 = "12";
    private static final String HOURS_24 = "24";
    
    private Calendar mDummyDate;
    private static final String KEY_DATE_FORMAT = "date_format";
    private static final String KEY_AUTO_TIME = "auto_time";

    private static final int DIALOG_DATEPICKER = 0;
    private static final int DIALOG_TIMEPICKER = 1;
    
    private CheckBoxPreference mAutoPref;
    private Preference mTimePref;
    private Preference mTime24Pref;
    private Preference mTimeZone;
    private Preference mDatePref;
    private ListPreference mDateFormat;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        addPreferencesFromResource(R.xml.date_time_prefs);
        
        initUI();        
    }
    
    private void initUI() {
        boolean autoEnabled = getAutoState();

        mDummyDate = Calendar.getInstance();
        mDummyDate.set(mDummyDate.get(Calendar.YEAR), 11, 31, 13, 0, 0);
        
        mAutoPref = (CheckBoxPreference) findPreference(KEY_AUTO_TIME);
        mAutoPref.setChecked(autoEnabled);
        mTimePref = findPreference("time");
        mTime24Pref = findPreference("24 hour");
        mTimeZone = findPreference("timezone");
        mDatePref = findPreference("date");
        mDateFormat = (ListPreference) findPreference(KEY_DATE_FORMAT);
        
        int currentFormatIndex = -1;
        String [] dateFormats = getResources().getStringArray(R.array.date_format_values);
        String [] formattedDates = new String[dateFormats.length];
        String currentFormat = getDateFormat();
        // Initialize if DATE_FORMAT is not set in the system settings
        // This can happen after a factory reset (or data wipe)
        if (currentFormat == null) {
            currentFormat = getResources().getString(R.string.default_date_format);
            setDateFormat(currentFormat);
        }
        for (int i = 0; i < formattedDates.length; i++) {
            formattedDates[i] = DateFormat.format(dateFormats[i], mDummyDate).toString();
            if (currentFormat.equals(dateFormats[i])) currentFormatIndex = i;
        }
        
        mDateFormat.setEntries(formattedDates);
        mDateFormat.setEntryValues(R.array.date_format_values);
        mDateFormat.setValue(currentFormat);
        
        mTimePref.setEnabled(!autoEnabled);
        mDatePref.setEnabled(!autoEnabled);
        mTimeZone.setEnabled(!autoEnabled);

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);        
    }

    
    @Override
    protected void onResume() {
        super.onResume();

        ((CheckBoxPreference)mTime24Pref).setChecked(is24Hour());

        // Register for time ticks and other reasons for time change
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mIntentReceiver, filter, null, null);
        
        updateTimeAndDateDisplay();
    }

    @Override 
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mIntentReceiver);
    }
    
    private void updateTimeAndDateDisplay() {
        java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(this);
        Date now = Calendar.getInstance().getTime();
        Date dummyDate = mDummyDate.getTime();
        mTimePref.setSummary(DateFormat.getTimeFormat(this).format(now));
        mTimeZone.setSummary(getTimeZoneText());
        mDatePref.setSummary(shortDateFormat.format(now));
        mDateFormat.setSummary(shortDateFormat.format(dummyDate));
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(when);
        }
        updateTimeAndDateDisplay();
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(when);
        }
        updateTimeAndDateDisplay();
        
        // We don't need to call timeUpdated() here because the TIME_CHANGED
        // broadcast is sent by the AlarmManager as a side effect of setting the
        // SystemClock time.
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals(KEY_DATE_FORMAT)) {
            String format = preferences.getString(key, 
                    getResources().getString(R.string.default_date_format));
            Settings.System.putString(getContentResolver(), 
                    Settings.System.DATE_FORMAT, format);
            updateTimeAndDateDisplay();
        } else if (key.equals(KEY_AUTO_TIME)) {
            boolean autoEnabled = preferences.getBoolean(key, true);
            Settings.System.putInt(getContentResolver(), 
                    Settings.System.AUTO_TIME, 
                    autoEnabled ? 1 : 0);
            mTimePref.setEnabled(!autoEnabled);
            mDatePref.setEnabled(!autoEnabled);
            mTimeZone.setEnabled(!autoEnabled);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog d;

        switch (id) {
        case DIALOG_DATEPICKER: {
            final Calendar calendar = Calendar.getInstance();
            d = new DatePickerDialog(
                this,
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
            d.setTitle(getResources().getString(R.string.date_time_changeDate_text));
            break;
        }
        case DIALOG_TIMEPICKER: {
            final Calendar calendar = Calendar.getInstance();
            d = new TimePickerDialog(
                    this,
                    this,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(this));
            d.setTitle(getResources().getString(R.string.date_time_changeTime_text));
            break;
        }
        default:
            d = null;
            break;
        }

        return d;
    }

    @Override
    public void onPrepareDialog(int id, Dialog d) {
        switch (id) {
        case DIALOG_DATEPICKER: {
            DatePickerDialog datePicker = (DatePickerDialog)d;
            final Calendar calendar = Calendar.getInstance();
            datePicker.updateDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            break;
        }
        case DIALOG_TIMEPICKER: {
            TimePickerDialog timePicker = (TimePickerDialog)d;
            final Calendar calendar = Calendar.getInstance();
            timePicker.updateTime(
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
            break;
        }
        default:
            break;
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDatePref) {
            showDialog(DIALOG_DATEPICKER);
        } else if (preference == mTimePref) {
            // The 24-hour mode may have changed, so recreate the dialog
            removeDialog(DIALOG_TIMEPICKER);
            showDialog(DIALOG_TIMEPICKER);
        } else if (preference == mTime24Pref) {
            set24Hour(((CheckBoxPreference)mTime24Pref).isChecked());
            updateTimeAndDateDisplay();
            timeUpdated();
        } else if (preference == mTimeZone) {
            Intent intent = new Intent();
            intent.setClass(this, ZoneList.class);
            startActivityForResult(intent, 0);
        }
        return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        updateTimeAndDateDisplay();
    }
    
    private void timeUpdated() {
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        sendBroadcast(timeChanged);
    }
    
    /*  Get & Set values from the system settings  */
    
    private boolean is24Hour() {
        return DateFormat.is24HourFormat(this);
    }
    
    private void set24Hour(boolean is24Hour) {
        Settings.System.putString(getContentResolver(),
                Settings.System.TIME_12_24,
                is24Hour? HOURS_24 : HOURS_12);
    }
    
    private String getDateFormat() {
        return Settings.System.getString(getContentResolver(), 
                Settings.System.DATE_FORMAT);
    }
    
    private boolean getAutoState() {
        try {
            return Settings.System.getInt(getContentResolver(), 
                Settings.System.AUTO_TIME) > 0;            
        } catch (SettingNotFoundException snfe) {
            return true;
        }
    }

    private void setDateFormat(String format) {
        Settings.System.putString(getContentResolver(), Settings.System.DATE_FORMAT, format);        
    }
    
    /*  Helper routines to format timezone */
    
    private String getTimeZoneText() {
        TimeZone    tz = java.util.Calendar.getInstance().getTimeZone();
        boolean daylight = tz.inDaylightTime(new Date());
        StringBuilder sb = new StringBuilder();

        sb.append(formatOffset(tz.getRawOffset() +
                               (daylight ? tz.getDSTSavings() : 0))).
            append(", ").
            append(tz.getDisplayName(daylight, TimeZone.LONG));

        return sb.toString();        
    }

    private char[] formatOffset(int off) {
        off = off / 1000 / 60;

        char[] buf = new char[9];
        buf[0] = 'G';
        buf[1] = 'M';
        buf[2] = 'T';

        if (off < 0) {
            buf[3] = '-';
            off = -off;
        } else {
            buf[3] = '+';
        }

        int hours = off / 60; 
        int minutes = off % 60;

        buf[4] = (char) ('0' + hours / 10);
        buf[5] = (char) ('0' + hours % 10);

        buf[6] = ':';

        buf[7] = (char) ('0' + minutes / 10);
        buf[8] = (char) ('0' + minutes % 10);

        return buf;
    }
    
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTimeAndDateDisplay();
        }
    };
}
