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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModeAllBypassingAppsPreferenceControllerTest {
    private ZenModeAllBypassingAppsPreferenceController mController;

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private ApplicationsState mApplicationState;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new ZenModeAllBypassingAppsPreferenceController(
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
        // WHEN there's two apps with notification channels that bypass DND
        ApplicationsState.AppEntry entry1 = mock(ApplicationsState.AppEntry.class);
        entry1.info = new ApplicationInfo();
        entry1.info.packageName = "test";
        entry1.info.uid = 0;

        ApplicationsState.AppEntry entry2 = mock(ApplicationsState.AppEntry.class);
        entry2.info = new ApplicationInfo();
        entry2.info.packageName = "test2";
        entry2.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry1);
        appEntries.add(entry2);
        List<NotificationChannel> channelsBypassing = new ArrayList<>();
        channelsBypassing.add(mock(NotificationChannel.class));
        channelsBypassing.add(mock(NotificationChannel.class));
        when(mBackend.getNotificationChannelsBypassingDnd(anyString(),
                anyInt())).thenReturn(new ParceledListSlice<>(channelsBypassing));

        // THEN there's are two preferences
        mController.updateAppList(appEntries);
        verify(mPreferenceCategory, times(2)).addPreference(any());
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
