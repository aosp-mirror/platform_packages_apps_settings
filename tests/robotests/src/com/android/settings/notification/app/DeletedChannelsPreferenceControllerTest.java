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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class DeletedChannelsPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;

    private DeletedChannelsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = new DeletedChannelsPreferenceController(mContext, mBackend);
    }

    @Test
    public void noCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void isAvailable_appScreen_notIfAppBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_groupScreen_never() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, mock(NotificationChannelGroup.class), null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_channelScreen_never() {
        mController.onResume(
                new NotificationBackend.AppRow(), mock(NotificationChannel.class), null, null, null,
                null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_appScreen_notIfNoDeletedChannels() {
        when(mBackend.getDeletedChannelCount(any(), anyInt())).thenReturn(0);
        mController.onResume(new NotificationBackend.AppRow(), null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_appScreen() {
        when(mBackend.getDeletedChannelCount(any(), anyInt())).thenReturn(1);
        mController.onResume(new NotificationBackend.AppRow(), null, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void updateState() {
        when(mBackend.getDeletedChannelCount(any(), anyInt())).thenReturn(1);
        mController.onResume(new NotificationBackend.AppRow(), null, null, null, null, null);

        Preference pref = mock(Preference.class);
        mController.updateState(pref);

        verify(pref, times(1)).setSelectable(false);
        verify(mBackend, times(1)).getDeletedChannelCount(any(), anyInt());
        ArgumentCaptor<CharSequence> argumentCaptor = ArgumentCaptor.forClass(CharSequence.class);
        verify(pref, times(1)).setTitle(argumentCaptor.capture());
        assertTrue(argumentCaptor.getValue().toString().contains("1"));
    }
}
