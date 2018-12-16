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

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.icu.util.TimeZone;

import com.android.settings.datetime.timezone.model.TimeZoneData;

import com.google.common.truth.Truth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { BaseTimeZoneInfoPickerTest.ShadowDataFormat.class })
public class BaseTimeZoneInfoPickerTest {
    @Implements(android.text.format.DateFormat.class)
    public static class ShadowDataFormat {

        private static String sTimeFormatString = "";

        @Implementation
        protected static String getTimeFormatString(Context context) {
            return sTimeFormatString;
        }
    }

    /**
     * Verify the summary, title, and time label in a time zone item are formatted properly.
     */
    @Test
    public void createAdapter_matchTimeZoneInfoAndOrder() {
        ShadowDataFormat.sTimeFormatString = "HH:MM";
        BaseTimeZoneInfoPicker picker = new TestBaseTimeZoneInfoPicker();
        BaseTimeZoneAdapter adapter = picker.createAdapter(mock(TimeZoneData.class));
        Truth.assertThat(adapter.getItemCount()).isEqualTo(2);

        BaseTimeZoneAdapter.AdapterItem item1 = adapter.getDataItem(0);
        Truth.assertThat(item1.getTitle().toString()).isEqualTo("Los Angeles");
        Truth.assertThat(item1.getSummary().toString()).isEqualTo("Pacific Time (GMT-08:00)");
        Truth.assertThat(item1.getCurrentTime())
                .hasLength(ShadowDataFormat.sTimeFormatString.length());

        BaseTimeZoneAdapter.AdapterItem item2 = adapter.getDataItem(1);
        Truth.assertThat(item2.getTitle().toString()).isEqualTo("New York");
        Truth.assertThat(item2.getSummary().toString()).isEqualTo("Eastern Time (GMT-05:00)");
        Truth.assertThat(item2.getCurrentTime())
                .hasLength(ShadowDataFormat.sTimeFormatString.length());
    }

    public static class TestBaseTimeZoneInfoPicker extends BaseTimeZoneInfoPicker {

        private TestBaseTimeZoneInfoPicker() {
            super(0, 0, false, false);
        }

        @Override
        public List<TimeZoneInfo> getAllTimeZoneInfos(TimeZoneData timeZoneData) {
            TimeZoneInfo zone1 = new TimeZoneInfo.Builder(
                    TimeZone.getFrozenTimeZone("America/Los_Angeles"))
                    .setGenericName("Pacific Time")
                    .setStandardName("Pacific Standard Time")
                    .setDaylightName("Pacific Daylight Time")
                    .setExemplarLocation("Los Angeles")
                    .setGmtOffset("GMT-08:00")
                    .build();
            TimeZoneInfo zone2 = new TimeZoneInfo.Builder(
                    TimeZone.getFrozenTimeZone("America/New_York"))
                    .setGenericName("Eastern Time")
                    .setStandardName("Eastern Standard Time")
                    .setDaylightName("Eastern Daylight Time")
                    .setExemplarLocation("New York")
                    .setGmtOffset("GMT-05:00")
                    .build();

            return Arrays.asList(zone1, zone2);
        }

        // Make the method public
        @Override
        public BaseTimeZoneAdapter createAdapter(TimeZoneData timeZoneData) {
            return super.createAdapter(timeZoneData);
        }

        @Override
        protected Locale getLocale() {
            return Locale.US;
        }

        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }

        @Override
        public int getMetricsCategory() {
            // the metric id doesn't matter in test
            return 1;
        }
    }
}
