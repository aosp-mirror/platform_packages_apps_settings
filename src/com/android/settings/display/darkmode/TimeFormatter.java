/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import android.content.Context;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Formats LocalTime to the locale time string format
*/
public class TimeFormatter {
    private final Context mContext;
    private final java.text.DateFormat mFormatter;
    public TimeFormatter(Context context) {
        mContext = context;
        mFormatter = android.text.format.DateFormat.getTimeFormat(context);
        mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public String of(LocalTime time) {
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(mFormatter.getTimeZone());
        c.set(Calendar.HOUR_OF_DAY, time.getHour());
        c.set(Calendar.MINUTE, time.getMinute());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return mFormatter.format(c.getTime());
    }

    public boolean is24HourFormat() {
        return android.text.format.DateFormat.is24HourFormat(mContext);
    }
}
