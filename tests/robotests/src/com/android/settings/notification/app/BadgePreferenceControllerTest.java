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
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.provider.Settings.Secure.NOTIFICATION_BADGING;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;

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
public class BadgePreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private BadgePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = spy(new BadgePreferenceController(mContext, mBackend));
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(RestrictedSwitchPreference.class));
        mController.onPreferenceChange(mock(RestrictedSwitchPreference.class), true);
    }

    @Test
    public void testIsAvailable_notIfAppBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class), null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfChannelBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channel_notIfAppOff() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfOffGlobally() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BADGING, 0);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BADGING, 1);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_defaultChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        when(channel.getId()).thenReturn(DEFAULT_CHANNEL_ID);
        mController.onResume(appRow, channel, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BADGING, 1);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BADGING, 1);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channelAppOff() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BADGING, 1);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testUpdateState_disabledByAdmin() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(new NotificationBackend.AppRow(), channel, null,
                null, null, mock(RestrictedLockUtils.EnforcedAdmin.class));

        Preference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void testUpdateState_channelNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("");
        when(channel.isImportanceLockedByOEM()).thenReturn(true);
        mController.onResume(appRow, channel, null, null, null, null);

        Preference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
    }

    @Test
    public void testUpdateState_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.canShowBadge()).thenReturn(true);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertTrue(pref.isChecked());

        when(channel.canShowBadge()).thenReturn(false);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(pref);

        assertFalse(pref.isChecked());
    }

    @Test
    public void testUpdateState_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = true;
        mController.onResume(appRow, null, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);
        assertTrue(pref.isChecked());

        appRow.showBadge = false;
        mController.onResume(appRow, null, null, null, null, null);

        mController.updateState(pref);
        assertFalse(pref.isChecked());
    }

    @Test
    public void testOnPreferenceChange_on_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = true;
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_LOW);
        channel.setShowBadge(false);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, true);
        assertTrue(channel.canShowBadge());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void testOnPreferenceChange_off_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = true;
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_HIGH);
        channel.setShowBadge(true);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, false);
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
        assertFalse(channel.canShowBadge());
    }

    @Test
    public void testOnPreferenceChange_on_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = false;
        mController.onResume(appRow, null, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, true);

        assertTrue(appRow.showBadge);
        verify(mBackend, times(1)).setShowBadge(any(), anyInt(), eq(true));
    }

    @Test
    public void testOnPreferenceChange_off_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.showBadge = true;
        mController.onResume(appRow, null, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, false);

        assertFalse(appRow.showBadge);
        verify(mBackend, times(1)).setShowBadge(any(), anyInt(), eq(false));
    }
}
