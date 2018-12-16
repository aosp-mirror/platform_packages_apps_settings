/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.usage.UsageEvents;
import android.os.Parcel;

import com.android.settings.notification.NotificationBackend.AppRow;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NotificationBackendTest {

    @Test
    public void testMarkAppRow_unblockablePackage() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[2];
        nonBlockablePkgs[0] = packageName;
        nonBlockablePkgs[1] = "some.other.package";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        // This package has a package lock but no locked channels
        assertTrue(appRow.lockedImportance);
        assertNull(appRow.lockedChannelId);
    }

    @Test
    public void testMarkAppRow_unblockableChannelOrPkg() {
        String channelBlockName = "foo.bar.pkgWithChannel";
        String pkgBlockName = "foo.bar.pkgBlock";
        String[] nonBlockablePkgs = new String[2];
        nonBlockablePkgs[0] = pkgBlockName;
        nonBlockablePkgs[1] = channelBlockName + ":SpecificChannel";

        // This package has a channel level lock but no full package lock
        AppRow channelBlockApp = new AppRow();
        channelBlockApp.pkg = channelBlockName;
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, channelBlockApp,
                channelBlockName);
        assertFalse(channelBlockApp.lockedImportance);
        assertEquals("SpecificChannel", channelBlockApp.lockedChannelId);

        // This other package has the reverse
        AppRow pkgBlock = new AppRow();
        pkgBlock.pkg = pkgBlockName;
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, pkgBlock, pkgBlockName);
        assertTrue(pkgBlock.lockedImportance);
        assertNull(pkgBlock.lockedChannelId);

        // This third package has no locks at all
        AppRow otherAppRow = new AppRow();
        otherAppRow.pkg ="foo.bar.nothingBlocked";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, otherAppRow,
                "foo.bar.nothingBlocked");
        assertFalse(otherAppRow.lockedImportance);
        assertNull(otherAppRow.lockedChannelId);
    }

    @Test
    public void testMarkAppRow_unblockableChannelAndPkg() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[2];
        nonBlockablePkgs[0] = "foo.bar.unblockable";
        nonBlockablePkgs[1] = "foo.bar.unblockable:SpecificChannel";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        // This package has both a channel lock and a package lock
        assertTrue(appRow.lockedImportance);
        assertEquals("SpecificChannel", appRow.lockedChannelId);
    }

    @Test
    public void testMarkAppRow_channelNameWithColons() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        String channelName = "SpecificChannel:1234:abc:defg";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[1];
        nonBlockablePkgs[0] = packageName + ":" + channelName;
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        assertEquals(channelName, appRow.lockedChannelId);
    }

    @Test
    public void testMarkAppRow_blocklistWithNullEntries() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[6]; // extra long list with some entries left null
        nonBlockablePkgs[2] = "foo.bar.unblockable";
        nonBlockablePkgs[4] = "foo.bar.unblockable:SpecificChannel";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        assertTrue(appRow.lockedImportance);
        assertEquals("SpecificChannel", appRow.lockedChannelId);
    }

    @Test
    public void testGetAggregatedUsageEvents_multipleEventsAgg() {
        List<UsageEvents.Event> events = new ArrayList<>();
        UsageEvents.Event good = new UsageEvents.Event();
        good.mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION;
        good.mPackage = "pkg";
        good.mNotificationChannelId = "channel1";
        good.mTimeStamp = 2;
        events.add(good);
        UsageEvents.Event good2 = new UsageEvents.Event();
        good2.mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION;
        good2.mPackage = "pkg";
        good2.mNotificationChannelId = "channel2";
        good2.mTimeStamp = 3;
        events.add(good2);
        UsageEvents.Event good1 = new UsageEvents.Event();
        good1.mEventType = UsageEvents.Event.NOTIFICATION_INTERRUPTION;
        good1.mPackage = "pkg";
        good1.mNotificationChannelId = "channel1";
        good1.mTimeStamp = 6;
        events.add(good1);
        NotificationBackend backend = new NotificationBackend();

        AppRow appRow = new AppRow();
        appRow.pkg = "pkg";
        backend.recordAggregatedUsageEvents(getUsageEvents(events), appRow);

        assertThat(appRow.sentByChannel.get("channel1").sentCount).isEqualTo(2);
        assertThat(appRow.sentByChannel.get("channel1").lastSent).isEqualTo(6);
        assertThat(appRow.sentByChannel.get("channel1").avgSentWeekly).isEqualTo(2);
        assertThat(appRow.sentByChannel.get("channel2").sentCount).isEqualTo(1);
        assertThat(appRow.sentByChannel.get("channel2").lastSent).isEqualTo(3);
        assertThat(appRow.sentByChannel.get("channel2").avgSentWeekly).isEqualTo(1);
        assertThat(appRow.sentByApp.sentCount).isEqualTo(3);
        assertThat(appRow.sentByApp.lastSent).isEqualTo(6);
        assertThat(appRow.sentByApp.avgSentWeekly).isEqualTo(3);
    }

    private UsageEvents getUsageEvents(List<UsageEvents.Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, new String[] {"pkg"});
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }
}
