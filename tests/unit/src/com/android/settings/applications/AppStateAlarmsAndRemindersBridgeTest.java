/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.app.AlarmManager;
import android.content.Context;
import android.os.UserHandle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AppStateAlarmsAndRemindersBridgeTest {
    private static final String TEST_PACKAGE_1 = "com.example.test.1";
    private static final String TEST_PACKAGE_2 = "com.example.test.2";
    private static final int UID_1 = 12345;
    private static final int UID_2 = 7654321;

    @Mock
    private AlarmManager mAlarmManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBeVisible_permissionRequestedIsTrue_isTrue() {
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                true /* permissionRequested */,
                true /* permissionGranted */)
                .shouldBeVisible()).isTrue();
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                true /* permissionRequested */,
                false /* permissionGranted */)
                .shouldBeVisible()).isTrue();
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                false /* permissionRequested */,
                true /* permissionGranted */)
                .shouldBeVisible()).isFalse();
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                false /* permissionRequested */,
                false /* permissionGranted */)
                .shouldBeVisible()).isFalse();
    }

    @Test
    public void isAllowed_permissionGrantedIsTrue_isTrue() {
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                true /* permissionRequested */,
                true /* permissionGranted */)
                .isAllowed()).isTrue();
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                true /* permissionRequested */,
                false /* permissionGranted */)
                .isAllowed()).isFalse();
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                false /* permissionRequested */,
                true /* permissionGranted */)
                .isAllowed()).isTrue();
        assertThat(new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                false /* permissionRequested */,
                false /* permissionGranted */)
                .isAllowed()).isFalse();
    }

    @Test
    public void createPermissionState() {
        AppStateAlarmsAndRemindersBridge bridge = new AppStateAlarmsAndRemindersBridge(mContext,
                null, null);
        bridge.mAlarmManager = mAlarmManager;
        bridge.mRequesterPackages = new String[]{TEST_PACKAGE_1, "some.other.package"};

        doReturn(false).when(mAlarmManager).hasScheduleExactAlarm(TEST_PACKAGE_1,
                UserHandle.getUserId(UID_1));
        doReturn(true).when(mAlarmManager).hasScheduleExactAlarm(TEST_PACKAGE_2,
                UserHandle.getUserId(UID_2));

        AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state1 =
                bridge.createPermissionState(TEST_PACKAGE_1, UID_1);
        assertThat(state1.shouldBeVisible()).isTrue();
        assertThat(state1.isAllowed()).isFalse();

        AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state2 =
                bridge.createPermissionState(TEST_PACKAGE_2, UID_2);
        assertThat(state2.shouldBeVisible()).isFalse();
        assertThat(state2.isAllowed()).isTrue();
    }
}
