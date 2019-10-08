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

import android.icu.text.Collator;

import com.android.settings.datetime.timezone.RegionZonePicker.TimeZoneInfoComparator;
import com.android.settings.datetime.timezone.TimeZoneInfo.Formatter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RegionZonePickerTest {

    @Test
    public void compareTimeZoneInfo_matchGmtOrder() {
        Date now = new Date(0); // 00:00 1, Jan 1970
        Formatter formatter = new Formatter(Locale.US, now);
        TimeZoneInfo timeZone1 = formatter.format("Pacific/Honolulu");
        TimeZoneInfo timeZone2 = formatter.format("America/Los_Angeles");
        TimeZoneInfo timeZone3 = formatter.format("America/Indiana/Marengo");
        TimeZoneInfo timeZone4 = formatter.format("America/New_York");

        TimeZoneInfoComparator comparator =
                new TimeZoneInfoComparator(Collator.getInstance(Locale.US), now);

        // Verify the sorted order
        List<TimeZoneInfo> list = Arrays.asList(timeZone4, timeZone2, timeZone3, timeZone1);
        Collections.sort(list, comparator);
        assertThat(list).isEqualTo(Arrays.asList(timeZone1, timeZone2, timeZone3, timeZone4));
    }
}
