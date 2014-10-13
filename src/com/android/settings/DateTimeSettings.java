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

import android.app.admin.DevicePolicyManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeSettings extends SettingsPreferenceFragment
        implements OnSharedPreferenceChangeListener,
                TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private static final String HOURS_12 = "12";
    private static final String HOURS_24 = "24";

    // Used for showing the current date format, which looks like "12/31/2010", "2010/12/13", etc.
    // The date value is dummy (independent of actual date).
    private Calendar mDummyDate;

    private static final String KEY_DATE_FORMAT = "date_format";
    private static final String KEY_AUTO_TIME = "auto_time";
    private static final String KEY_AUTO_TIME_ZONE = "auto_zone";

    private static final int DIALOG_DATEPICKER = 0;
    private static final int DIALOG_TIMEPICKER = 1;

    // have we been launched from the setup wizard?
    protected static final String EXTRA_IS_FIRST_RUN = "firstRun";

    private SwitchPreference mAutoTimePref;
    private Preference mTimePref;
    private Preference mTime24Pref;
    private SwitchPreference mAutoTimeZonePref;
    private Preference mTimeZone;
    private Preference mDatePref;
    private ListPreference mDateFormat;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.date_time_prefs);

        initUI();
    }

    private void initUI() {
        boolean autoTimeEnabled = getAutoState(Settings.Global.AUTO_TIME);
        boolean autoTimeZoneEnabled = getAutoState(Settings.Global.AUTO_TIME_ZONE);

        mAutoTimePref = (SwitchPreference) findPreference(KEY_AUTO_TIME);

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context
                .DEVICE_POLICY_SERVICE);
        if (dpm.getAutoTimeRequired()) {
            mAutoTimePref.setEnabled(false);

            // If Settings.Global.AUTO_TIME is false it will be set to true
            // by the device policy manager very soon.
            // Note that this app listens to that change.
        }

        Intent intent = getActivity().getIntent();
        boolean isFirstRun = intent.getBooleanExtra(EXTRA_IS_FIRST_RUN, false);

        mDummyDate = Calendar.getInstance();

        mAutoTimePref.setChecked(autoTimeEnabled);
        mAutoTimeZonePref = (SwitchPreference) findPreference(KEY_AUTO_TIME_ZONE);
        // Override auto-timezone if it's a wifi-only device or if we're still in setup wizard.
        // TODO: Remove the wifiOnly test when auto-timezone is implemented based on wifi-location.
        if (Utils.isWifiOnly(getActivity()) || isFirstRun) {
            getPreferenceScreen().removePreference(mAutoTimeZonePref);
            autoTimeZoneEnabled = false;
        }
        mAutoTimeZonePref.setChecked(autoTimeZoneEnabled);

        mTimePref = findPreference("time");
        mTime24Pref = findPreference("24 hour");
        mTimeZone = findPreference("timezone");
        mDatePref = findPreference("date");
        mDateFormat = (ListPreference) findPreference(KEY_DATE_FORMAT);
        if (isFirstRun) {
            getPreferenceScreen().removePreference(mTime24Pref);
            getPreferenceScreen().removePreference(mDateFormat);
        }

        String [] dateFormats = getResources().getStringArray(R.array.date_format_values);
        String [] formattedDates = new String[dateFormats.length];
        String currentFormat = getDateFormat();
        // Initialize if DATE_FORMAT is not set in the system settings
        // This can happen after a factory reset (or data wipe)
        if (currentFormat == null) {
            currentFormat = "";
        }

        // Prevents duplicated values on date format selector.
        mDummyDate.set(mDummyDate.get(Calendar.YEAR), mDummyDate.DECEMBER, 31, 13, 0, 0);

        for (int i = 0; i < formattedDates.length; i++) {
            String formatted =
                    DateFormat.getDateFormatForSetting(getActivity(), dateFormats[i])
                    .format(mDummyDate.getTime());

            if (dateFormats[i].length() == 0) {
                formattedDates[i] = getResources().
                    getString(R.string.normal_date_format, formatted);
            } else {
                formattedDates[i] = formatted;
            }
        }

        mDateFormat.setEntries(formattedDates);
        mDateFormat.setEntryValues(R.array.date_format_values);
        mDateFormat.setValue(currentFormat);

        mTimePref.setEnabled(!autoTimeEnabled);
        mDatePref.setEnabled(!autoTimeEnabled);
        mTimeZone.setEnabled(!autoTimeZoneEnabled);
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        ((SwitchPreference)mTime24Pref).setChecked(is24Hour());

        // Register for time ticks and other reasons for time change
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getActivity().registerReceiver(mIntentReceiver, filter, null, null);

        updateTimeAndDateDisplay(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mIntentReceiver);
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void updateTimeAndDateDisplay(Context context) {
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

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        final Activity activity = getActivity();
        if (activity != null) {
            setDate(activity, year, month, day);
            updateTimeAndDateDisplay(activity);
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        final Activity activity = getActivity();
        if (activity != null) {
            setTime(activity, hourOfDay, minute);
            updateTimeAndDateDisplay(activity);
        }

        // We don't need to call timeUpdated() here because the TIME_CHANGED
        // broadcast is sent by the AlarmManager as a side effect of setting the
        // SystemClock time.
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals(KEY_DATE_FORMAT)) {
            String format = preferences.getString(key,
                    getResources().getString(R.string.default_date_format));
            Settings.System.putString(getContentResolver(),
                    Settings.System.DATE_FORMAT, format);
            updateTimeAndDateDisplay(getActivity());
        } else if (key.equals(KEY_AUTO_TIME)) {
            boolean autoEnabled = preferences.getBoolean(key, true);
            Settings.Global.putInt(getContentResolver(), Settings.Global.AUTO_TIME,
                    autoEnabled ? 1 : 0);
            mTimePref.setEnabled(!autoEnabled);
            mDatePref.setEnabled(!autoEnabled);
        } else if (key.equals(KEY_AUTO_TIME_ZONE)) {
            boolean autoZoneEnabled = preferences.getBoolean(key, true);
            Settings.Global.putInt(
                    getContentResolver(), Settings.Global.AUTO_TIME_ZONE, autoZoneEnabled ? 1 : 0);
            mTimeZone.setEnabled(!autoZoneEnabled);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        final Calendar calendar = Calendar.getInstance();
        switch (id) {
        case DIALOG_DATEPICKER:
            DatePickerDialog d = new DatePickerDialog(
                    getActivity(),
                    this,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            configureDatePicker(d.getDatePicker());
            return d;
        case DIALOG_TIMEPICKER:
            return new TimePickerDialog(
                    getActivity(),
                    this,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(getActivity()));
        default:
            throw new IllegalArgumentException();
        }
    }

    static void configureDatePicker(DatePicker datePicker) {
        // The system clock can't represent dates outside this range.
        Calendar t = Calendar.getInstance();
        t.clear();
        t.set(1970, Calendar.JANUARY, 1);
        datePicker.setMinDate(t.getTimeInMillis());
        t.clear();
        t.set(2037, Calendar.DECEMBER, 31);
        datePicker.setMaxDate(t.getTimeInMillis());
    }

    /*
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
    */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDatePref) {
            showDialog(DIALOG_DATEPICKER);
        } else if (preference == mTimePref) {
            // The 24-hour mode may have changed, so recreate the dialog
            removeDialog(DIALOG_TIMEPICKER);
            showDialog(DIALOG_TIMEPICKER);
        } else if (preference == mTime24Pref) {
            final boolean is24Hour = ((SwitchPreference)mTime24Pref).isChecked();
            set24Hour(is24Hour);
            updateTimeAndDateDisplay(getActivity());
            timeUpdated(is24Hour);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        updateTimeAndDateDisplay(getActivity());
    }

    private void timeUpdated(boolean is24Hour) {
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        timeChanged.putExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, is24Hour);
        getActivity().sendBroadcast(timeChanged);
    }

    /*  Get & Set values from the system settings  */

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(getActivity());
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

    private boolean getAutoState(String name) {
        try {
            return Settings.Global.getInt(getContentResolver(), name) > 0;
        } catch (SettingNotFoundException snfe) {
            return false;
        }
    }

    /* package */ static void setDate(Context context, int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    /* package */ static void setTime(Context context, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    public static String getTimeZoneText(TimeZone tz, boolean includeName) {
        Date now = new Date();

        // Use SimpleDateFormat to format the GMT+00:00 string.
        SimpleDateFormat gmtFormatter = new SimpleDateFormat("ZZZZ");
        gmtFormatter.setTimeZone(tz);
        String gmtString = gmtFormatter.format(now);

        // Ensure that the "GMT+" stays with the "00:00" even if the digits are RTL.
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        Locale l = Locale.getDefault();
        boolean isRtl = TextUtils.getLayoutDirectionFromLocale(l) == View.LAYOUT_DIRECTION_RTL;
        gmtString = bidiFormatter.unicodeWrap(gmtString,
                isRtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR);

        if (!includeName) {
            return gmtString;
        }

        // Optionally append the time zone name.
        SimpleDateFormat zoneNameFormatter = new SimpleDateFormat("zzzz");
        zoneNameFormatter.setTimeZone(tz);
        String zoneNameString = zoneNameFormatter.format(now);

        // We don't use punctuation here to avoid having to worry about localizing that too!
        return gmtString + " " + zoneNameString;
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Activity activity = getActivity();
            if (activity != null) {
                updateTimeAndDateDisplay(activity);
            }
        }
    };
}
