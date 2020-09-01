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

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class DndPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private DndPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = spy(new DndPreferenceController(mContext, mBackend));
    }

    @Test
    public void testIsAvailable_app() {
        when(mNm.getNotificationPolicy()).thenReturn(new NotificationManager.Policy(0, 0, 0, 0));
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channel() {
        when(mNm.getNotificationPolicy()).thenReturn(new NotificationManager.Policy(0, 0, 0, 1));
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "", IMPORTANCE_MIN);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testUpdateState_disabledByAdmin() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, mock(
                RestrictedLockUtils.EnforcedAdmin.class));

        Preference pref = new RestrictedSwitchPreference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void testUpdateState_notBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByOEM()).thenReturn(true);
        mController.onResume(appRow, channel, null, null, null, null);

        Preference pref = new RestrictedSwitchPreference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
    }

    @Test
    public void testUpdateState_configurable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(appRow, channel, null, null, null, null);

        Preference pref = new RestrictedSwitchPreference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
    }

    @Test
    public void testUpdateState_bypassDnd() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.canBypassDnd()).thenReturn(true);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        RestrictedSwitchPreference pref =
                new RestrictedSwitchPreference(RuntimeEnvironment.application);
        mController.updateState(pref);
        assertTrue(pref.isChecked());
    }

    @Test
    public void testUpdateState_noBypassDnd() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.canBypassDnd()).thenReturn(false);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        RestrictedSwitchPreference pref =
                new RestrictedSwitchPreference(RuntimeEnvironment.application);
        mController.updateState(pref);
        assertFalse(pref.isChecked());
    }

    @Test
    public void testOnPreferenceChange_on() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_LOW);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        RestrictedSwitchPreference pref =
                new RestrictedSwitchPreference(RuntimeEnvironment.application);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, true);

        assertTrue(channel.canBypassDnd());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void testOnPreferenceChange_off() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_HIGH);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        RestrictedSwitchPreference pref =
                new RestrictedSwitchPreference(RuntimeEnvironment.application);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, false);

        assertFalse(channel.canBypassDnd());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }
}
