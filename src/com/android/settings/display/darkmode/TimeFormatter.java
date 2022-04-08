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
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats LocalTime to the locale time string format
*/
public class TimeFormatter {
    private final Context mContext;
    private final DateTimeFormatter mFormatter;
    public TimeFormatter(Context context) {
        mContext = context;
        Locale locale = mContext.getResources().getConfiguration().locale;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        mFormatter = DateTimeFormatter.ofPattern("hh:mm a", locale);
    }

    public String of(LocalTime time) {
        return mFormatter.format(time);
    }

    public boolean is24HourFormat() {
        return android.text.format.DateFormat.is24HourFormat(mContext);
    }
}
