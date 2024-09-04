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

import static android.service.notification.SystemZenRules.PACKAGE_ANDROID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenModeConfig;

import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeExitAtAlarmPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    @Mock
    private ZenModesBackend mBackend;

    private ZenModeExitAtAlarmPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mPrefController = new ZenModeExitAtAlarmPreferenceController(mContext, "exit_at_alarm",
                mBackend);
    }

    @Test
    public void testUpdateState() {
        TwoStatePreference preference = mock(TwoStatePreference.class);

        // previously: don't exit at alarm
        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY };
        scheduleInfo.startHour = 1;
        scheduleInfo.endHour = 2;
        scheduleInfo.exitAtAlarm = false;

        ZenMode mode = new TestModeBuilder()
                .setPackage(PACKAGE_ANDROID)
                .setConditionId(ZenModeConfig.toScheduleConditionId(scheduleInfo))
                .build();

        // need to call updateZenMode for the first call
        mPrefController.updateZenMode(preference, mode);
        verify(preference).setChecked(false);

        // Now update state after changing exitAtAlarm
        scheduleInfo.exitAtAlarm = true;
        mode.getRule().setConditionId(ZenModeConfig.toScheduleConditionId(scheduleInfo));

        // now can just call updateState
        mPrefController.updateState(preference, mode);
        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange() {
        TwoStatePreference preference = mock(TwoStatePreference.class);

        // previously: exit at alarm
        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY };
        scheduleInfo.startHour = 1;
        scheduleInfo.endHour = 2;
        scheduleInfo.exitAtAlarm = true;

        ZenMode mode = new TestModeBuilder()
                .setPackage(PACKAGE_ANDROID)
                .setConditionId(ZenModeConfig.toScheduleConditionId(scheduleInfo))
                .build();
        mPrefController.updateZenMode(preference, mode);

        // turn off exit at alarm
        mPrefController.onPreferenceChange(preference, false);
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        ZenModeConfig.ScheduleInfo newSchedule = ZenModeConfig.tryParseScheduleConditionId(
                captor.getValue().getRule().getConditionId());
        assertThat(newSchedule.exitAtAlarm).isFalse();

        // other properties remain the same
        assertThat(newSchedule.startHour).isEqualTo(1);
        assertThat(newSchedule.endHour).isEqualTo(2);
    }
}
