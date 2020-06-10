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
 * limitations under the License
 */

package com.android.settings.applications;

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import static com.android.settings.applications.AppStateNotificationBridge
        .FILTER_APP_NOTIFICATION_BLOCKED;
import static com.android.settings.applications.AppStateNotificationBridge
        .FILTER_APP_NOTIFICATION_FREQUENCY;
import static com.android.settings.applications.AppStateNotificationBridge
        .FILTER_APP_NOTIFICATION_RECENCY;
import static com.android.settings.applications.AppStateNotificationBridge
        .FREQUENCY_NOTIFICATION_COMPARATOR;
import static com.android.settings.applications.AppStateNotificationBridge
        .RECENT_NOTIFICATION_COMPARATOR;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.applications.AppStateNotificationBridge.NotificationsSentState;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class AppStateNotificationBridgeTest {

    private static String PKG1 = "pkg1";
    private static String PKG2 = "pkg2";

    @Mock
    private ApplicationsState.Session mSession;
    @Mock
    private ApplicationsState mState;
    @Mock
    private IUsageStatsManager mUsageStats;
    @Mock
    private UserManager mUserManager;
    @Mock
    private NotificationBackend mBackend;
    private Context mContext;
    private AppStateNotificationBridge mBridge;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mState.newSession(any())).thenReturn(mSession);
        when(mState.getBackgroundLooper()).thenReturn(mock(Looper.class));
        when(mBackend.getNotificationsBanned(anyString(), anyInt())).thenReturn(true);
        when(mBackend.isSystemApp(any(), any())).thenReturn(true);
        // most tests assume no work profile
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        mContext = RuntimeEnvironment.application.getApplicationContext();

        mBridge = new AppStateNotificationBridge(mContext, mState,
                mock(AppStateBaseBridge.Callback.class), mUsageStats, mUserManager, mBackend);
    }

    private AppEntry getMockAppEntry(String pkg) {
        AppEntry entry = mock(AppEntry.class);
        entry.info = mock(ApplicationInfo.class);
        entry.info.packageName = pkg;
        return entry;
    }

    private UsageEvents getUsageEvents(List<Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, new String[] {PKG1, PKG2});
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }

    @Test
    public void testGetAggregatedUsageEvents_noEvents() throws Exception {
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(mock(UsageEvents.class));

        assertThat(mBridge.getAggregatedUsageEvents()).isEmpty();
    }

    @Test
    public void testGetAggregatedUsageEvents_onlyNotificationEvents() throws Exception {
        List<Event> events = new ArrayList<>();
        Event good = new Event();
        good.mEventType = Event.NOTIFICATION_INTERRUPTION;
        good.mPackage = PKG1;
        good.mTimeStamp = 1;
        events.add(good);
        Event bad = new Event();
        bad.mEventType = Event.CHOOSER_ACTION;
        bad.mPackage = PKG1;
        bad.mTimeStamp = 2;
        events.add(bad);

        UsageEvents usageEvents = getUsageEvents(events);
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        Map<String, NotificationsSentState> map = mBridge.getAggregatedUsageEvents();
        assertThat(map.get(AppStateNotificationBridge.getKey(0, PKG1)).sentCount).isEqualTo(1);
    }

    @Test
    public void testGetAggregatedUsageEvents_multipleEventsAgg() throws Exception {
        List<Event> events = new ArrayList<>();
        Event good = new Event();
        good.mEventType = Event.NOTIFICATION_INTERRUPTION;
        good.mPackage = PKG1;
        good.mTimeStamp = 6;
        events.add(good);
        Event good1 = new Event();
        good1.mEventType = Event.NOTIFICATION_INTERRUPTION;
        good1.mPackage = PKG1;
        good1.mTimeStamp = 1;
        events.add(good1);

        UsageEvents usageEvents = getUsageEvents(events);
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        Map<String, NotificationsSentState> map  = mBridge.getAggregatedUsageEvents();
        assertThat(map.get(AppStateNotificationBridge.getKey(0, PKG1)).sentCount).isEqualTo(2);
        assertThat(map.get(AppStateNotificationBridge.getKey(0, PKG1)).lastSent).isEqualTo(6);
    }

    @Test
    public void testGetAggregatedUsageEvents_multiplePkgs() throws Exception {
        List<Event> events = new ArrayList<>();
        Event good = new Event();
        good.mEventType = Event.NOTIFICATION_INTERRUPTION;
        good.mPackage = PKG1;
        good.mTimeStamp = 6;
        events.add(good);
        Event good1 = new Event();
        good1.mEventType = Event.NOTIFICATION_INTERRUPTION;
        good1.mPackage = PKG2;
        good1.mTimeStamp = 1;
        events.add(good1);

        UsageEvents usageEvents = getUsageEvents(events);
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        Map<String, NotificationsSentState> map
                = mBridge.getAggregatedUsageEvents();
        assertThat(map.get(AppStateNotificationBridge.getKey(0, PKG1)).sentCount).isEqualTo(1);
        assertThat(map.get(AppStateNotificationBridge.getKey(0, PKG2)).sentCount).isEqualTo(1);
        assertThat(map.get(AppStateNotificationBridge.getKey(0, PKG1)).lastSent).isEqualTo(6);
        assertThat(map.get(AppStateNotificationBridge.getKey(0, PKG2)).lastSent).isEqualTo(1);
    }

    @Test
    public void testLoadAllExtraInfo_noEvents() throws RemoteException {
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(mock(UsageEvents.class));
        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(getMockAppEntry(PKG1));
        when(mSession.getAllApps()).thenReturn(apps);

        mBridge.loadAllExtraInfo();
        // extra info should exist and blocked status should be populated
        assertThat(apps.get(0).extraInfo).isNotNull();
        verify(mBackend).getNotificationsBanned(PKG1, 0);
        // but the recent/frequent counts should be 0 so they don't appear on those screens
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).avgSentDaily).isEqualTo(0);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).lastSent).isEqualTo(0);
    }

    @Test
    public void testLoadAllExtraInfo_multipleEventsAgg() throws RemoteException {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Event good = new Event();
            good.mEventType = Event.NOTIFICATION_INTERRUPTION;
            good.mPackage = PKG1;
            good.mTimeStamp = i;
            events.add(good);
        }

        UsageEvents usageEvents = getUsageEvents(events);
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(getMockAppEntry(PKG1));
        when(mSession.getAllApps()).thenReturn(apps);

        mBridge.loadAllExtraInfo();
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).sentCount).isEqualTo(7);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).lastSent).isEqualTo(6);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).avgSentDaily).isEqualTo(1);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).avgSentWeekly).isEqualTo(0);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).blocked).isTrue();
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).systemApp).isTrue();
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).blockable).isTrue();
    }

    @Test
    public void testLoadAllExtraInfo_multiplePkgs() throws RemoteException {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Event good = new Event();
            good.mEventType = Event.NOTIFICATION_INTERRUPTION;
            good.mPackage = PKG1;
            good.mTimeStamp = i;
            events.add(good);
        }
        Event good1 = new Event();
        good1.mEventType = Event.NOTIFICATION_INTERRUPTION;
        good1.mPackage = PKG2;
        good1.mTimeStamp = 1;
        events.add(good1);

        UsageEvents usageEvents = getUsageEvents(events);
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(getMockAppEntry(PKG1));
        apps.add(getMockAppEntry(PKG2));
        when(mSession.getAllApps()).thenReturn(apps);

        mBridge.loadAllExtraInfo();
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).sentCount).isEqualTo(8);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).lastSent).isEqualTo(7);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).avgSentWeekly).isEqualTo(0);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).avgSentDaily).isEqualTo(1);

        assertThat(((NotificationsSentState) apps.get(1).extraInfo).sentCount).isEqualTo(1);
        assertThat(((NotificationsSentState) apps.get(1).extraInfo).lastSent).isEqualTo(1);
        assertThat(((NotificationsSentState) apps.get(1).extraInfo).avgSentWeekly).isEqualTo(1);
        assertThat(((NotificationsSentState) apps.get(1).extraInfo).avgSentDaily).isEqualTo(0);
    }

    @Test
    public void testLoadAllExtraInfo_multipleUsers() throws RemoteException {
        // has work profile
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{1});
        mBridge = new AppStateNotificationBridge(mContext, mState,
                mock(AppStateBaseBridge.Callback.class), mUsageStats, mUserManager, mBackend);

        List<Event> eventsProfileOwner = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Event good = new Event();
            good.mEventType = Event.NOTIFICATION_INTERRUPTION;
            good.mPackage = PKG1;
            good.mTimeStamp = i;
            eventsProfileOwner.add(good);
        }

        List<Event> eventsProfile = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Event good = new Event();
            good.mEventType = Event.NOTIFICATION_INTERRUPTION;
            good.mPackage = PKG1;
            good.mTimeStamp = i;
            eventsProfile.add(good);
        }

        UsageEvents usageEventsOwner = getUsageEvents(eventsProfileOwner);
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), eq(0), anyString()))
                .thenReturn(usageEventsOwner);

        UsageEvents usageEventsProfile = getUsageEvents(eventsProfile);
        when(mUsageStats.queryEventsForUser(anyLong(), anyLong(), eq(1), anyString()))
                .thenReturn(usageEventsProfile);

        ArrayList<AppEntry> apps = new ArrayList<>();
        AppEntry owner = getMockAppEntry(PKG1);
        owner.info.uid = 1;
        apps.add(owner);

        AppEntry profile = getMockAppEntry(PKG1);
        profile.info.uid = UserHandle.PER_USER_RANGE + 1;
        apps.add(profile);
        when(mSession.getAllApps()).thenReturn(apps);

        mBridge.loadAllExtraInfo();

        assertThat(((NotificationsSentState) apps.get(0).extraInfo).sentCount).isEqualTo(8);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).lastSent).isEqualTo(7);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).avgSentWeekly).isEqualTo(0);
        assertThat(((NotificationsSentState) apps.get(0).extraInfo).avgSentDaily).isEqualTo(1);

        assertThat(((NotificationsSentState) apps.get(1).extraInfo).sentCount).isEqualTo(4);
        assertThat(((NotificationsSentState) apps.get(1).extraInfo).lastSent).isEqualTo(3);
        assertThat(((NotificationsSentState) apps.get(1).extraInfo).avgSentWeekly).isEqualTo(4);
        assertThat(((NotificationsSentState) apps.get(1).extraInfo).avgSentDaily).isEqualTo(1);
    }

    @Test
    public void testUpdateExtraInfo_noEvents() throws RemoteException {
        when(mUsageStats.queryEventsForPackageForUser(
                anyLong(), anyLong(), anyInt(), anyString(), anyString()))
                .thenReturn(mock(UsageEvents.class));
        AppEntry entry = getMockAppEntry(PKG1);

        mBridge.updateExtraInfo(entry, "", 0);
        assertThat(entry.extraInfo).isNull();
    }

    @Test
    public void testUpdateExtraInfo_multipleEventsAgg() throws RemoteException {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Event good = new Event();
            good.mEventType = Event.NOTIFICATION_INTERRUPTION;
            good.mPackage = PKG1;
            good.mTimeStamp = i;
            events.add(good);
        }

        UsageEvents usageEvents = getUsageEvents(events);
        when(mUsageStats.queryEventsForPackageForUser(
                anyLong(), anyLong(), anyInt(), anyString(), anyString())).thenReturn(usageEvents);

        AppEntry entry = getMockAppEntry(PKG1);
        mBridge.updateExtraInfo(entry, "", 0);

        assertThat(((NotificationsSentState) entry.extraInfo).sentCount).isEqualTo(13);
        assertThat(((NotificationsSentState) entry.extraInfo).lastSent).isEqualTo(12);
        assertThat(((NotificationsSentState) entry.extraInfo).avgSentDaily).isEqualTo(2);
        assertThat(((NotificationsSentState) entry.extraInfo).avgSentWeekly).isEqualTo(0);
        assertThat(((NotificationsSentState) entry.extraInfo).blocked).isTrue();
        assertThat(((NotificationsSentState) entry.extraInfo).systemApp).isTrue();
        assertThat(((NotificationsSentState) entry.extraInfo).blockable).isTrue();
    }

    @Test
    public void testSummary_recency() {
        NotificationsSentState neverSent = new NotificationsSentState();
        NotificationsSentState sent = new NotificationsSentState();
        sent.lastSent = System.currentTimeMillis() - (2 * DAY_IN_MILLIS);

        assertThat(AppStateNotificationBridge.getSummary(
                mContext, neverSent, R.id.sort_order_recent_notification)).isEqualTo(
                        mContext.getString(R.string.notifications_sent_never));
        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sent, R.id.sort_order_recent_notification).toString()).contains("2");
    }

    @Test
    public void testSummary_frequency() {
        NotificationsSentState sentRarely = new NotificationsSentState();
        sentRarely.avgSentWeekly = 1;
        NotificationsSentState sentOften = new NotificationsSentState();
        sentOften.avgSentDaily = 8;

        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sentRarely, R.id.sort_order_frequent_notification).toString())
                .contains("1");
        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sentRarely, R.id.sort_order_frequent_notification).toString())
                .contains("notification ");
        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sentRarely, R.id.sort_order_frequent_notification).toString())
                .contains("week");
        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sentOften, R.id.sort_order_frequent_notification).toString())
                .contains("8");
        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sentOften, R.id.sort_order_frequent_notification).toString())
                .contains("notifications");
        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sentOften, R.id.sort_order_frequent_notification).toString())
                .contains("day");
    }

    @Test
    public void testSummary_alpha() {
        NotificationsSentState sentRarely = new NotificationsSentState();
        sentRarely.avgSentWeekly = 1;
        assertThat(AppStateNotificationBridge.getSummary(
                mContext, sentRarely, R.id.sort_order_alpha).toString())
                .isEqualTo("");
    }

    @Test
    public void testFilterRecency() {
        NotificationsSentState allowState = new NotificationsSentState();
        allowState.lastSent = 1;
        AppEntry allow = mock(AppEntry.class);
        allow.extraInfo = allowState;


        assertTrue(FILTER_APP_NOTIFICATION_RECENCY.filterApp(allow));

        NotificationsSentState denyState = new NotificationsSentState();
        denyState.lastSent = 0;
        AppEntry deny = mock(AppEntry.class);
        deny.extraInfo = denyState;

        assertFalse(FILTER_APP_NOTIFICATION_RECENCY.filterApp(deny));
    }

    @Test
    public void testFilterFrequency() {
        NotificationsSentState allowState = new NotificationsSentState();
        allowState.sentCount = 1;
        AppEntry allow = mock(AppEntry.class);
        allow.extraInfo = allowState;

        assertTrue(FILTER_APP_NOTIFICATION_FREQUENCY.filterApp(allow));

        NotificationsSentState denyState = new NotificationsSentState();
        denyState.sentCount = 0;
        AppEntry deny = mock(AppEntry.class);
        deny.extraInfo = denyState;

        assertFalse(FILTER_APP_NOTIFICATION_FREQUENCY.filterApp(deny));
    }

    @Test
    public void testFilterBlocked() {
        NotificationsSentState allowState = new NotificationsSentState();
        allowState.blocked = true;
        AppEntry allow = mock(AppEntry.class);
        allow.extraInfo = allowState;

        assertTrue(FILTER_APP_NOTIFICATION_BLOCKED.filterApp(allow));

        NotificationsSentState denyState = new NotificationsSentState();
        denyState.blocked = false;
        AppEntry deny = mock(AppEntry.class);
        deny.extraInfo = denyState;

        assertFalse(FILTER_APP_NOTIFICATION_BLOCKED.filterApp(deny));
    }

    @Test
    public void testComparators_nullsNoCrash() {
        List<AppEntry> entries = new ArrayList<>();
        AppEntry a = mock(AppEntry.class);
        a.label = "1";
        AppEntry b = mock(AppEntry.class);
        b.label = "2";
        entries.add(a);
        entries.add(b);

        entries.sort(RECENT_NOTIFICATION_COMPARATOR);
        entries.sort(FREQUENCY_NOTIFICATION_COMPARATOR);
    }

    @Test
    public void testRecencyComparator() {
        List<AppEntry> entries = new ArrayList<>();

        NotificationsSentState earlier = new NotificationsSentState();
        earlier.lastSent = 1;
        AppEntry earlyEntry = mock(AppEntry.class);
        earlyEntry.extraInfo = earlier;
        entries.add(earlyEntry);

        NotificationsSentState later = new NotificationsSentState();
        later.lastSent = 8;
        AppEntry lateEntry = mock(AppEntry.class);
        lateEntry.extraInfo = later;
        entries.add(lateEntry);

        entries.sort(RECENT_NOTIFICATION_COMPARATOR);

        assertThat(entries).containsExactly(lateEntry, earlyEntry);
    }

    @Test
    public void testFrequencyComparator() {
        List<AppEntry> entries = new ArrayList<>();

        NotificationsSentState notFrequentWeekly = new NotificationsSentState();
        notFrequentWeekly.sentCount = 2;
        AppEntry notFrequentWeeklyEntry = mock(AppEntry.class);
        notFrequentWeeklyEntry.extraInfo = notFrequentWeekly;
        entries.add(notFrequentWeeklyEntry);

        NotificationsSentState notFrequentDaily = new NotificationsSentState();
        notFrequentDaily.sentCount = 7;
        AppEntry notFrequentDailyEntry = mock(AppEntry.class);
        notFrequentDailyEntry.extraInfo = notFrequentDaily;
        entries.add(notFrequentDailyEntry);

        NotificationsSentState veryFrequentWeekly = new NotificationsSentState();
        veryFrequentWeekly.sentCount = 6;
        AppEntry veryFrequentWeeklyEntry = mock(AppEntry.class);
        veryFrequentWeeklyEntry.extraInfo = veryFrequentWeekly;
        entries.add(veryFrequentWeeklyEntry);

        NotificationsSentState veryFrequentDaily = new NotificationsSentState();
        veryFrequentDaily.sentCount = 19;
        AppEntry veryFrequentDailyEntry = mock(AppEntry.class);
        veryFrequentDailyEntry.extraInfo = veryFrequentDaily;
        entries.add(veryFrequentDailyEntry);

        entries.sort(FREQUENCY_NOTIFICATION_COMPARATOR);

        assertThat(entries).containsExactly(veryFrequentDailyEntry, notFrequentDailyEntry,
                veryFrequentWeeklyEntry, notFrequentWeeklyEntry);
    }

    @Test
    public void testSwitchOnClickListener() {
        ViewGroup parent = mock(ViewGroup.class);
        Switch toggle = mock(Switch.class);
        when(toggle.isChecked()).thenReturn(true);
        when(toggle.isEnabled()).thenReturn(true);
        when(parent.findViewById(anyInt())).thenReturn(toggle);

        AppEntry entry = mock(AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = "pkg";
        entry.info.uid = 1356;
        entry.extraInfo = new NotificationsSentState();

        ViewGroup.OnClickListener listener = mBridge.getSwitchOnClickListener(entry);
        listener.onClick(parent);

        verify(toggle).toggle();
        verify(mBackend).setNotificationsEnabledForPackage(
                entry.info.packageName, entry.info.uid, true);
        assertThat(((NotificationsSentState) entry.extraInfo).blocked).isFalse();
    }

    @Test
    public void testSwitchViews_nullDoesNotCrash() {
        AppStateNotificationBridge.enableSwitch(null);
        AppStateNotificationBridge.checkSwitch(null);
    }
}
