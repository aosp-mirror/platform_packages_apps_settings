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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.flags.Flags;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class AppChannelsBypassingDndPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private NotificationBackend mBackend;

    private NotificationBackend.AppRow mAppRow;
    private AppChannelsBypassingDndPreferenceController mController;

    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();

        mAppRow = new NotificationBackend.AppRow();
        mAppRow.uid = 42;
        mAppRow.pkg = "com.example.exampling";

        mController = new AppChannelsBypassingDndPreferenceController(context, mBackend);
        mController.onResume(mAppRow, null, null, null, null, null, new ArrayList<>());

        PreferenceManager preferenceManager = new PreferenceManager(context);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(context);
        mCategory = new PreferenceCategory(context);
        mCategory.setKey(AppChannelsBypassingDndPreferenceController.KEY);
        mPreferenceScreen.addPreference(mCategory);
    }

    @Test
    public void displayPreference_showsAllAndChannels() {
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, true, false));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(mCategory.getPreferenceCount()).isEqualTo(4); // "All" + 3 channels
        assertThat(mCategory.getPreference(0).getTitle().toString()).isEqualTo(
                "Allow all notifications");
        assertThat(mCategory.getPreference(1).getTitle().toString()).isEqualTo("Channel 1");
        assertThat(mCategory.getPreference(2).getTitle().toString()).isEqualTo("Channel 2");
        assertThat(mCategory.getPreference(3).getTitle().toString()).isEqualTo("Channel 3");
    }

    @Test
    public void displayPreference_canToggleAllInterrupt() {
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, true, false));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(mCategory.getPreference(0).isEnabled()).isTrue();
    }

    @Test
    public void displayPreference_canToggleInterruptIfChannelEnabled() {
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, false, true));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                1)).isSwitchEnabled()).isTrue();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                2)).isSwitchEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                3)).isSwitchEnabled()).isTrue();
    }

    @Test
    public void displayPreference_appBlocked_cannotToggleAllOrChannelInterrupts() {
        mAppRow.banned = true;
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, false, true));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(mCategory.getPreference(0).isEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                1)).isSwitchEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                2)).isSwitchEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                3)).isSwitchEnabled()).isFalse();
    }

    private static ParceledListSlice<NotificationChannelGroup> buildGroupList(
            boolean... enabledByChannel) {
        NotificationChannelGroup group = new NotificationChannelGroup("group", "The Group");
        for (int i = 0; i < enabledByChannel.length; i++) {
            group.addChannel(new NotificationChannel("channel-" + (i + 1), "Channel " + (i + 1),
                    enabledByChannel[i] ? NotificationManager.IMPORTANCE_DEFAULT
                            : NotificationManager.IMPORTANCE_NONE));
        }
        return new ParceledListSlice<>(Collections.singletonList(group));
    }

    @Test
    public void displayPreference_duplicateChannelName_AddsGroupNameAsSummary() {
        mSetFlagsRule.enableFlags(Flags.FLAG_DEDUPE_DND_SETTINGS_CHANNELS);
        NotificationChannelGroup group1 = new NotificationChannelGroup("group1_id", "Group1");
        NotificationChannelGroup group2 = new NotificationChannelGroup("group2_id", "Group2");

        group1.addChannel(new NotificationChannel("mail_group1_id", "Mail",
                NotificationManager.IMPORTANCE_DEFAULT));
        group1.addChannel(new NotificationChannel("other_group1_id", "Other",
                NotificationManager.IMPORTANCE_DEFAULT));

        group2.addChannel(new NotificationChannel("music_group2_id", "Music",
                NotificationManager.IMPORTANCE_DEFAULT));
        // This channel has the same name as a channel in group1.
        group2.addChannel(new NotificationChannel("mail_group2_id", "Mail",
                NotificationManager.IMPORTANCE_DEFAULT));

        ParceledListSlice<NotificationChannelGroup> groups = new ParceledListSlice<>(
                new ArrayList<NotificationChannelGroup>() {
                    {
                        add(group1);
                        add(group2);
                    }
                }
        );

        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(groups);
        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();
        // Check that we've added the group name as a summary to channels that have identical names.
        // Channels are also alphabetized.
        assertThat(mCategory.getPreference(1).getTitle().toString()).isEqualTo("Mail");
        assertThat(mCategory.getPreference(1).getSummary().toString()).isEqualTo("Group1");
        assertThat(mCategory.getPreference(2).getTitle().toString()).isEqualTo("Mail");
        assertThat(mCategory.getPreference(2).getSummary().toString()).isEqualTo("Group2");
        assertThat(mCategory.getPreference(3).getTitle().toString()).isEqualTo("Music");
        assertThat(mCategory.getPreference(4).getTitle().toString()).isEqualTo("Other");

    }
}
