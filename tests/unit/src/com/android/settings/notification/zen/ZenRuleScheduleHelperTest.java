/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.notification.zen;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.service.notification.ZenModeConfig.ScheduleInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Calendar;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class ZenRuleScheduleHelperTest {
    private ZenRuleScheduleHelper mScheduleHelper;
    private ScheduleInfo mScheduleInfo;

    private Context mContext;

    @Mock
    private Resources mResources;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        // explicitly initialize to Locale.US just for ease of explicitly testing the string values
        // of the days of the week if the test locale doesn't happen to be in the US
        mScheduleHelper = new ZenRuleScheduleHelper(Locale.US);
        mScheduleInfo = new ScheduleInfo();

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getResources()).thenReturn(mResources);

        // Resources will be called upon to join strings together, either to get a divider
        // or a combination of two strings. Conveniently, these have different signatures.
        // Divider method calls getString(string divider id)
        when(mResources.getString(anyInt())).thenReturn(",");

        // Combination method calls getString(combination id, first item, second item)
        // and returns "first - second"
        when(mResources.getString(anyInt(), anyString(), anyString())).thenAnswer(
                invocation -> {
                    return invocation.getArgument(1).toString() // first item
                            + "-"
                            + invocation.getArgument(2).toString();  // second item
                });

        // for locale used in time format
        Configuration config = new Configuration();
        config.setLocales(new LocaleList(Locale.US));
        when(mResources.getConfiguration()).thenReturn(config);
    }

    @Test
    public void getDaysDescription() {
        // Test various cases of where the days are set.
        // No days
        mScheduleInfo.days = new int[] {};
        assertThat(mScheduleHelper.getDaysDescription(mContext, mScheduleInfo)).isNull();

        // one day
        mScheduleInfo.days = new int[] {Calendar.FRIDAY};
        assertThat(mScheduleHelper.getDaysDescription(mContext, mScheduleInfo)).isEqualTo("Fri");

        // Monday through Friday
        mScheduleInfo.days = new int[] {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY};
        assertThat(mScheduleHelper.getDaysDescription(mContext, mScheduleInfo))
                .isEqualTo("Mon,Tue,Wed,Thu,Fri");

        // Some scattered days of the week
        mScheduleInfo.days = new int[] {Calendar.SUNDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.SATURDAY};
        assertThat(mScheduleHelper.getDaysDescription(mContext, mScheduleInfo))
                .isEqualTo("Sun,Wed,Thu,Sat");
    }

    @Test
    public void getShortDaysSummary_noOrSingleDays() {
        // Test various cases for grouping and not-grouping of days.
        // No days
        mScheduleInfo.days = new int[]{};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo)).isNull();

        // A single day at the beginning of the week
        mScheduleInfo.days = new int[]{Calendar.SUNDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo)).isEqualTo("Sun");

        // A single day in the middle of the week
        mScheduleInfo.days = new int[]{Calendar.THURSDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo)).isEqualTo("Thu");

        // A single day at the end of the week
        mScheduleInfo.days = new int[]{Calendar.SATURDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo)).isEqualTo("Sat");
    }

    @Test
    public void getShortDaysSummary_oneGroup() {
        // The whole week
        mScheduleInfo.days = new int[] {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo))
                .isEqualTo("Sun-Sat");

        // Various cases of one big group
        // Sunday through Thursday
        mScheduleInfo.days = new int[] {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo))
                .isEqualTo("Sun-Thu");

        // Wednesday through Saturday
        mScheduleInfo.days = new int[] {Calendar.WEDNESDAY, Calendar.THURSDAY,
                Calendar.FRIDAY, Calendar.SATURDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo))
                .isEqualTo("Wed-Sat");

        // Monday through Friday
        mScheduleInfo.days = new int[] {Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo))
                .isEqualTo("Mon-Fri");
    }

    @Test
    public void getShortDaysSummary_mixed() {
        // cases combining groups and single days scattered around
        mScheduleInfo.days = new int[] {Calendar.SUNDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.SATURDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo))
                .isEqualTo("Sun,Tue-Thu,Sat");

        mScheduleInfo.days = new int[] {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.FRIDAY, Calendar.SATURDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo))
                .isEqualTo("Sun-Wed,Fri-Sat");

        mScheduleInfo.days = new int[] {Calendar.MONDAY, Calendar.WEDNESDAY,
                Calendar.FRIDAY, Calendar.SATURDAY};
        assertThat(mScheduleHelper.getShortDaysSummary(mContext, mScheduleInfo))
                .isEqualTo("Mon,Wed,Fri-Sat");
    }

    @Test
    public void getDaysAndTimeSummary() {
        // Combination days & time settings
        // No days, no output, even if the times are set.
        mScheduleInfo.startHour = 10;
        mScheduleInfo.endHour = 16;
        mScheduleInfo.days = new int[]{};
        assertThat(mScheduleHelper.getDaysAndTimeSummary(mContext, mScheduleInfo)).isNull();

        // If there are days then they are combined with the time combination
        mScheduleInfo.days = new int[]{Calendar.SUNDAY, Calendar.MONDAY, Calendar.WEDNESDAY};
        assertThat(mScheduleHelper.getDaysAndTimeSummary(mContext, mScheduleInfo))
                .isEqualTo("Sun-Mon,Wed,10:00 AM-4:00 PM");
    }
}
