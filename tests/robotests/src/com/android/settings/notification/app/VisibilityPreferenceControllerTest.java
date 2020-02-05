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

import static android.app.Notification.VISIBILITY_PRIVATE;
import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.NotificationManager.VISIBILITY_NO_OVERRIDE;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.RestrictedListPreference;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.testutils.shadow.ShadowRestrictionUtils;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowRestrictionUtils.class)
public class VisibilityPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private LockPatternUtils mLockUtils;
    @Mock
    private UserManager mUm;
    @Mock
    private DevicePolicyManager mDm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)

    private VisibilityPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        shadowApplication.setSystemService(Context.DEVICE_POLICY_SERVICE, mDm);
        mContext = RuntimeEnvironment.application;
        mController = spy(new VisibilityPreferenceController(mContext, mLockUtils, mBackend));

        // by default the lockscreen is secure
        when(mLockUtils.isSecure(anyInt())).thenReturn(true);
        // and notifications are visible in redacted form
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 1);
        // and not restricted
        ShadowRestrictionUtils.setRestricted(false);
        // with no managed profile
        UserInfo userInfo = new UserInfo();
        when(mUm.getUserInfo(anyInt())).thenReturn(userInfo);
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(RestrictedListPreference.class));
        mController.onPreferenceChange(mock(RestrictedListPreference.class), true);
    }

    @Test
    public void testIsAvailable_notSecure() {
        when(mLockUtils.isSecure(anyInt())).thenReturn(false);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfNotImportant() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_MIN);
        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "", IMPORTANCE_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());

        channel = new NotificationChannel("", "", IMPORTANCE_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testUpdateState_disabledByAdmin_disableSecure() {
        ShadowRestrictionUtils.setRestricted(true);
        UserInfo userInfo = new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE);
        when(mUm.getUserInfo(anyInt())).thenReturn(userInfo);
        List<ComponentName> components = new ArrayList<>();
        components.add(new ComponentName("", ""));
        when(mDm.getActiveAdminsAsUser(anyInt())).thenReturn(components);
        when(mDm.getKeyguardDisabledFeatures(any(), anyInt()))
                .thenReturn(KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);

        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, mock(
                RestrictedLockUtils.EnforcedAdmin.class));

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        verify(pref, times(2)).addRestrictedItem(any());
    }

    @Test
    public void testUpdateState_disabledByAdmin_disableUnredacted() {
        ShadowRestrictionUtils.setRestricted(true);
        UserInfo userInfo = new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE);
        when(mUm.getUserInfo(anyInt())).thenReturn(userInfo);
        List<ComponentName> components = new ArrayList<>();
        components.add(new ComponentName("", ""));
        when(mDm.getActiveAdminsAsUser(anyInt())).thenReturn(components);
        when(mDm.getKeyguardDisabledFeatures(any(), anyInt()))
                .thenReturn(KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);

        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, mock(
                RestrictedLockUtils.EnforcedAdmin.class));

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        verify(pref, times(1)).addRestrictedItem(any());
    }

    @Test
    public void testUpdateState_noLockScreenNotificationsGlobally() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        ArgumentCaptor<CharSequence[]> argumentCaptor =
                ArgumentCaptor.forClass(CharSequence[].class);
        verify(pref, times(1)).setEntryValues(argumentCaptor.capture());
        assertFalse(toStringList(argumentCaptor.getValue())
                .contains(String.valueOf(VISIBILITY_NO_OVERRIDE)));
        assertFalse(toStringList(argumentCaptor.getValue())
                .contains(String.valueOf(VISIBILITY_PRIVATE)));
    }

    @Test
    public void testUpdateState_noLockScreenNotificationsGloballyInProfile() {
        final int primaryUserId = 2;
        final UserInfo primaryUserInfo = new UserInfo(primaryUserId, "user 2", 0);
        when(mUm.getProfileParent(anyInt())).thenReturn(primaryUserInfo);

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0, primaryUserId);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        ArgumentCaptor<CharSequence[]> argumentCaptor =
                ArgumentCaptor.forClass(CharSequence[].class);
        verify(pref, times(1)).setEntryValues(argumentCaptor.capture());
        assertFalse(toStringList(argumentCaptor.getValue())
                .contains(String.valueOf(VISIBILITY_NO_OVERRIDE)));
        assertFalse(toStringList(argumentCaptor.getValue())
                .contains(String.valueOf(VISIBILITY_PRIVATE)));
    }

    @Test
    public void testUpdateState_noPrivateLockScreenNotificationsGlobally() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        ArgumentCaptor<CharSequence[]> argumentCaptor =
                ArgumentCaptor.forClass(CharSequence[].class);
        verify(pref, times(1)).setEntryValues(argumentCaptor.capture());
        assertEquals(2, toStringList(argumentCaptor.getValue()).size());
        assertFalse(toStringList(argumentCaptor.getValue())
                .contains(String.valueOf(VISIBILITY_NO_OVERRIDE)));
    }

    @Test
    public void testUpdateState_noGlobalRestriction() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        ArgumentCaptor<CharSequence[]> argumentCaptor =
                ArgumentCaptor.forClass(CharSequence[].class);
        verify(pref, times(1)).setEntryValues(argumentCaptor.capture());
        List<String> values = toStringList(argumentCaptor.getValue());
        assertEquals(3, values.size());
        assertTrue(values.contains(String.valueOf(VISIBILITY_NO_OVERRIDE)));
        assertTrue(values.contains(String.valueOf(VISIBILITY_PRIVATE)));
        assertTrue(values.contains(String.valueOf(Notification.VISIBILITY_SECRET)));
    }

    private static List<String> toStringList(CharSequence[] charSequences) {
        List<String> result = new ArrayList<>();
        for (CharSequence charSequence : charSequences) {
            result.add(charSequence.toString());
        }
        return result;
    }

    @Test
    public void testUpdateState_noChannelOverride() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getLockscreenVisibility()).thenReturn(VISIBILITY_NO_OVERRIDE);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(pref, times(1)).setValue(argumentCaptor.capture());

        assertEquals(String.valueOf(VISIBILITY_PRIVATE), argumentCaptor.getValue());
    }

    @Test
    public void testUpdateState_channelOverride() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getLockscreenVisibility()).thenReturn(Notification.VISIBILITY_SECRET);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(pref, times(1)).setValue(argumentCaptor.capture());

        assertEquals(String.valueOf(Notification.VISIBILITY_SECRET), argumentCaptor.getValue());
    }

    @Test
    public void testOnPreferenceChange_noOverride() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", 4);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, String.valueOf(VISIBILITY_PRIVATE));

        assertEquals(VISIBILITY_NO_OVERRIDE, channel.getLockscreenVisibility());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void testOnPreferenceChange_override() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", 4);
        channel.setLockscreenVisibility(VISIBILITY_NO_OVERRIDE);
        mController.onResume(appRow, channel, null, null, null, null);

        RestrictedListPreference pref = mock(RestrictedListPreference.class);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, String.valueOf(Notification.VISIBILITY_SECRET));

        assertEquals(Notification.VISIBILITY_SECRET, channel.getLockscreenVisibility());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }
}
