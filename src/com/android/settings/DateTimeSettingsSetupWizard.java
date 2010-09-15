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
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.TimeZone;

public class DateTimeSettingsSetupWizard extends Activity
        implements OnClickListener, ZoneSelectionListener, OnCheckedChangeListener{

    private boolean mXLargeScreenSize;

    /* Available only in XL */
    private Button mTimeZone;
    private TimePicker mTimePicker;
    private DatePicker mDatePicker;

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
        // TODO: use system value
        final boolean autoTimeZoneEnabled = false;
        final CompoundButton autoTimeZoneButton =
                (CompoundButton)findViewById(R.id.time_zone_auto);
        autoTimeZoneButton.setChecked(autoTimeZoneEnabled);
        autoTimeZoneButton.setOnCheckedChangeListener(this);
        // TODO: remove this after the system support.
        autoTimeZoneButton.setEnabled(false);

        final boolean autoDateTimeEnabled = isAutoDateTimeEnabled();
        final CompoundButton autoDateTimeButton =
                (CompoundButton)findViewById(R.id.date_time_auto);
        autoDateTimeButton.setChecked(autoDateTimeEnabled);
        autoDateTimeButton.setText(autoDateTimeEnabled ? R.string.date_time_auto_summaryOn :
                R.string.date_time_auto_summaryOff);
        autoDateTimeButton.setOnCheckedChangeListener(this);

        final TimeZone tz = TimeZone.getDefault();
        mTimeZone = (Button)findViewById(R.id.current_time_zone);
        mTimeZone.setText(DateTimeSettings.getTimeZoneText(tz));
        mTimeZone.setOnClickListener(this);
        mTimeZone.setEnabled(!autoDateTimeEnabled);

        mTimePicker = (TimePicker)findViewById(R.id.time_picker);
        mTimePicker.setEnabled(!autoDateTimeEnabled);
        mDatePicker = (DatePicker)findViewById(R.id.date_picker);
        mDatePicker.setEnabled(!autoDateTimeEnabled);

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
        Settings.System.putInt(getContentResolver(),
                Settings.System.AUTO_TIME,
                isChecked ? 1 : 0);
        if (isChecked) {
            findViewById(R.id.current_time_zone).setVisibility(View.VISIBLE);
            findViewById(R.id.zone_picker).setVisibility(View.GONE);
        }
        mTimeZone.setEnabled(!autoEnabled);
        mTimePicker.setEnabled(!autoEnabled);
        mDatePicker.setEnabled(!autoEnabled);
        if (autoEnabled) {
            mTimePicker.clearFocus();
            mDatePicker.clearFocus();
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
}
