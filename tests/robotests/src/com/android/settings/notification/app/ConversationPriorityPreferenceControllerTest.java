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

package com.android.settings.notification.app;

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedLockUtils;

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
public class ConversationPriorityPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    private ConversationPriorityPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = spy(new ConversationPriorityPreferenceController(
                mContext, mBackend, mDependentFieldListener));
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
        mController.onPreferenceChange(mock(Preference.class), true);
    }

    @Test
    public void testIsAvailable_notChannelNull() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testUpdateState_disabledByAdmin() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, mock(
                RestrictedLockUtils.EnforcedAdmin.class));

        Preference pref = new ConversationPriorityPreference(mContext, null);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void testUpdateState_notConfigurable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByOEM()).thenReturn(true);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);

        Preference pref = new ConversationPriorityPreference(mContext, null);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void testUpdateState_systemButConfigurable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByOEM()).thenReturn(false);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);

        Preference pref = new ConversationPriorityPreference(mContext, null);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
    }

    @Test
    public void testUpdateState_defaultApp() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByCriticalDeviceFunction()).thenReturn(true);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);

        Preference pref = new ConversationPriorityPreference(mContext, null);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
    }

    @Test
    public void testUpdateState() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_HIGH);
        channel.setImportantConversation(true);
        channel.setOriginalImportance(IMPORTANCE_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);

        ConversationPriorityPreference pref = mock(ConversationPriorityPreference.class);
        mController.updateState(pref);

        verify(pref, times(1)).setConfigurable(anyBoolean());
        verify(pref, times(1)).setImportance(IMPORTANCE_HIGH);
        verify(pref, times(1)).setOriginalImportance(IMPORTANCE_DEFAULT);
        verify(pref, times(1)).setPriorityConversation(true);
    }

    @Test
    public void testImportanceLowToImportant() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_LOW);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        ConversationPriorityPreference pref = new ConversationPriorityPreference(mContext, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, new Pair(IMPORTANCE_HIGH, true));

        assertEquals(IMPORTANCE_HIGH, channel.getImportance());
        assertTrue(channel.canBubble());
        assertTrue(channel.isImportantConversation());
    }
    @Test
    public void testImportanceLowToDefault() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_LOW);
        channel.setAllowBubbles(false);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        ConversationPriorityPreference pref = new ConversationPriorityPreference(mContext, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, new Pair(IMPORTANCE_HIGH, false));

        assertEquals(IMPORTANCE_HIGH, channel.getImportance());
        assertFalse(channel.canBubble());
        assertFalse(channel.isImportantConversation());
    }

    @Test
    public void testImportanceDefaultToLow() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_DEFAULT);
        channel.setAllowBubbles(false);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        ConversationPriorityPreference pref = new ConversationPriorityPreference(mContext, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, new Pair(IMPORTANCE_LOW, false));

        assertEquals(IMPORTANCE_LOW, channel.getImportance());
        assertFalse(channel.canBubble());
        assertFalse(channel.isImportantConversation());
    }

    @Test
    public void testImportanceLowToDefault_bubblesMaintained() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_LOW);
        channel.setAllowBubbles(true);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        ConversationPriorityPreference pref = new ConversationPriorityPreference(mContext, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, new Pair(IMPORTANCE_DEFAULT, false));

        assertEquals(IMPORTANCE_DEFAULT, channel.getImportance());
        assertTrue(channel.canBubble());
        assertFalse(channel.isImportantConversation());
    }

    @Test
    public void testImportancePriorityToDefault() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_HIGH);
        channel.setAllowBubbles(true);
        channel.setImportantConversation(true);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, null);

        ConversationPriorityPreference pref = new ConversationPriorityPreference(mContext, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, new Pair(IMPORTANCE_HIGH, false));

        assertEquals(IMPORTANCE_HIGH, channel.getImportance());
        assertFalse(channel.canBubble());
        assertFalse(channel.isImportantConversation());
    }
}
