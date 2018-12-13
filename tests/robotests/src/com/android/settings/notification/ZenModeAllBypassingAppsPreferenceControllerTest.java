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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;

import com.android.settingslib.applications.ApplicationsState;

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

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class ZenModeAllBypassingAppsPreferenceControllerTest {
    private ZenModeAllBypassingAppsPreferenceController mController;

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private ApplicationsState mApplicationState;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new ZenModeAllBypassingAppsPreferenceController(
                mContext, null, mock(Fragment.class));
        mController.mPreferenceScreen = mPreferenceScreen;
        mController.mApplicationsState = mApplicationState;
        mController.mPrefContext = mContext;
        ReflectionHelpers.setField(mController, "mNotificationBackend", mBackend);
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateNotificationChannelList() {
        ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = "test";
        entry.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry);

        List<NotificationChannel> channelsBypassing = new ArrayList<>();
        channelsBypassing.add(mock(NotificationChannel.class));
        channelsBypassing.add(mock(NotificationChannel.class));
        channelsBypassing.add(mock(NotificationChannel.class));

        when(mBackend.getNotificationChannelsBypassingDnd(entry.info.packageName,
                entry.info.uid)).thenReturn(new ParceledListSlice<>(channelsBypassing));

        mController.updateNotificationChannelList(appEntries);
        verify(mPreferenceScreen, times(3)).addPreference(any());
    }

    @Test
    public void testUpdateNotificationChannelList_nullChannels() {
        mController.updateNotificationChannelList(null);
        verify(mPreferenceScreen, never()).addPreference(any());
    }

    @Test
    public void testUpdateNotificationChannelList_emptyChannelsList() {
        mController.updateNotificationChannelList(new ArrayList<>());
        verify(mPreferenceScreen, never()).addPreference(any());
    }
}
