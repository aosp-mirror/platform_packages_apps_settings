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
import static android.service.notification.ZenModeConfig.EventInfo.REPLY_YES;

import static com.android.settings.notification.modes.ZenModeSetCalendarPreferenceController.CALENDAR_NAME;
import static com.android.settings.notification.modes.ZenModeSetCalendarPreferenceController.KEY_CALENDAR;
import static com.android.settings.notification.modes.ZenModeSetCalendarPreferenceController.KEY_REPLY;
import static com.android.settings.notification.modes.ZenModeSetCalendarPreferenceController.addCalendar;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.SystemZenRules;
import android.service.notification.ZenModeConfig;

import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModeSetCalendarPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    @Mock
    private ZenModesBackend mBackend;
    private Context mContext;

    @Mock
    private PreferenceCategory mPrefCategory;
    private DropDownPreference mCalendar, mReply;

    private ZenModeSetCalendarPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mCalendar = new DropDownPreference(mContext);
        mReply = new DropDownPreference(mContext);
        when(mPrefCategory.findPreference(KEY_CALENDAR)).thenReturn(mCalendar);
        when(mPrefCategory.findPreference(KEY_REPLY)).thenReturn(mReply);

        mPrefController = new ZenModeSetCalendarPreferenceController(mContext,
                "zen_mode_event_category", mBackend);
    }

    @Test
    @EnableFlags({Flags.FLAG_MODES_API, Flags.FLAG_MODES_UI})
    public void updateEventMode_updatesConditionAndTriggerDescription() {
        ZenMode mode = new TestModeBuilder()
                .setPackage(SystemZenRules.PACKAGE_ANDROID)
                .build();

        // Explicitly update preference controller with mode info first, which will also call
        // updateState()
        mPrefController.updateZenMode(mPrefCategory, mode);

        ZenModeConfig.EventInfo eventInfo = new ZenModeConfig.EventInfo();
        eventInfo.calendarId = 1L;
        eventInfo.calName = "My events";

        // apply event mode updater to existing mode
        ZenMode out = mPrefController.updateEventMode(eventInfo).apply(mode);

        assertThat(out.getRule().getOwner()).isEqualTo(ZenModeConfig.getEventConditionProvider());
        assertThat(out.getRule().getConditionId()).isEqualTo(
                ZenModeConfig.toEventConditionId(eventInfo));
        assertThat(out.getRule().getTriggerDescription()).isEqualTo("My events");
    }

    @Test
    public void updateState_setsPreferenceValues() {
        ZenModeConfig.EventInfo eventInfo = new ZenModeConfig.EventInfo();
        eventInfo.calendarId = 1L;
        eventInfo.calName = "Definitely A Calendar";
        eventInfo.reply = REPLY_YES;

        ZenMode mode = new TestModeBuilder()
                .setConditionId(ZenModeConfig.toEventConditionId(eventInfo))
                .build();
        mPrefController.updateZenMode(mPrefCategory, mode);

        // We should see mCalendar, mReply have their values set
        assertThat(mCalendar.getValue()).isEqualTo(
                ZenModeSetCalendarPreferenceController.key(eventInfo.userId, eventInfo.calendarId,
                        eventInfo.calName));
        assertThat(mReply.getValue()).isEqualTo(Integer.toString(eventInfo.reply));
    }

    @Test
    public void testNoDuplicateCalendars() {
        List<ZenModeSetCalendarPreferenceController.CalendarInfo> calendarsList = new ArrayList<>();
        addCalendar(1234, "calName", 1, calendarsList);
        addCalendar(1234, "calName", 2, calendarsList);
        addCalendar(1234, "calName", 3, calendarsList);
        assertThat(calendarsList).hasSize(1);
    }

    @Test
    public void testCalendarInfoSortByName() {
        List<ZenModeSetCalendarPreferenceController.CalendarInfo> calendarsList = new ArrayList<>();
        addCalendar(123, "zyx", 1, calendarsList);
        addCalendar(456, "wvu", 2, calendarsList);
        addCalendar(789, "abc", 3, calendarsList);
        Collections.sort(calendarsList, CALENDAR_NAME);

        List<ZenModeSetCalendarPreferenceController.CalendarInfo> sortedList = new ArrayList<>();
        addCalendar(789, "abc", 3, sortedList);
        addCalendar(456, "wvu", 2, sortedList);
        addCalendar(123, "zyx", 1, sortedList);

        assertThat(calendarsList).containsExactlyElementsIn(sortedList).inOrder();
    }
}
