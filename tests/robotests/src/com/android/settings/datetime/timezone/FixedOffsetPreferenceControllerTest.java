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
public class FixedOffsetPreferenceControllerTest {

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void updateState_GmtMinus8_matchTimeZoneSummary() {
        TimeZoneInfo fixedOffsetZone = new TimeZoneInfo.Builder(
                    TimeZone.getFrozenTimeZone("Etc/GMT-8"))
                    .setGmtOffset("GMT-08:00")
                    .build();
        Preference preference = new Preference(mActivity);
        FixedOffsetPreferenceController controller = new FixedOffsetPreferenceController(mActivity);
        controller.setTimeZoneInfo(fixedOffsetZone);
        controller.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("GMT-08:00");
    }

    @Test
    public void updateState_Utc_matchTimeZoneSummary() {
        TimeZoneInfo fixedOffsetZone = new TimeZoneInfo.Builder(
                    TimeZone.getFrozenTimeZone("Etc/UTC"))
                    .setStandardName("Coordinated Universal Time")
                    .setGmtOffset("GMT+00:00")
                    .build();
        Preference preference = new Preference(mActivity);
        FixedOffsetPreferenceController controller = new FixedOffsetPreferenceController(mActivity);
        controller.setTimeZoneInfo(fixedOffsetZone);
        controller.updateState(preference);
        assertThat(preference.getSummary().toString())
                .isEqualTo("Coordinated Universal Time (GMT+00:00)");
    }
}
