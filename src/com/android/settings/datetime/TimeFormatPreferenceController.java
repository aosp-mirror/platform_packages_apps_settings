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

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.util.Calendar;
import java.util.Date;

public class TimeFormatPreferenceController extends TogglePreferenceController {

    static final String HOURS_12 = "12";
    static final String HOURS_24 = "24";

    // Used for showing the current date format, which looks like "12/31/2010", "2010/12/13", etc.
    // The date value is stubs (independent of actual date).
    private final Calendar mDummyDate;
    private boolean mIsFromSUW;
    private UpdateTimeAndDateCallback mUpdateTimeAndDateCallback;

    public TimeFormatPreferenceController(Context context, String key) {
        super(context, key);
        mDummyDate = Calendar.getInstance();
    }

    /**
     * Set the Time and Date callback
     */
    public TimeFormatPreferenceController setTimeAndDateCallback(
            UpdateTimeAndDateCallback callback) {
        mUpdateTimeAndDateCallback = callback;
        return this;
    }

    /**
     * Set if current fragment is launched via SUW
     */
    public TimeFormatPreferenceController setFromSUW(boolean isFromSUW) {
        mIsFromSUW = isFromSUW;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mIsFromSUW) {
            return DISABLED_DEPENDENT_SETTING;
        }
        if (AutoTimeFormatPreferenceController.isAutoTimeFormatSelection(mContext)) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
        refreshSummary(preference);
    }

    @Override
    public boolean isChecked() {
        return is24Hour();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        update24HourFormat(mContext, isChecked);
        mUpdateTimeAndDateCallback.updateTimeAndDateDisplay(mContext);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        final Calendar now = Calendar.getInstance();
        mDummyDate.setTimeZone(now.getTimeZone());
        // We use December 31st because it's unambiguous when demonstrating the date format.
        // We use 13:00 so we can demonstrate the 12/24 hour options.
        mDummyDate.set(now.get(Calendar.YEAR), 11, 31, 13, 0, 0);
        final Date dummyDate = mDummyDate.getTime();
        return DateFormat.getTimeFormat(mContext).format(dummyDate);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(mContext);
    }

    static void update24HourFormat(Context context, Boolean is24Hour) {
        set24Hour(context, is24Hour);
        timeUpdated(context, is24Hour);
    }

    static void timeUpdated(Context context, Boolean is24Hour) {
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        timeChanged.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        int timeFormatPreference;
        if (is24Hour == null) {
            timeFormatPreference = Intent.EXTRA_TIME_PREF_VALUE_USE_LOCALE_DEFAULT;
        } else {
            timeFormatPreference = is24Hour ? Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR
                : Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR;
        }
        timeChanged.putExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, timeFormatPreference);
        context.sendBroadcast(timeChanged);
    }

    static void set24Hour(Context context, Boolean is24Hour) {
        String value = is24Hour == null ? null :
            is24Hour ? HOURS_24 : HOURS_12;
        Settings.System.putString(context.getContentResolver(),
                Settings.System.TIME_12_24, value);
    }
}
