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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

import androidx.preference.Preference;

import com.android.settings.datetime.timezone.TimeZoneInfo.Formatter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class TimeZoneInfoPreferenceControllerTest {

    @Test
    public void updateState_matchExpectedFormattedText() {
        Date now = new Date(0L); // 00:00 1/1/1970
        Formatter formatter = new Formatter(Locale.US, now);

        TimeZoneInfo timeZoneInfo = formatter.format("America/Los_Angeles");
        TimeZoneInfoPreferenceController controller =
                new TimeZoneInfoPreferenceController(RuntimeEnvironment.application);
        controller.mDate = now;
        controller.setTimeZoneInfo(timeZoneInfo);
        Preference preference = spy(new Preference(RuntimeEnvironment.application));
        controller.updateState(preference);
        assertEquals("Uses Pacific Time (GMT-08:00). "
                        + "Pacific Daylight Time starts on April 26, 1970.",
                preference.getTitle().toString());
    }
}
