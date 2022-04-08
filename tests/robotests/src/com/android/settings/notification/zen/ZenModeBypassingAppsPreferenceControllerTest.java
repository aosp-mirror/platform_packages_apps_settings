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
 * limitations under the License.
 */

package com.android.settings.notification.zen;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModeBypassingAppsPreferenceControllerTest {

    private ZenModeBypassingAppsPreferenceController mController;

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    private int mPreviousZenSetting;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ZenModeBypassingAppsPreferenceController(
                mContext, null, mock(Fragment.class), mock(Lifecycle.class));
        mController.mPreference = new Preference(mContext);
        mPreviousZenSetting =
                Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_OFF);
        ReflectionHelpers.setField(mController, "mNotificationBackend", mBackend);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                mPreviousZenSetting);
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateBypassingApps() {
        // GIVEN DND is off
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_OFF);

        // mock app list
        ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = "test";
        entry.label = "test";
        entry.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry);

        List<NotificationChannel> channelsBypassing = new ArrayList<>();
        channelsBypassing.add(mock(NotificationChannel.class));

        when(mBackend.getNotificationChannelsBypassingDnd(entry.info.packageName,
                entry.info.uid)).thenReturn(new ParceledListSlice<>(channelsBypassing));

        // WHEN a single app is passed to the controller
        mController.updateAppsBypassingDndSummaryText(appEntries);

        // THEN the preference is enabled and the summary contains the app name from the list
        assertThat(mController.mPreference.isEnabled()).isTrue();
        assertThat(mController.getSummary().contains(entry.label)).isTrue();
    }

    @Test
    public void testUpdateBypassingApps_multipleApps() {
        // GIVEN DND is off
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_OFF);

        // mock app list
        ApplicationsState.AppEntry entry1 = mock(ApplicationsState.AppEntry.class);
        entry1.info = new ApplicationInfo();
        entry1.info.packageName = "test1";
        entry1.label = "test1";
        entry1.info.uid = 1;
        ApplicationsState.AppEntry entry2 = mock(ApplicationsState.AppEntry.class);
        entry2.info = new ApplicationInfo();
        entry2.info.packageName = "test2";
        entry2.label = "test2";
        entry2.info.uid = 2;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry1);
        appEntries.add(entry2);

        List<NotificationChannel> channelsBypassing = new ArrayList<>();
        NotificationChannel mockChannel = mock(NotificationChannel.class);
        when(mockChannel.getConversationId()).thenReturn(null); // not a conversation
        channelsBypassing.add(mockChannel);

        when(mBackend.getNotificationChannelsBypassingDnd(entry1.info.packageName,
                entry1.info.uid)).thenReturn(new ParceledListSlice<>(channelsBypassing));
        when(mBackend.getNotificationChannelsBypassingDnd(entry2.info.packageName,
                entry2.info.uid)).thenReturn(new ParceledListSlice<>(channelsBypassing));

        // WHEN a list of apps is passed to the controller
        mController.updateAppsBypassingDndSummaryText(appEntries);

        // THEN the preference is enabled and the summary contains the app names from the list
        assertThat(mController.mPreference.isEnabled()).isTrue();
        assertThat(mController.getSummary().contains(entry1.label)).isTrue();
        assertThat(mController.getSummary().contains(entry2.label)).isTrue();
    }

    @Test
    public void testUpdateBypassingApps_conversation() {
        // GIVEN DND is off
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_OFF);

        // mock app list
        ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = "test";
        entry.label = "test";
        entry.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry);

        List<NotificationChannel> channelsBypassing = new ArrayList<>();
        NotificationChannel conversation  = mock(NotificationChannel.class);
        when(conversation.getConversationId()).thenReturn("conversation!");
        channelsBypassing.add(conversation);

        when(mBackend.getNotificationChannelsBypassingDnd(entry.info.packageName,
                entry.info.uid)).thenReturn(new ParceledListSlice<>(channelsBypassing));

        // WHEN a single app is passed to the controller with a conversation notif channel
        mController.updateAppsBypassingDndSummaryText(appEntries);

        // THEN the preference is enabled and the summary doesn't contain any apps because the
        // only channel bypassing DND is a conversation (which will be showed on the
        // conversations page instead of the apps page)
        assertThat(mController.mPreference.isEnabled()).isTrue();
        assertThat(mController.getSummary().contains("No apps")).isTrue();
    }

    @Test
    public void testUpdateBypassingApps_demotedConversation() {
        // GIVEN DND is off
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_OFF);

        // mock app list
        ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = "test";
        entry.label = "test";
        entry.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry);

        List<NotificationChannel> channelsBypassing = new ArrayList<>();
        NotificationChannel demotedConversation  = mock(NotificationChannel.class);
        when(demotedConversation.getConversationId()).thenReturn("conversationId");
        when(demotedConversation.isDemoted()).thenReturn(true);
        channelsBypassing.add(demotedConversation);

        when(mBackend.getNotificationChannelsBypassingDnd(entry.info.packageName,
                entry.info.uid)).thenReturn(new ParceledListSlice<>(channelsBypassing));

        // WHEN a single app is passed to the controller with a demoted conversation notif channel
        mController.updateAppsBypassingDndSummaryText(appEntries);

        // THEN the preference is enabled and the summary contains the app name from the list
        assertThat(mController.mPreference.isEnabled()).isTrue();
        assertThat(mController.getSummary().contains(entry.label)).isTrue();
    }

    @Test
    public void testUpdateAppsBypassingDnd_nullAppsList() {
        // GIVEN DND is off
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_OFF);

        // WHEN the list of apps is null
        mController.updateAppsBypassingDndSummaryText(null);

        // THEN the preference is enabled and summary is unchanged (in this case, null)
        assertThat(mController.mPreference.isEnabled()).isTrue();
        assertThat(mController.getSummary()).isNull();
    }

    @Test
    public void testUpdateAppsBypassingDnd_emptyAppsList() {
        // GIVEN the DND is off
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_OFF);

        // WHEN the list of apps is an empty list
        mController.updateAppsBypassingDndSummaryText(new ArrayList<>());

        // THEN the preference is enabled and summary is updated
        assertThat(mController.mPreference.isEnabled()).isTrue();
        assertThat(mController.getSummary().contains("No apps")).isTrue();
    }

    @Test
    public void testUpdateAppsBypassingDnd_alarmsOnly() {
        // GIVEN alarms only DND mode
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ZEN_MODE,
                Settings.Global.ZEN_MODE_ALARMS);

        // mock app entries
        ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = "test";
        entry.label = "test";
        entry.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry);

        List<NotificationChannel> channelsBypassing = new ArrayList<>();
        channelsBypassing.add(mock(NotificationChannel.class));
        when(mBackend.getNotificationChannelsBypassingDnd(entry.info.packageName,
                entry.info.uid)).thenReturn(new ParceledListSlice<>(channelsBypassing));

        // WHEN we update apps bypassing dnd summary text
        mController.updateAppsBypassingDndSummaryText(appEntries);

        // THEN the preference is disabled and the summary says no apps can bypass
        assertThat(mController.mPreference.isEnabled()).isFalse();
        assertThat(mController.getSummary().contains("No apps")).isTrue();
    }
}
