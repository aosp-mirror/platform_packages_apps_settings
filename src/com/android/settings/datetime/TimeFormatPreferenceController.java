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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Calendar;
import java.util.Date;

public class TimeFormatPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    static final String HOURS_12 = "12";
    static final String HOURS_24 = "24";

    private static final String KEY_TIME_FORMAT = "24 hour";

    // Used for showing the current date format, which looks like "12/31/2010", "2010/12/13", etc.
    // The date value is dummy (independent of actual date).
    private final Calendar mDummyDate;
    private final boolean mIsFromSUW;
    private final UpdateTimeAndDateCallback mUpdateTimeAndDateCallback;

    public TimeFormatPreferenceController(Context context, UpdateTimeAndDateCallback callback,
            boolean isFromSUW) {
        super(context);
        mIsFromSUW = isFromSUW;
        mDummyDate = Calendar.getInstance();
        mUpdateTimeAndDateCallback = callback;
    }

    @Override
    public boolean isAvailable() {
        return !mIsFromSUW;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof TwoStatePreference)) {
            return;
        }
        ((TwoStatePreference) preference).setChecked(is24Hour());
        final Calendar now = Calendar.getInstance();
        mDummyDate.setTimeZone(now.getTimeZone());
        // We use December 31st because it's unambiguous when demonstrating the date format.
        // We use 13:00 so we can demonstrate the 12/24 hour options.
        mDummyDate.set(now.get(Calendar.YEAR), 11, 31, 13, 0, 0);
        final Date dummyDate = mDummyDate.getTime();
        preference.setSummary(DateFormat.getTimeFormat(mContext).format(dummyDate));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof TwoStatePreference)
                || !TextUtils.equals(KEY_TIME_FORMAT, preference.getKey())) {
            return false;
        }
        final boolean is24Hour = ((SwitchPreference) preference).isChecked();
        set24Hour(is24Hour);
        timeUpdated(is24Hour);
        mUpdateTimeAndDateCallback.updateTimeAndDateDisplay(mContext);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TIME_FORMAT;
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(mContext);
    }

    private void timeUpdated(boolean is24Hour) {
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        int timeFormatPreference =
                is24Hour ? Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR
                        : Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR;
        timeChanged.putExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, timeFormatPreference);
        mContext.sendBroadcast(timeChanged);
    }

    private void set24Hour(boolean is24Hour) {
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.TIME_12_24,
                is24Hour ? HOURS_24 : HOURS_12);
    }
}
