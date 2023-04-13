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
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.PowerExemptionManager;
import android.os.UserHandle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import libcore.util.EmptyArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AppStateAlarmsAndRemindersBridgeTest {
    private static final String TEST_PACKAGE = "com.example.test.1";
    private static final int TEST_UID = 12345;

    @Mock
    private AlarmManager mAlarmManager;
    @Mock
    private PowerExemptionManager mPowerExemptionManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void alarmsAndRemindersState_shouldBeVisible() {
        boolean seaPermissionRequested;
        boolean ueaPermissionRequested;
        boolean seaPermissionGranted;
        boolean allowListed;

        for (int i = 0; i < (1 << 4); i++) {
            seaPermissionRequested = (i & 1) != 0;
            ueaPermissionRequested = (i & (1 << 1)) != 0;
            seaPermissionGranted = (i & (1 << 2)) != 0;
            allowListed = (i & (1 << 3)) != 0;

            final boolean visible = new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                    seaPermissionRequested,
                    ueaPermissionRequested,
                    seaPermissionGranted,
                    allowListed).shouldBeVisible();

            assertWithMessage("Wrong return value " + visible
                    + " for {seaPermissionRequested = " + seaPermissionRequested
                    + ", ueaPermissionRequested = " + ueaPermissionRequested
                    + ", seaPermissionGranted = " + seaPermissionGranted
                    + ", allowListed = " + allowListed + "}")
                    .that(visible)
                    .isEqualTo(seaPermissionRequested && !ueaPermissionRequested && !allowListed);
        }
    }

    @Test
    public void alarmsAndRemindersState_isAllowed() {
        boolean seaPermissionRequested;
        boolean ueaPermissionRequested;
        boolean seaPermissionGranted;
        boolean allowListed;

        for (int i = 0; i < (1 << 4); i++) {
            seaPermissionRequested = (i & 1) != 0;
            ueaPermissionRequested = (i & (1 << 1)) != 0;
            seaPermissionGranted = (i & (1 << 2)) != 0;
            allowListed = (i & (1 << 3)) != 0;

            final boolean allowed = new AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState(
                    seaPermissionRequested,
                    ueaPermissionRequested,
                    seaPermissionGranted,
                    allowListed).isAllowed();

            assertWithMessage("Wrong return value " + allowed
                    + " for {seaPermissionRequested = " + seaPermissionRequested
                    + ", ueaPermissionRequested = " + ueaPermissionRequested
                    + ", seaPermissionGranted = " + seaPermissionGranted
                    + ", allowListed = " + allowListed + "}")
                    .that(allowed)
                    .isEqualTo(seaPermissionGranted || ueaPermissionRequested || allowListed);
        }
    }

    private PackageInfo createPackageInfoWithPermissions(String... requestedPermissions) {
        final PackageInfo info = new PackageInfo();
        info.requestedPermissions = requestedPermissions;
        return info;
    }

    @Test
    public void createPermissionState_SeaGrantedNoUeaNoAllowlist() throws Exception {
        AppStateAlarmsAndRemindersBridge bridge = new AppStateAlarmsAndRemindersBridge(mContext,
                null, null);
        bridge.mAlarmManager = mAlarmManager;
        bridge.mPackageManager = mPackageManager;
        bridge.mPowerExemptionManager = mPowerExemptionManager;

        doReturn(true).when(mAlarmManager).hasScheduleExactAlarm(TEST_PACKAGE,
                UserHandle.getUserId(TEST_UID));
        doReturn(createPackageInfoWithPermissions(Manifest.permission.SCHEDULE_EXACT_ALARM))
                .when(mPackageManager).getPackageInfoAsUser(eq(TEST_PACKAGE), anyInt(), anyInt());
        doReturn(false).when(mPowerExemptionManager).isAllowListed(TEST_PACKAGE, true);

        AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state =
                bridge.createPermissionState(TEST_PACKAGE, TEST_UID);
        assertThat(state.shouldBeVisible()).isTrue();
        assertThat(state.isAllowed()).isTrue();
    }

    @Test
    public void createPermissionState_requestsBothSeaDeniedNoAllowlist() throws Exception {
        AppStateAlarmsAndRemindersBridge bridge = new AppStateAlarmsAndRemindersBridge(mContext,
                null, null);
        bridge.mAlarmManager = mAlarmManager;
        bridge.mPackageManager = mPackageManager;
        bridge.mPowerExemptionManager = mPowerExemptionManager;

        doReturn(false).when(mAlarmManager).hasScheduleExactAlarm(TEST_PACKAGE,
                UserHandle.getUserId(TEST_UID));
        doReturn(createPackageInfoWithPermissions(
                Manifest.permission.SCHEDULE_EXACT_ALARM,
                Manifest.permission.USE_EXACT_ALARM))
                .when(mPackageManager).getPackageInfoAsUser(eq(TEST_PACKAGE), anyInt(), anyInt());
        doReturn(false).when(mPowerExemptionManager).isAllowListed(TEST_PACKAGE, true);

        AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state =
                bridge.createPermissionState(TEST_PACKAGE, TEST_UID);
        assertThat(state.shouldBeVisible()).isFalse();
        assertThat(state.isAllowed()).isTrue();
    }

    @Test
    public void createPermissionState_requestsNoneNoAllowlist() throws Exception {
        AppStateAlarmsAndRemindersBridge bridge = new AppStateAlarmsAndRemindersBridge(mContext,
                null, null);
        bridge.mAlarmManager = mAlarmManager;
        bridge.mPackageManager = mPackageManager;
        bridge.mPowerExemptionManager = mPowerExemptionManager;

        doReturn(false).when(mAlarmManager).hasScheduleExactAlarm(TEST_PACKAGE,
                UserHandle.getUserId(TEST_UID));
        doReturn(createPackageInfoWithPermissions(EmptyArray.STRING))
                .when(mPackageManager).getPackageInfoAsUser(eq(TEST_PACKAGE), anyInt(), anyInt());
        doReturn(false).when(mPowerExemptionManager).isAllowListed(TEST_PACKAGE, true);

        AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state =
                bridge.createPermissionState(TEST_PACKAGE, TEST_UID);
        assertThat(state.shouldBeVisible()).isFalse();
        assertThat(state.isAllowed()).isFalse();
    }

    @Test
    public void createPermissionState_requestsOnlyUeaNoAllowlist() throws Exception {
        AppStateAlarmsAndRemindersBridge bridge = new AppStateAlarmsAndRemindersBridge(mContext,
                null, null);
        bridge.mAlarmManager = mAlarmManager;
        bridge.mPackageManager = mPackageManager;
        bridge.mPowerExemptionManager = mPowerExemptionManager;

        doReturn(false).when(mAlarmManager).hasScheduleExactAlarm(TEST_PACKAGE,
                UserHandle.getUserId(TEST_UID));
        doReturn(createPackageInfoWithPermissions(Manifest.permission.USE_EXACT_ALARM))
                .when(mPackageManager).getPackageInfoAsUser(eq(TEST_PACKAGE), anyInt(), anyInt());
        doReturn(false).when(mPowerExemptionManager).isAllowListed(TEST_PACKAGE, true);

        AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state =
                bridge.createPermissionState(TEST_PACKAGE, TEST_UID);
        assertThat(state.shouldBeVisible()).isFalse();
        assertThat(state.isAllowed()).isTrue();
    }

    @Test
    public void createPermissionState_requestsNoneButAllowlisted() throws Exception {
        AppStateAlarmsAndRemindersBridge bridge = new AppStateAlarmsAndRemindersBridge(mContext,
                null, null);
        bridge.mAlarmManager = mAlarmManager;
        bridge.mPackageManager = mPackageManager;
        bridge.mPowerExemptionManager = mPowerExemptionManager;

        doReturn(false).when(mAlarmManager).hasScheduleExactAlarm(TEST_PACKAGE,
                UserHandle.getUserId(TEST_UID));
        doReturn(createPackageInfoWithPermissions(EmptyArray.STRING))
                .when(mPackageManager).getPackageInfoAsUser(eq(TEST_PACKAGE), anyInt(), anyInt());
        doReturn(true).when(mPowerExemptionManager).isAllowListed(TEST_PACKAGE, true);

        AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state =
                bridge.createPermissionState(TEST_PACKAGE, TEST_UID);
        assertThat(state.shouldBeVisible()).isFalse();
        assertThat(state.isAllowed()).isTrue();
    }
}
