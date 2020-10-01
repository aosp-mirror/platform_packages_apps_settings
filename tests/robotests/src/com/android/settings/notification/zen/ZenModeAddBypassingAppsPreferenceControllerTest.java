/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModeAddBypassingAppsPreferenceControllerTest {

    @Mock
    private NotificationBackend mBackend;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private ApplicationsState mApplicationState;
    private ZenModeAddBypassingAppsPreferenceController mController;
    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new ZenModeAddBypassingAppsPreferenceController(
                mContext, null, mock(Fragment.class), mBackend);
        mController.mPreferenceCategory = mPreferenceCategory;
        mController.mApplicationsState = mApplicationState;
        mController.mPrefContext = mContext;
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateAppList() {
        // GIVEN there's an app with bypassing channels, app without any channels, and then an app
        // with notification channels but none that can bypass DND
        ApplicationsState.AppEntry appWithBypassingChannels =
                mock(ApplicationsState.AppEntry.class);
        appWithBypassingChannels.info = new ApplicationInfo();
        appWithBypassingChannels.info.packageName = "appWithBypassingChannels";
        appWithBypassingChannels.info.uid = 0;
        when(mBackend.getNotificationChannelsBypassingDnd(
                appWithBypassingChannels.info.packageName,
                appWithBypassingChannels.info.uid))
                .thenReturn(new ParceledListSlice<>(
                        Arrays.asList(mock(NotificationChannel.class))));
        when(mBackend.getChannelCount(
                appWithBypassingChannels.info.packageName,
                appWithBypassingChannels.info.uid))
                .thenReturn(5);

        ApplicationsState.AppEntry appWithoutChannels = mock(ApplicationsState.AppEntry.class);
        appWithoutChannels.info = new ApplicationInfo();
        appWithoutChannels.info.packageName = "appWithoutChannels";
        appWithoutChannels.info.uid = 0;
        when(mBackend.getChannelCount(
                appWithoutChannels.info.packageName,
                appWithoutChannels.info.uid))
                .thenReturn(0);
        when(mBackend.getNotificationChannelsBypassingDnd(
                appWithoutChannels.info.packageName,
                appWithoutChannels.info.uid))
                .thenReturn(new ParceledListSlice<>(new ArrayList<>()));

        ApplicationsState.AppEntry appWithChannelsNoneBypassing =
                mock(ApplicationsState.AppEntry.class);
        appWithChannelsNoneBypassing.info = new ApplicationInfo();
        appWithChannelsNoneBypassing.info.packageName = "appWithChannelsNoneBypassing";
        appWithChannelsNoneBypassing.info.uid = 0;
        when(mBackend.getChannelCount(
                appWithChannelsNoneBypassing.info.packageName,
                appWithChannelsNoneBypassing.info.uid))
                .thenReturn(5);
        when(mBackend.getNotificationChannelsBypassingDnd(
                appWithChannelsNoneBypassing.info.packageName,
                appWithChannelsNoneBypassing.info.uid))
                .thenReturn(new ParceledListSlice<>(new ArrayList<>()));

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(appWithBypassingChannels);
        appEntries.add(appWithoutChannels);
        appEntries.add(appWithChannelsNoneBypassing);

        // WHEN the controller updates the app list with the app entries
        mController.updateAppList(appEntries);

        // THEN only the appWithChannelsNoneBypassing makes it to the app list
        ArgumentCaptor<Preference> prefCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mPreferenceCategory).addPreference(prefCaptor.capture());

        Preference pref = prefCaptor.getValue();
        assertThat(pref.getKey()).isEqualTo(
                ZenModeAllBypassingAppsPreferenceController.getKey(
                        appWithChannelsNoneBypassing.info.packageName));
    }

    @Test
    public void testUpdateAppList_nullApps() {
        mController.updateAppList(null);
        verify(mPreferenceCategory, never()).addPreference(any());
    }

    @Test
    public void testUpdateAppList_emptyAppList() {
        // WHEN there are no apps
        mController.updateAppList(new ArrayList<>());

        // THEN only the appWithChannelsNoneBypassing makes it to the app list
        ArgumentCaptor<Preference> prefCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mPreferenceCategory).addPreference(prefCaptor.capture());

        Preference pref = prefCaptor.getValue();
        assertThat(pref.getKey()).isEqualTo(
                ZenModeAllBypassingAppsPreferenceController.KEY_NO_APPS);
    }
}
