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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settings.datetime.timezone.TimeZoneInfo.Formatter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class TimeZoneInfoPreferenceControllerTest {

    private TimeZoneInfo mTimeZoneInfo;
    private TimeZoneInfoPreferenceController mController;

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        final Date now = new Date(0L); // 00:00 1/1/1970
        final Formatter formatter = new Formatter(Locale.US, now);
        mTimeZoneInfo = formatter.format("America/Los_Angeles");
        mController = new TimeZoneInfoPreferenceController(context, "key");
        mController.mDate = now;
        mController.setTimeZoneInfo(mTimeZoneInfo);
    }

    @Test
    public void getSummary_matchExpectedFormattedText() {
        assertThat(mController.getSummary().toString()).isEqualTo(
                "Uses Pacific Time (GMT-08:00). "
                        + "Pacific Daylight Time starts on April 26, 1970.");
    }

    @Test
    public void getAvailabilityStatus_timeZoneInfoSet_shouldReturnAVAILABLE_UNSEARCHABLE() {
        mController.setTimeZoneInfo(mTimeZoneInfo);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_noTimeZoneInfoSet_shouldReturnUNSUPPORTED_ON_DEVICE() {
        mController.setTimeZoneInfo(null);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
