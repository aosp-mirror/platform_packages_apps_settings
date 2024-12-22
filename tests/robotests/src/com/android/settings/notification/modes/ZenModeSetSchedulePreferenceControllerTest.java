/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
public class ZenModeSetSchedulePreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    @Mock
    private ZenModesBackend mBackend;
    private Context mContext;

    @Mock
    private Fragment mParent;
    @Mock
    private Calendar mCalendar;
    @Mock
    private ViewGroup mDaysContainer;
    @Mock
    private ToggleButton mDay0, mDay1, mDay2, mDay3, mDay4, mDay5, mDay6;

    private ZenModeSetSchedulePreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mPrefController = new ZenModeSetSchedulePreferenceController(mContext, mParent, "schedule",
                mBackend);
        setupMockDayContainer();
    }

    @Test
    @EnableFlags({Flags.FLAG_MODES_API, Flags.FLAG_MODES_UI})
    public void updateScheduleRule_updatesConditionAndTriggerDescription() {
        ZenMode mode = new TestModeBuilder()
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .build();

        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY };
        scheduleInfo.startHour = 1;
        scheduleInfo.endHour = 2;
        ZenMode out = mPrefController.updateScheduleMode(scheduleInfo).apply(mode);

        assertThat(out.getRule().getConditionId())
                .isEqualTo(ZenModeConfig.toScheduleConditionId(scheduleInfo));
        assertThat(out.getRule().getTriggerDescription()).isNotEmpty();
        assertThat(out.getRule().getOwner()).isEqualTo(
                ZenModeConfig.getScheduleConditionProvider());
    }

    @Test
    public void testUpdateScheduleDays() {
        // Confirm that adding/subtracting/etc days works as expected
        // starting from null: no days set
        ZenModeConfig.ScheduleInfo schedule = new ZenModeConfig.ScheduleInfo();

        // Unset a day that's already unset: nothing should change
        assertThat(ZenModeSetSchedulePreferenceController.updateScheduleDays(schedule,
                Calendar.TUESDAY, false)).isFalse();
        // not explicitly checking whether schedule.days is still null here, as we don't necessarily
        // want to require nullness as distinct from an empty list of days.

        // set a few new days
        assertThat(ZenModeSetSchedulePreferenceController.updateScheduleDays(schedule,
                Calendar.MONDAY, true)).isTrue();
        assertThat(ZenModeSetSchedulePreferenceController.updateScheduleDays(schedule,
                Calendar.FRIDAY, true)).isTrue();
        assertThat(schedule.days).hasLength(2);
        assertThat(schedule.days).asList().containsExactly(Calendar.MONDAY, Calendar.FRIDAY);

        // remove an existing day to make sure that works
        assertThat(ZenModeSetSchedulePreferenceController.updateScheduleDays(schedule,
                Calendar.MONDAY, false)).isTrue();
        assertThat(schedule.days).hasLength(1);
        assertThat(schedule.days).asList().containsExactly(Calendar.FRIDAY);
    }

    @Test
    public void testSetupDayToggles_daysOfWeekOrder() {
        // Confirm that days are correctly associated with the actual day of the week independent
        // of when the first day of the week is for the given calendar.
        ZenModeConfig.ScheduleInfo schedule = new ZenModeConfig.ScheduleInfo();
        schedule.days = new int[] { Calendar.SUNDAY, Calendar.TUESDAY, Calendar.FRIDAY };
        schedule.startHour = 1;
        schedule.endHour = 5;

        // Start mCalendar on Wednesday, arbitrarily
        when(mCalendar.getFirstDayOfWeek()).thenReturn(Calendar.WEDNESDAY);

        // Setup the day toggles
        mPrefController.setupDayToggles(mDaysContainer, schedule, mCalendar);

        // we should see toggle 0 associated with the first day of the week, etc.
        // in this week order, schedule turns on friday (2), sunday (4), tuesday (6) so those
        // should be checked while everything else should not be checked.
        verify(mDay0).setChecked(false);  // weds
        verify(mDay1).setChecked(false);  // thurs
        verify(mDay2).setChecked(true);   // fri
        verify(mDay3).setChecked(false);  // sat
        verify(mDay4).setChecked(true);   // sun
        verify(mDay5).setChecked(false);  // mon
        verify(mDay6).setChecked(true);   // tues
    }

    private void setupMockDayContainer() {
        // associate each index (regardless of associated day of the week) with the appropriate
        // res id in the days container
        when(mDaysContainer.findViewById(R.id.day0)).thenReturn(mDay0);
        when(mDaysContainer.findViewById(R.id.day1)).thenReturn(mDay1);
        when(mDaysContainer.findViewById(R.id.day2)).thenReturn(mDay2);
        when(mDaysContainer.findViewById(R.id.day3)).thenReturn(mDay3);
        when(mDaysContainer.findViewById(R.id.day4)).thenReturn(mDay4);
        when(mDaysContainer.findViewById(R.id.day5)).thenReturn(mDay5);
        when(mDaysContainer.findViewById(R.id.day6)).thenReturn(mDay6);
    }
}
