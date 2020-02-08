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
 * limitations under the License.
 */

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_NONE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class NotificationsOffPreferenceControllerTest {

    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;

    private NotificationsOffPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mController = spy(new NotificationsOffPreferenceController(RuntimeEnvironment.application));
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void testIsAvailable_yesIfAppBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_yesIfChannelGroupBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(true);
        mController.onResume(appRow, null, group, null, null, null);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_yesIfChannelBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null, null, null);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateState_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null, null, null);

        Preference pref = new Preference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertThat(pref.getTitle().toString()).contains("category");
        assertThat(pref.isSelectable()).isFalse();
    }

    @Test
    public void testUpdateState_channelGroup() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(true);
        mController.onResume(appRow, null, group, null, null, null);

        Preference pref = new Preference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertThat(pref.getTitle().toString()).contains("group");
        assertThat(pref.isSelectable()).isFalse();
    }

    @Test
    public void testUpdateState_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null);

        Preference pref = new Preference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertThat(pref.getTitle().toString()).contains("app");
        assertThat(pref.isSelectable()).isFalse();
    }
}
