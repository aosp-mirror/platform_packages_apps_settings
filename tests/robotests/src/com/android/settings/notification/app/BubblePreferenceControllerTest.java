/*
 * Copyright (C) 2019 The Android Open Source Project
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
import static android.app.NotificationManager.BUBBLE_PREFERENCE_ALL;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_NONE;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_SELECTED;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.provider.Settings.Secure.NOTIFICATION_BUBBLES;

import static com.android.settings.notification.BubbleHelper.SYSTEM_WIDE_OFF;
import static com.android.settings.notification.BubbleHelper.SYSTEM_WIDE_ON;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.ConversationChannelWrapper;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowActivityManager.class,
})
public class BubblePreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

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
    private FragmentManager mFragmentManager;
    @Mock
    private NotificationSettings.DependentFieldListener mListener;

    private BubblePreferenceController mController;
    private BubblePreferenceController mAppPageController;

    @Before
    public void setUp() {
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = ApplicationProvider.getApplicationContext();
        when(mFragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));
        mController = spy(new BubblePreferenceController(mContext, mFragmentManager, mBackend,
                false /* isAppPage */, mListener));
        mAppPageController = spy(new BubblePreferenceController(mContext, mFragmentManager,
                mBackend, true /* isAppPage */, mListener));
        List<ConversationChannelWrapper> convos = new ArrayList<>();
        convos.add(mock(ConversationChannelWrapper.class));
        when(mBackend.getConversations(anyString(), anyInt())).thenReturn(
                new ParceledListSlice<>(convos));
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(RestrictedSwitchPreference.class));
        mController.onPreferenceChange(mock(RestrictedSwitchPreference.class), true);
    }

    @Test
    public void isAvailable_notIfAppBlocked() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class), null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfChannelBlocked() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_channel_notIfAppOff() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_NONE;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null, null);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void isNotAvailable_ifOffGlobally_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void isNotAvailable_ifLowRam() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null, null);

        final ShadowActivityManager activityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setIsLowRamDevice(true);
        assertFalse(mController.isAvailable());
    }


    @Test
    public void isAvailable_notIfOffGlobally_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_ifNotLowRam() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        final ShadowActivityManager activityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setIsLowRamDevice(false);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_app_evenIfOffGlobally() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mAppPageController.onResume(appRow, null, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);

        assertTrue(mAppPageController.isAvailable());
    }

    @Test
    public void isAvailable_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_defaultChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        when(channel.getId()).thenReturn(DEFAULT_CHANNEL_ID);
        mController.onResume(appRow, channel, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);

        mController.onResume(appRow, channel, null, null, null, null, null);
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_filteredIn() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null,
                ImmutableList.of(NotificationChannel.EDIT_CONVERSATION));
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_filteredOut() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null, new ArrayList<>());
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void updateState_disabledByAdmin() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(new NotificationBackend.AppRow(), channel, null,
                null, null, mock(RestrictedLockUtils.EnforcedAdmin.class), null);

        Preference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void updateState_app_disabledByAdmin() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        mAppPageController.onResume(appRow, channel, null,
                null, null, mock(RestrictedLockUtils.EnforcedAdmin.class), null);

        BubblePreference pref = new BubblePreference(mContext);
        mAppPageController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void updateState_channel() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.canBubble()).thenReturn(true);
        mController.onResume(appRow, channel, null, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        mController.updateState(pref);

        assertTrue(pref.isChecked());

        when(channel.canBubble()).thenReturn(false);
        mController.onResume(appRow, channel, null, null, null, null, null);
        mController.updateState(pref);

        assertFalse(pref.isChecked());
    }

    @Test
    public void updateState_app() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.label = "App!";
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        BubblePreference pref = new BubblePreference(mContext);
        mAppPageController.updateState(pref);
        assertEquals(BUBBLE_PREFERENCE_ALL, pref.getSelectedPreference());

        appRow.bubblePreference = BUBBLE_PREFERENCE_NONE;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        mAppPageController.updateState(pref);
        assertEquals(BUBBLE_PREFERENCE_NONE, pref.getSelectedPreference());

        appRow.bubblePreference = BUBBLE_PREFERENCE_SELECTED;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        mAppPageController.updateState(pref);
        assertEquals(BUBBLE_PREFERENCE_SELECTED, pref.getSelectedPreference());
    }

    @Test
    public void updateState_app_offGlobally() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, SYSTEM_WIDE_OFF);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.label = "App!";
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        BubblePreference pref = new BubblePreference(mContext);
        mAppPageController.updateState(pref);
        assertEquals(BUBBLE_PREFERENCE_NONE, pref.getSelectedPreference());
    }

    @Test
    public void onPreferenceChange_on_channel() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.bubblePreference = BUBBLE_PREFERENCE_SELECTED;
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_LOW);
        mController.onResume(appRow, channel, null, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, true);
        assertTrue(channel.canBubble());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void onPreferenceChange_off_channel() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.bubblePreference = BUBBLE_PREFERENCE_SELECTED;
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null, null);

        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, false);
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
        assertFalse(channel.canBubble());
    }


    @Test
    public void onPreferenceChange_app_all() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.bubblePreference = BUBBLE_PREFERENCE_NONE;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        BubblePreference pref = new BubblePreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mAppPageController.displayPreference(mScreen);
        mAppPageController.updateState(pref);

        mAppPageController.onPreferenceChange(pref, BUBBLE_PREFERENCE_ALL);

        assertEquals(appRow.bubblePreference, BUBBLE_PREFERENCE_ALL);
        verify(mBackend, times(1)).setAllowBubbles(any(), anyInt(), eq(BUBBLE_PREFERENCE_ALL));
    }

    @Test
    public void testOnPreferenceChange_app_all_offGlobally() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.bubblePreference = BUBBLE_PREFERENCE_NONE;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        BubblePreference pref = new BubblePreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mAppPageController.displayPreference(mScreen);
        mAppPageController.updateState(pref);

        mAppPageController.onPreferenceChange(pref, BUBBLE_PREFERENCE_ALL);

        assertEquals(appRow.bubblePreference, BUBBLE_PREFERENCE_NONE);
        verify(mBackend, never()).setAllowBubbles(any(), anyInt(), eq(BUBBLE_PREFERENCE_ALL));
        verify(mFragmentManager, times(1)).beginTransaction();
    }

    @Test
    public void onPreferenceChange_app_selected() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        BubblePreference pref = new BubblePreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mAppPageController.displayPreference(mScreen);
        mAppPageController.updateState(pref);

        mAppPageController.onPreferenceChange(pref, BUBBLE_PREFERENCE_NONE);

        assertEquals(BUBBLE_PREFERENCE_NONE, appRow.bubblePreference);
        verify(mBackend, times(1)).setAllowBubbles(any(), anyInt(), eq(BUBBLE_PREFERENCE_NONE));
    }

    @Test
    public void onPreferenceChange_app_none() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        BubblePreference pref = new BubblePreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mAppPageController.displayPreference(mScreen);
        mAppPageController.updateState(pref);

        mAppPageController.onPreferenceChange(pref, BUBBLE_PREFERENCE_NONE);

        assertEquals(BUBBLE_PREFERENCE_NONE, appRow.bubblePreference);
        verify(mBackend, times(1)).setAllowBubbles(any(), anyInt(), eq(BUBBLE_PREFERENCE_NONE));
    }

    @Test
    public void onPreferenceChange_dependentFieldListenerCalled() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "a";
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mAppPageController.onResume(appRow, null, null, null, null, null, null);

        BubblePreference pref = new BubblePreference(mContext);
        mAppPageController.onPreferenceChange(pref, BUBBLE_PREFERENCE_NONE);

        verify(mListener, times(1)).onFieldValueChanged();
    }
}
