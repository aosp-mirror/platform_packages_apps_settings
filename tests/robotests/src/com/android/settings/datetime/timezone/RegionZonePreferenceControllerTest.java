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

import android.app.Activity;
import android.icu.util.TimeZone;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RegionZonePreferenceControllerTest {

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void updateState_matchTimeZoneName() {
        TimeZoneInfo tzInfo = new TimeZoneInfo.Builder(
                TimeZone.getFrozenTimeZone("America/Los_Angeles"))
                .setGenericName("Pacific Time")
                .setStandardName("Pacific Standard Time")
                .setDaylightName("Pacific Daylight Time")
                .setExemplarLocation("Los Angeles")
                .setGmtOffset("GMT-08:00")
                .build();
        Preference preference = new Preference(mActivity);
        RegionZonePreferenceController controller = new RegionZonePreferenceController(mActivity);
        controller.setTimeZoneInfo(tzInfo);
        controller.setClickable(false);
        controller.updateState(preference);
        String expectedSummary = "Los Angeles (GMT-08:00)";
        assertThat(controller.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(preference.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(preference.isEnabled()).isFalse();
    }
}
