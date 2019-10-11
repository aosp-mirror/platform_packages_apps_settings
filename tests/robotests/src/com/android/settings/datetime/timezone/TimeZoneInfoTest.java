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

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.datetime.timezone.TimeZoneInfo.Formatter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.Locale;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TimeZoneInfoTest {

    @Test
    public void testFormat() {
        Date now = new Date(0L); // 00:00 1/1/1970
        Formatter formatter = new Formatter(Locale.US, now);

        TimeZoneInfo timeZoneInfo = formatter.format("America/Los_Angeles");
        assertThat(timeZoneInfo.getId()).isEqualTo("America/Los_Angeles");
        assertThat(timeZoneInfo.getExemplarLocation()).isEqualTo("Los Angeles");
        assertThat(timeZoneInfo.getGmtOffset().toString()).isEqualTo("GMT-08:00");
        assertThat(timeZoneInfo.getGenericName()).isEqualTo("Pacific Time");
        assertThat(timeZoneInfo.getStandardName()).isEqualTo("Pacific Standard Time");
        assertThat(timeZoneInfo.getDaylightName()).isEqualTo("Pacific Daylight Time");
    }

    @Test
    public void getGmtOffset_zoneLordHowe_correctGmtOffset() {
        Date date = new Date(1514764800000L); // 00:00 1/1/2018 GMT
        Formatter formatter = new Formatter(Locale.US, date);

        TimeZoneInfo timeZoneInfo = formatter.format("Australia/Lord_Howe");
        assertThat(timeZoneInfo.getGmtOffset().toString()).isEqualTo("GMT+11:00");
    }
}
