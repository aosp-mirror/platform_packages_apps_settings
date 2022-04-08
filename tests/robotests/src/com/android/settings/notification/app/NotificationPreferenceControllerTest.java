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

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import static org.junit.Assert.assertEquals;
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
public class NotificationPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;

    private TestPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = new TestPreferenceController(mContext, mBackend);
    }

    @Test
    public void noCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
        assertFalse(mController.checkCanBeVisible(IMPORTANCE_UNSPECIFIED));
        mController.saveChannel();
        assertFalse(mController.isChannelBlockable());
        assertFalse(mController.isChannelGroupBlockable());
    }

    @Test
    public void isAvailable_notIfNull() {
        mController.onResume(null, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfAppBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class),
                mock(NotificationChannelGroup.class), null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfChannelBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(false);
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);

        mController.onResume(appRow, channel, group, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfChannelGroupBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_DEFAULT);
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);

        mController.onResume(appRow, channel, group, null, null, null);
        when(group.isBlocked()).thenReturn(true);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_DEFAULT);
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(false);

        mController.onResume(appRow, channel, group, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testOnResume() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        RestrictedLockUtils.EnforcedAdmin admin = mock(RestrictedLockUtils.EnforcedAdmin.class);

        mController.onResume(appRow, channel, group, null, null, admin);

        assertEquals(appRow, mController.mAppRow);
        assertEquals(channel, mController.mChannel);
        assertEquals(group, mController.mChannelGroup);
        assertEquals(admin, mController.mAdmin);
    }

    @Test
    public void testCanBeVisible_unspecified() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_UNSPECIFIED);

        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.checkCanBeVisible(IMPORTANCE_MIN));
    }

    @Test
    public void testCanBeVisible_sameImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);

        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.checkCanBeVisible(IMPORTANCE_LOW));
    }

    @Test
    public void testCanBeVisible_greaterImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);

        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.checkCanBeVisible(IMPORTANCE_MIN));
    }

    @Test
    public void testCanBeVisible_lesserImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);

        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.checkCanBeVisible(IMPORTANCE_DEFAULT));
    }

    @Test
    public void testSaveImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_DEFAULT);

        mController.onResume(appRow, channel, null, null, null, null);
        mController.saveChannel();
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void testIsBlockable_oemWhitelist() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByOEM()).thenReturn(true);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);

        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isChannelBlockable());

        when(channel.isImportanceLockedByOEM()).thenReturn(false);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isChannelBlockable());
    }

    @Test
    public void testIsBlockable_defaultApp() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);
        when(channel.isImportanceLockedByCriticalDeviceFunction()).thenReturn(true);

        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_nonSystemAppsBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockable()).thenReturn(false);

        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_mostSystemAppsNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockable()).thenReturn(false);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);

        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_someSystemAppsAreBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockable()).thenReturn(true);

        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_canUndoSystemBlock() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockable()).thenReturn(false);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);

        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelGroupBlockable_nonSystemBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = false;
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(false);

        mController.onResume(appRow, null, group, null, null, null);
        assertTrue(mController.isChannelGroupBlockable());
    }

    @Test
    public void testIsChannelBlockable_oemLocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByOEM()).thenReturn(true);
        when(channel.getImportance()).thenReturn(IMPORTANCE_DEFAULT);

        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_criticalDeviceFunction() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByCriticalDeviceFunction()).thenReturn(true);
        when(channel.getImportance()).thenReturn(IMPORTANCE_DEFAULT);

        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelGroupBlockable_SystemNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(false);

        mController.onResume(appRow, null, group, null, null, null);
        assertFalse(mController.isChannelGroupBlockable());
    }

    @Test
    public void testIsChannelGroupBlockable_canUndoSystemBlock() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(true);

        mController.onResume(appRow, null, group, null, null, null);
        assertTrue(mController.isChannelGroupBlockable());
    }

    @Test
    public void testIsDefaultChannel_noChannel() {
        mController.onResume(mock(NotificationBackend.AppRow.class), null, null, null, null, null);

        assertFalse(mController.isDefaultChannel());
    }

    @Test
    public void testIsDefaultChannel_nonDefaultChannel() {
        NotificationChannel channel = mock(NotificationChannel.class);
        mController.onResume(mock(NotificationBackend.AppRow.class), channel, null, null, null, null);

        assertFalse(mController.isDefaultChannel());
    }

    @Test
    public void testIsDefaultChannel() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(NotificationChannel.DEFAULT_CHANNEL_ID);
        mController.onResume(mock(NotificationBackend.AppRow.class), channel, null, null, null, null);

        assertTrue(mController.isDefaultChannel());
    }

    private final class TestPreferenceController extends NotificationPreferenceController {

        private TestPreferenceController(Context context, NotificationBackend backend) {
            super(context, backend);
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }
    }
}
