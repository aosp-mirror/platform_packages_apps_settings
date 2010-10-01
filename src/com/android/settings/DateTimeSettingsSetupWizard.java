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

import com.android.settings.ZonePicker.ZoneSelectionListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.TimeZone;

public class DateTimeSettingsSetupWizard extends Activity
        implements OnClickListener, ZoneSelectionListener, OnCheckedChangeListener{
    private static final String TAG = DateTimeSettingsSetupWizard.class.getSimpleName();

    // force the first status of auto datetime flag.
    private static final String EXTRA_INITIAL_AUTO_DATETIME_VALUE =
        "extra_initial_auto_datetime_value";

    private boolean mXLargeScreenSize;

    /* Available only in XL */
    private CompoundButton mAutoDateTimeButton;
    // private CompoundButton mAutoTimeZoneButton;
    private Button mTimeZone;
    private TimePicker mTimePicker;
    private DatePicker mDatePicker;
    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.date_time_settings_setupwizard);
        mXLargeScreenSize = (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE;
        if (mXLargeScreenSize) {
            initUiForXl();
        } else {
            findViewById(R.id.next_button).setOnClickListener(this);
        }
    }

    public void initUiForXl() {
        // Currently just comment out codes related to auto timezone.
        // TODO: Remove them when we are sure they are unnecessary.
        /*
        final boolean autoTimeZoneEnabled = isAutoTimeZoneEnabled();
        mAutoTimeZoneButton = (CompoundButton)findViewById(R.id.time_zone_auto);
        mAutoTimeZoneButton.setChecked(autoTimeZoneEnabled);
        mAutoTimeZoneButton.setOnCheckedChangeListener(this);
        mAutoTimeZoneButton.setText(autoTimeZoneEnabled ? R.string.zone_auto_summaryOn :
                R.string.zone_auto_summaryOff);*/

        final TimeZone tz = TimeZone.getDefault();
        mTimeZone = (Button)findViewById(R.id.current_time_zone);
        mTimeZone.setText(DateTimeSettings.getTimeZoneText(tz));
        mTimeZone.setOnClickListener(this);
        // mTimeZone.setEnabled(!autoTimeZoneEnabled);

        final boolean autoDateTimeEnabled;
        final Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_INITIAL_AUTO_DATETIME_VALUE)) {
            autoDateTimeEnabled = intent.getBooleanExtra(EXTRA_INITIAL_AUTO_DATETIME_VALUE, false);
        } else {
            autoDateTimeEnabled = isAutoDateTimeEnabled();
        }

        mAutoDateTimeButton = (CompoundButton)findViewById(R.id.date_time_auto);
        mAutoDateTimeButton.setChecked(autoDateTimeEnabled);
        mAutoDateTimeButton.setText(autoDateTimeEnabled ? R.string.date_time_auto_summaryOn :
                R.string.date_time_auto_summaryOff);
        mAutoDateTimeButton.setOnCheckedChangeListener(this);

        mTimePicker = (TimePicker)findViewById(R.id.time_picker);
        mTimePicker.setEnabled(!autoDateTimeEnabled);
        mDatePicker = (DatePicker)findViewById(R.id.date_picker);
        mDatePicker.setEnabled(!autoDateTimeEnabled);

        mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        ((ZonePicker)getFragmentManager().findFragmentById(R.id.zone_picker_fragment))
                .setZoneSelectionListener(this);

        ((Button)findViewById(R.id.next_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.skip_button)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.current_time_zone: {
            findViewById(R.id.current_time_zone).setVisibility(View.GONE);
            findViewById(R.id.zone_picker).setVisibility(View.VISIBLE);
            break;
        }
        case R.id.next_button: {
            if (mXLargeScreenSize) {
                /* Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME_ZONE,
                        mAutoTimeZoneButton.isChecked() ? 1 : 0); */
                Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME,
                        mAutoDateTimeButton.isChecked() ? 1 : 0);
                // Note: in non-XL, Date & Time is stored by DatePickerDialog/TimePickerDialog,
                // so we don't need to save those values there, while in XL, we need to as
                // we don't use those Dialogs.
                DateTimeSettings.setDate(mDatePicker.getYear(), mDatePicker.getMonth(),
                        mDatePicker.getDayOfMonth());
                DateTimeSettings.setTime(
                        mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
            }
        }  // $FALL-THROUGH$
        case R.id.skip_button: {
            setResult(RESULT_OK);
            finish();
            break;
        }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final boolean autoEnabled = isChecked;  // just for readibility.
        /*if (buttonView == mAutoTimeZoneButton) {
            // In XL screen, we save all the state only when the next button is pressed.
            if (!mXLargeScreenSize) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.AUTO_TIME_ZONE,
                        isChecked ? 1 : 0);
            }
            mTimeZone.setEnabled(!autoEnabled);
            if (isChecked) {
                findViewById(R.id.current_time_zone).setVisibility(View.VISIBLE);
                findViewById(R.id.zone_picker).setVisibility(View.GONE);
            }
        } else */
        if (buttonView == mAutoDateTimeButton) {
            if (!mXLargeScreenSize) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.AUTO_TIME,
                        isChecked ? 1 : 0);
            }
            mTimePicker.setEnabled(!autoEnabled);
            mDatePicker.setEnabled(!autoEnabled);
        }
        if (autoEnabled) {
            final View focusedView = getCurrentFocus();
            if (focusedView != null) {
                mInputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                focusedView.clearFocus();
            }
        }
    }

    @Override
    public void onZoneSelected(TimeZone tz) {
        findViewById(R.id.current_time_zone).setVisibility(View.VISIBLE);
        findViewById(R.id.zone_picker).setVisibility(View.GONE);
        final Calendar now = Calendar.getInstance(tz);
        mTimeZone.setText(DateTimeSettings.getTimeZoneText(tz));
        mDatePicker.updateDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        mTimePicker.setCurrentHour(now.get(Calendar.HOUR));
        mTimePicker.setCurrentMinute(now.get(Calendar.MINUTE));
    }

    private boolean isAutoDateTimeEnabled() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.AUTO_TIME) > 0;
        } catch (SettingNotFoundException e) {
            return true;
        }
    }

    /*
    private boolean isAutoTimeZoneEnabled() {
        try {
            return Settings.System.getInt(getContentResolver(),
                    Settings.System.AUTO_TIME_ZONE) > 0;
        } catch (SettingNotFoundException e) {
            return true;
        }
    }*/
}
