/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import android.app.Instrumentation;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.NotificationsSentState;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ChannelListPreferenceControllerTest {
    private Context mContext;
    private NotificationBackend mBackend;
    private NotificationBackend.AppRow mAppRow;
    private ChannelListPreferenceController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mGroupList;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            mBackend = new NotificationBackend();
            mAppRow = mBackend.loadAppRow(mContext,
                    mContext.getPackageManager(), mContext.getApplicationInfo());
            mController = new ChannelListPreferenceController(mContext, mBackend);
            mController.onResume(mAppRow, null, null, null, null, null, null);
            mPreferenceManager = new PreferenceManager(mContext);
            mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
            mGroupList = new PreferenceCategory(mContext);
            mPreferenceScreen.addPreference(mGroupList);
        });
    }

    @Test
    @UiThreadTest
    public void testUpdateFullList_incrementalUpdates() {
        // Start by testing the case with no groups or channels
        List<NotificationChannelGroup> inGroups = new ArrayList<>();
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            assertEquals("zeroCategories", mGroupList.getPreference(0).getKey());
        }

        // Test that adding a group clears the zero category and adds everything
        NotificationChannelGroup inGroup1 = new NotificationChannelGroup("group1", "Group 1");
        inGroup1.addChannel(new NotificationChannel("ch1a", "Channel 1A", IMPORTANCE_DEFAULT));
        inGroups.add(inGroup1);
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group1 = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group1", group1.getKey());
            assertEquals(2, group1.getPreferenceCount());
            assertNull(group1.getPreference(0).getKey());
            assertEquals("All \"Group 1\" notifications",
                    group1.getPreference(0).getTitle().toString());
            assertEquals("ch1a", group1.getPreference(1).getKey());
            assertEquals("Channel 1A", group1.getPreference(1).getTitle().toString());
        }

        // Test that adding a channel works -- no dupes or omissions
        inGroup1.addChannel(new NotificationChannel("ch1b", "Channel 1B", IMPORTANCE_DEFAULT));
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group1 = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group1", group1.getKey());
            assertEquals(3, group1.getPreferenceCount());
            assertNull(group1.getPreference(0).getKey());
            assertEquals("All \"Group 1\" notifications",
                    group1.getPreference(0).getTitle().toString());
            assertEquals("ch1a", group1.getPreference(1).getKey());
            assertEquals("Channel 1A", group1.getPreference(1).getTitle().toString());
            assertEquals("ch1b", group1.getPreference(2).getKey());
            assertEquals("Channel 1B", group1.getPreference(2).getTitle().toString());
        }

        // Test that renaming a channel does in fact rename the preferences
        inGroup1.getChannels().get(1).setName("Channel 1B - Renamed");
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group1 = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group1", group1.getKey());
            assertEquals(3, group1.getPreferenceCount());
            assertNull(group1.getPreference(0).getKey());
            assertEquals("All \"Group 1\" notifications",
                    group1.getPreference(0).getTitle().toString());
            assertEquals("ch1a", group1.getPreference(1).getKey());
            assertEquals("Channel 1A", group1.getPreference(1).getTitle().toString());
            assertEquals("ch1b", group1.getPreference(2).getKey());
            assertEquals("Channel 1B - Renamed", group1.getPreference(2).getTitle().toString());
        }

        // Test that adding a group works and results in the correct sorting.
        NotificationChannelGroup inGroup0 = new NotificationChannelGroup("group0", "Group 0");
        inGroup0.addChannel(new NotificationChannel("ch0b", "Channel 0B", IMPORTANCE_DEFAULT));
        // NOTE: updateFullList takes a List which has been sorted, so we insert at 0 for this check
        inGroups.add(0, inGroup0);
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(2, mGroupList.getPreferenceCount());
            PreferenceGroup group0 = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group0", group0.getKey());
            assertEquals(2, group0.getPreferenceCount());
            assertNull(group0.getPreference(0).getKey());
            assertEquals("All \"Group 0\" notifications",
                    group0.getPreference(0).getTitle().toString());
            assertEquals("ch0b", group0.getPreference(1).getKey());
            assertEquals("Channel 0B", group0.getPreference(1).getTitle().toString());
            PreferenceGroup group1 = (PreferenceGroup) mGroupList.getPreference(1);
            assertEquals("group1", group1.getKey());
            assertEquals(3, group1.getPreferenceCount());
            assertNull(group1.getPreference(0).getKey());
            assertEquals("All \"Group 1\" notifications",
                    group1.getPreference(0).getTitle().toString());
            assertEquals("ch1a", group1.getPreference(1).getKey());
            assertEquals("Channel 1A", group1.getPreference(1).getTitle().toString());
            assertEquals("ch1b", group1.getPreference(2).getKey());
            assertEquals("Channel 1B - Renamed", group1.getPreference(2).getTitle().toString());
        }

        // Test that adding a channel that comes before another works and has correct ordering.
        // NOTE: the channels within a group are sorted inside updateFullList.
        inGroup0.addChannel(new NotificationChannel("ch0a", "Channel 0A", IMPORTANCE_DEFAULT));
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(2, mGroupList.getPreferenceCount());
            PreferenceGroup group0 = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group0", group0.getKey());
            assertEquals(3, group0.getPreferenceCount());
            assertNull(group0.getPreference(0).getKey());
            assertEquals("All \"Group 0\" notifications",
                    group0.getPreference(0).getTitle().toString());
            assertEquals("ch0a", group0.getPreference(1).getKey());
            assertEquals("Channel 0A", group0.getPreference(1).getTitle().toString());
            assertEquals("ch0b", group0.getPreference(2).getKey());
            assertEquals("Channel 0B", group0.getPreference(2).getTitle().toString());
            PreferenceGroup group1 = (PreferenceGroup) mGroupList.getPreference(1);
            assertEquals("group1", group1.getKey());
            assertEquals(3, group1.getPreferenceCount());
            assertNull(group1.getPreference(0).getKey());
            assertEquals("All \"Group 1\" notifications",
                    group1.getPreference(0).getTitle().toString());
            assertEquals("ch1a", group1.getPreference(1).getKey());
            assertEquals("Channel 1A", group1.getPreference(1).getTitle().toString());
            assertEquals("ch1b", group1.getPreference(2).getKey());
            assertEquals("Channel 1B - Renamed", group1.getPreference(2).getTitle().toString());
        }

        // Test that the "Other" group works.
        // Also test a simultaneous addition and deletion.
        inGroups.remove(inGroup0);
        NotificationChannelGroup inGroupOther = new NotificationChannelGroup(null, null);
        inGroupOther.addChannel(new NotificationChannel("chXa", "Other A", IMPORTANCE_DEFAULT));
        inGroupOther.addChannel(new NotificationChannel("chXb", "Other B", IMPORTANCE_DEFAULT));
        inGroups.add(inGroupOther);
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(2, mGroupList.getPreferenceCount());
            PreferenceGroup group1 = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group1", group1.getKey());
            assertEquals(3, group1.getPreferenceCount());
            assertNull(group1.getPreference(0).getKey());
            assertEquals("All \"Group 1\" notifications",
                    group1.getPreference(0).getTitle().toString());
            assertEquals("ch1a", group1.getPreference(1).getKey());
            assertEquals("Channel 1A", group1.getPreference(1).getTitle().toString());
            assertEquals("ch1b", group1.getPreference(2).getKey());
            assertEquals("Channel 1B - Renamed", group1.getPreference(2).getTitle().toString());
            PreferenceGroup groupOther = (PreferenceGroup) mGroupList.getPreference(1);
            assertEquals("categories", groupOther.getKey());
            assertEquals(2, groupOther.getPreferenceCount());
            assertEquals("chXa", groupOther.getPreference(0).getKey());
            assertEquals("Other A", groupOther.getPreference(0).getTitle().toString());
            assertEquals("chXb", groupOther.getPreference(1).getKey());
            assertEquals("Other B", groupOther.getPreference(1).getTitle().toString());
        }

        // Test that the removal of a channel works.
        inGroupOther.getChannels().remove(0);
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(2, mGroupList.getPreferenceCount());
            PreferenceGroup group1 = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group1", group1.getKey());
            assertEquals(3, group1.getPreferenceCount());
            assertNull(group1.getPreference(0).getKey());
            assertEquals("All \"Group 1\" notifications",
                    group1.getPreference(0).getTitle().toString());
            assertEquals("ch1a", group1.getPreference(1).getKey());
            assertEquals("Channel 1A", group1.getPreference(1).getTitle().toString());
            assertEquals("ch1b", group1.getPreference(2).getKey());
            assertEquals("Channel 1B - Renamed", group1.getPreference(2).getTitle().toString());
            PreferenceGroup groupOther = (PreferenceGroup) mGroupList.getPreference(1);
            assertEquals("categories", groupOther.getKey());
            assertEquals(1, groupOther.getPreferenceCount());
            assertEquals("chXb", groupOther.getPreference(0).getKey());
            assertEquals("Other B", groupOther.getPreference(0).getTitle().toString());
        }

        // Test that we go back to the empty state when clearing all groups and channels.
        inGroups.clear();
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            assertEquals("zeroCategories", mGroupList.getPreference(0).getKey());
        }
    }


    @Test
    @UiThreadTest
    public void testUpdateFullList_groupBlockedChange() {
        List<NotificationChannelGroup> inGroups = new ArrayList<>();
        NotificationChannelGroup inGroup = new NotificationChannelGroup("group", "My Group");
        inGroup.addChannel(new NotificationChannel("channelA", "Channel A", IMPORTANCE_DEFAULT));
        inGroup.addChannel(new NotificationChannel("channelB", "Channel B", IMPORTANCE_NONE));
        inGroups.add(inGroup);

        // Test that the group is initially showing all preferences
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group", group.getKey());
            assertEquals(3, group.getPreferenceCount());
            SwitchPreferenceCompat groupBlockPref = (SwitchPreferenceCompat) group.getPreference(0);
            assertNull(groupBlockPref.getKey());
            assertEquals("All \"My Group\" notifications", groupBlockPref.getTitle().toString());
            assertTrue(groupBlockPref.isChecked());
            PrimarySwitchPreference channelAPref = (PrimarySwitchPreference) group.getPreference(1);
            assertEquals("channelA", channelAPref.getKey());
            assertEquals("Channel A", channelAPref.getTitle().toString());
            assertEquals(Boolean.TRUE, channelAPref.getCheckedState());
            PrimarySwitchPreference channelBPref = (PrimarySwitchPreference) group.getPreference(2);
            assertEquals("channelB", channelBPref.getKey());
            assertEquals("Channel B", channelBPref.getTitle().toString());
            assertEquals(Boolean.FALSE, channelBPref.getCheckedState());
        }

        // Test that when a group is blocked, the list removes its individual channel preferences
        inGroup.setBlocked(true);
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group", group.getKey());
            assertEquals(1, group.getPreferenceCount());
            SwitchPreferenceCompat groupBlockPref = (SwitchPreferenceCompat) group.getPreference(0);
            assertNull(groupBlockPref.getKey());
            assertEquals("All \"My Group\" notifications", groupBlockPref.getTitle().toString());
            assertFalse(groupBlockPref.isChecked());
        }

        // Test that when a group is unblocked, the list adds its individual channel preferences
        inGroup.setBlocked(false);
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group", group.getKey());
            assertEquals(3, group.getPreferenceCount());
            SwitchPreferenceCompat groupBlockPref = (SwitchPreferenceCompat) group.getPreference(0);
            assertNull(groupBlockPref.getKey());
            assertEquals("All \"My Group\" notifications", groupBlockPref.getTitle().toString());
            assertTrue(groupBlockPref.isChecked());
            PrimarySwitchPreference channelAPref = (PrimarySwitchPreference) group.getPreference(1);
            assertEquals("channelA", channelAPref.getKey());
            assertEquals("Channel A", channelAPref.getTitle().toString());
            assertEquals(Boolean.TRUE, channelAPref.getCheckedState());
            PrimarySwitchPreference channelBPref = (PrimarySwitchPreference) group.getPreference(2);
            assertEquals("channelB", channelBPref.getKey());
            assertEquals("Channel B", channelBPref.getTitle().toString());
            assertEquals(Boolean.FALSE, channelBPref.getCheckedState());
        }
    }

    @Test
    @UiThreadTest
    public void testUpdateFullList_channelUpdates() {
        List<NotificationChannelGroup> inGroups = new ArrayList<>();
        NotificationChannelGroup inGroup = new NotificationChannelGroup("group", "Group");
        NotificationChannel channelA =
                new NotificationChannel("channelA", "Channel A", IMPORTANCE_HIGH);
        NotificationChannel channelB =
                new NotificationChannel("channelB", "Channel B", IMPORTANCE_NONE);
        inGroup.addChannel(channelA);
        inGroup.addChannel(channelB);
        inGroups.add(inGroup);

        NotificationsSentState sentA = new NotificationsSentState();
        sentA.avgSentDaily = 2;
        sentA.avgSentWeekly = 10;
        NotificationsSentState sentB = new NotificationsSentState();
        sentB.avgSentDaily = 0;
        sentB.avgSentWeekly = 2;
        mAppRow.sentByChannel.put("channelA", sentA);

        // Test that the channels' properties are reflected in the preference
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group", group.getKey());
            assertEquals(3, group.getPreferenceCount());
            assertNull(group.getPreference(0).getKey());
            assertEquals("All \"Group\" notifications",
                    group.getPreference(0).getTitle().toString());
            PrimarySwitchPreference channelAPref = (PrimarySwitchPreference) group.getPreference(1);
            assertEquals("channelA", channelAPref.getKey());
            assertEquals("Channel A", channelAPref.getTitle().toString());
            assertEquals(Boolean.TRUE, channelAPref.getCheckedState());
            assertEquals("About 2 notifications per day", channelAPref.getSummary().toString());
            PrimarySwitchPreference channelBPref = (PrimarySwitchPreference) group.getPreference(2);
            assertEquals("channelB", channelBPref.getKey());
            assertEquals("Channel B", channelBPref.getTitle().toString());
            assertEquals(Boolean.FALSE, channelBPref.getCheckedState());
            assertNull(channelBPref.getSummary());
        }

        channelA.setImportance(IMPORTANCE_NONE);
        channelB.setImportance(IMPORTANCE_DEFAULT);

        mAppRow.sentByChannel.remove("channelA");
        mAppRow.sentByChannel.put("channelB", sentB);

        // Test that changing the channels' properties correctly updates the preference
        mController.updateFullList(mGroupList, inGroups);
        {
            assertEquals(1, mGroupList.getPreferenceCount());
            PreferenceGroup group = (PreferenceGroup) mGroupList.getPreference(0);
            assertEquals("group", group.getKey());
            assertEquals(3, group.getPreferenceCount());
            assertNull(group.getPreference(0).getKey());
            assertEquals("All \"Group\" notifications",
                    group.getPreference(0).getTitle().toString());
            PrimarySwitchPreference channelAPref = (PrimarySwitchPreference) group.getPreference(1);
            assertEquals("channelA", channelAPref.getKey());
            assertEquals("Channel A", channelAPref.getTitle().toString());
            assertEquals(Boolean.FALSE, channelAPref.getCheckedState());
            assertNull(channelAPref.getSummary());
            PrimarySwitchPreference channelBPref = (PrimarySwitchPreference) group.getPreference(2);
            assertEquals("channelB", channelBPref.getKey());
            assertEquals("Channel B", channelBPref.getTitle().toString());
            assertEquals(Boolean.TRUE, channelBPref.getCheckedState());
            assertEquals("About 2 notifications per week", channelBPref.getSummary().toString());
        }
    }
}
