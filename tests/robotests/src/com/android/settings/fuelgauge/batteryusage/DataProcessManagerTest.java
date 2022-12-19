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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class DataProcessManagerTest {
    private Context mContext;
    private DataProcessManager mDataProcessManager;

    @Mock
    private IUsageStatsManager mUsageStatsManager;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        DataProcessor.sUsageStatsManager = mUsageStatsManager;
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mUserManager)
                .when(mContext)
                .getSystemService(UserManager.class);

        mDataProcessManager = new DataProcessManager(
                mContext, /*handler=*/ null,  /*callbackFunction=*/ null,
                /*hourlyBatteryLevelsPerDay=*/ null, /*batteryHistoryMap=*/ null);
    }

    @Test
    public void start_loadExpectedCurrentAppUsageData() throws RemoteException {
        final UsageEvents.Event event1 =
                getUsageEvent(UsageEvents.Event.ACTIVITY_RESUMED, /*timestamp=*/ 1);
        final UsageEvents.Event event2 =
                getUsageEvent(UsageEvents.Event.ACTIVITY_STOPPED, /*timestamp=*/ 2);
        final List<UsageEvents.Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);
        doReturn(getUsageEvents(events))
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), any());
        doReturn(true).when(mUserManager).isUserUnlocked(anyInt());

        mDataProcessManager.start();

        assertThat(mDataProcessManager.getIsCurrentAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsDatabaseAppUsageLoaded()).isTrue();
        assertThat(mDataProcessManager.getIsCurrentBatteryHistoryLoaded()).isTrue();
        assertThat(mDataProcessManager.getShowScreenOnTime()).isTrue();
        final List<AppUsageEvent> appUsageEventList = mDataProcessManager.getAppUsageEventList();
        assertThat(appUsageEventList.size()).isEqualTo(2);
        assertAppUsageEvent(
                appUsageEventList.get(0), AppUsageEventType.ACTIVITY_RESUMED, /*timestamp=*/ 1);
        assertAppUsageEvent(
                appUsageEventList.get(1), AppUsageEventType.ACTIVITY_STOPPED, /*timestamp=*/ 2);
    }

    @Test
    public void start_currentUserLocked_emptyAppUsageList() throws RemoteException {
        final UsageEvents.Event event =
                getUsageEvent(UsageEvents.Event.ACTIVITY_RESUMED, /*timestamp=*/ 1);
        final List<UsageEvents.Event> events = new ArrayList<>();
        events.add(event);
        doReturn(getUsageEvents(events))
                .when(mUsageStatsManager)
                .queryEventsForUser(anyLong(), anyLong(), anyInt(), any());
        doReturn(false).when(mUserManager).isUserUnlocked(anyInt());

        mDataProcessManager.start();

        assertThat(mDataProcessManager.getAppUsageEventList()).isEmpty();
        assertThat(mDataProcessManager.getShowScreenOnTime()).isFalse();
    }

    private UsageEvents getUsageEvents(final List<UsageEvents.Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, new String[] {"package"});
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }

    private UsageEvents.Event getUsageEvent(
            final int eventType, final long timestamp) {
        final UsageEvents.Event event = new UsageEvents.Event();
        event.mEventType = eventType;
        event.mPackage = "package";
        event.mTimeStamp = timestamp;
        return event;
    }

    private void assertAppUsageEvent(
            final AppUsageEvent event, final AppUsageEventType eventType, final long timestamp) {
        assertThat(event.getType()).isEqualTo(eventType);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }
}
