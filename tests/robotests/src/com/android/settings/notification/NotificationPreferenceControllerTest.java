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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertEquals;
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
import android.os.Build;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.NotificationChannelGroupWrapper;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = Build.VERSION_CODES.O)
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
        mContext = shadowApplication.getApplicationContext();
        mController = new TestPreferenceController(mContext, mBackend);
    }

    @Test
    public void noCrashIfNoOnResume() throws Exception {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
        assertFalse(mController.checkCanBeVisible(IMPORTANCE_UNSPECIFIED));
        mController.saveChannel();
        assertFalse(mController.isChannelConfigurable());
        assertFalse(mController.isChannelBlockable());
        assertFalse(mController.isChannelGroupBlockable());
    }

    @Test
    public void isAvailable_notIfNull() throws Exception {
        mController.onResume(null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfAppBlocked() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class),
                mock(NotificationChannelGroupWrapper.class), null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfChannelBlocked() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);

        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfChannelGroupBlocked() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        NotificationChannelGroupWrapper group = mock(NotificationChannelGroupWrapper.class);

        mController.onResume(appRow, channel, group, null);
        when(group.isBlocked()).thenReturn(true);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_DEFAULT);
        NotificationChannelGroupWrapper group = mock(NotificationChannelGroupWrapper.class);
        when(group.isBlocked()).thenReturn(false);

        mController.onResume(appRow, channel, group, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testOnResume() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        NotificationChannelGroupWrapper group = mock(NotificationChannelGroupWrapper.class);
        RestrictedLockUtils.EnforcedAdmin admin = mock(RestrictedLockUtils.EnforcedAdmin.class);

        mController.onResume(appRow, channel, group, admin);

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

        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.checkCanBeVisible(IMPORTANCE_MIN));
    }

    @Test
    public void testCanBeVisible_sameImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);

        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.checkCanBeVisible(IMPORTANCE_LOW));
    }

    @Test
    public void testCanBeVisible_greaterImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);

        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.checkCanBeVisible(IMPORTANCE_MIN));
    }

    @Test
    public void testCanBeVisible_lesserImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);

        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.checkCanBeVisible(IMPORTANCE_DEFAULT));
    }

    @Test
    public void testSaveImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_DEFAULT);

        mController.onResume(appRow, channel, null, null);
        mController.saveChannel();
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void testIsConfigurable() {
        String sameId = "bananas";
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.lockedChannelId = sameId;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(sameId);

        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.isChannelConfigurable());

        when(channel.getId()).thenReturn("something new");
        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.isChannelConfigurable());
    }

    @Test
    public void testIsChannelBlockable_nonSystemAppsBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockableSystem()).thenReturn(false);

        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_mostSystemAppsNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockableSystem()).thenReturn(false);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);

        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_someSystemAppsAreBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockableSystem()).thenReturn(true);

        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelBlockable_canUndoSystemBlock() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isBlockableSystem()).thenReturn(false);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);

        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.isChannelBlockable());
    }

    @Test
    public void testIsChannelGroupBlockable_nonSystemBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = false;
        NotificationChannelGroupWrapper group = mock(NotificationChannelGroupWrapper.class);
        when(group.isBlocked()).thenReturn(false);

        mController.onResume(appRow, null, group, null);
        assertTrue(mController.isChannelGroupBlockable());
    }

    @Test
    public void testIsChannelGroupBlockable_SystemNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannelGroupWrapper group = mock(NotificationChannelGroupWrapper.class);
        when(group.isBlocked()).thenReturn(false);

        mController.onResume(appRow, null, group, null);
        assertFalse(mController.isChannelGroupBlockable());
    }

    @Test
    public void testIsChannelGroupBlockable_canUndoSystemBlock() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannelGroupWrapper group = mock(NotificationChannelGroupWrapper.class);
        when(group.isBlocked()).thenReturn(true);

        mController.onResume(appRow, null, group, null);
        assertTrue(mController.isChannelGroupBlockable());
    }

    private final class TestPreferenceController extends NotificationPreferenceController {

        public TestPreferenceController(Context context,
                NotificationBackend backend) {
            super(context, backend);
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }
    }
}
