/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.datetime.timezone;

import android.content.Context;
import android.support.v7.preference.Preference;

public class FixedOffsetPreferenceController extends BaseTimeZonePreferenceController {

    private static final String PREFERENCE_KEY = "fixed_offset";

    private TimeZoneInfo mTimeZoneInfo;

    public FixedOffsetPreferenceController(Context context) {
        super(context, PREFERENCE_KEY);
    }

    @Override
    public CharSequence getSummary() {
        // This is a Spannable object, which contains TTS span. It shouldn't be converted to String.
        return mTimeZoneInfo == null ? "" : mTimeZoneInfo.getGmtOffset();
    }

    public void setTimeZoneInfo(TimeZoneInfo timeZoneInfo) {
        mTimeZoneInfo = timeZoneInfo;
    }

    public TimeZoneInfo getTimeZoneInfo() {
        return mTimeZoneInfo;
    }
}

