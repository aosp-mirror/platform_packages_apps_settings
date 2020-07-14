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
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BlockPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;

    @Mock
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    private BlockPreferenceController mController;
    @Mock
    private LayoutPreference mPreference;
    private SwitchBar mSwitch;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = spy(new BlockPreferenceController(mContext, mDependentFieldListener, mBackend));
        mSwitch = new SwitchBar(mContext);
        when(mPreference.findViewById(R.id.switch_bar)).thenReturn(mSwitch);
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(LayoutPreference.class));
        mController.onSwitchChanged(null, false);
    }

    @Test
    public void testIsAvailable_notIfNull() {
        mController.onResume(null, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channelNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_channelNonDefault() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_ifChannelDefault() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        when(channel.getId()).thenReturn(DEFAULT_CHANNEL_ID);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_GroupNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        mController.onResume(appRow, null, mock(NotificationChannelGroup.class), null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_AppNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        mController.onResume(appRow, null, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_systemApp() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_nonSystemApp() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = false;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsEnabled_lockedApp() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.lockedImportance = true;
        appRow.systemApp = true;
        mController.onResume(appRow, null, null, null, null, null);
        mController.updateState(mPreference);
        assertFalse(mSwitch.isEnabled());
    }

    @Test
    public void testIsEnabled_GroupNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        mController.onResume(appRow, null, mock(NotificationChannelGroup.class), null, null, null);
        mController.updateState(mPreference);
        assertFalse(mSwitch.isEnabled());
    }

    @Test
    public void testIsEnabled_systemAppNotBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        mController.onResume(appRow, null, null, null, null, null);
        mController.updateState(mPreference);
        assertFalse(mSwitch.isEnabled());
    }

    @Test
    public void testIsEnabled_systemAppBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.systemApp = true;
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_DEFAULT);
        channel.setBlockable(true);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);
        assertTrue(mSwitch.isEnabled());
    }

    @Test
    public void testIsEnabled_lockedChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByOEM()).thenReturn(true);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);

        mController.updateState(mPreference);

        assertFalse(mSwitch.isEnabled());
    }

    @Test
    public void testIsEnabled_defaultAppChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByCriticalDeviceFunction()).thenReturn(true);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);

        mController.updateState(mPreference);

        assertFalse(mSwitch.isEnabled());
    }

    @Test
    public void testIsEnabled_channel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);

        mController.updateState(mPreference);

        assertTrue(mSwitch.isEnabled());
    }

    @Test
    public void testIsEnabled_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null);

        mController.updateState(mPreference);

        assertTrue(mSwitch.isEnabled());
    }

    @Test
    public void testUpdateState_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null);
        mController.updateState(mPreference);

        assertNotNull(mPreference.findViewById(R.id.switch_bar));

        assertFalse(mSwitch.isChecked());

        appRow.banned = false;
        mController.onResume(appRow, null, null, null, null, null);
        mController.updateState(mPreference);

        assertTrue(mSwitch.isChecked());
    }

    @Test
    public void testUpdateState_group() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannelGroup group = mock(NotificationChannelGroup.class);
        when(group.isBlocked()).thenReturn(true);
        mController.onResume(appRow, null, group, null, null, null);
        mController.updateState(mPreference);

        assertFalse(mSwitch.isChecked());

        appRow.banned = true;
        mController.onResume(appRow, null, group, null, null, null);
        when(group.isBlocked()).thenReturn(true);
        mController.updateState(mPreference);

        assertFalse(mSwitch.isChecked());

        appRow.banned = false;
        mController.onResume(appRow, null, group, null, null, null);
        when(group.isBlocked()).thenReturn(false);
        mController.updateState(mPreference);

        assertTrue(mSwitch.isChecked());
    }

    @Test
    public void testUpdateState_channelBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("a", "a", IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);

        assertFalse(mSwitch.isChecked());

        appRow.banned = true;
        channel = new NotificationChannel("a", "a", IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);

        assertFalse(mSwitch.isChecked());

        appRow.banned = false;
        channel = new NotificationChannel("a", "a", IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);

        assertTrue(mSwitch.isChecked());
    }

    @Test
    public void testUpdateState_noCrashIfCalledTwice() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("a", "a", IMPORTANCE_LOW);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);
        mController.updateState(mPreference);
    }

    @Test
    public void testUpdateState_doesNotResetImportance() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("a", "a", IMPORTANCE_LOW);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);

        assertEquals(IMPORTANCE_LOW, channel.getImportance());
    }

    @Test
    public void testOnSwitchChanged_channel_default() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "pkg";
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_UNSPECIFIED);
        when(mBackend.onlyHasDefaultChannel(anyString(), anyInt())).thenReturn(true);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);

        mController.onSwitchChanged(null, false);
        assertEquals(IMPORTANCE_NONE, channel.getImportance());
        assertTrue(appRow.banned);

        mController.onSwitchChanged(null, true);
        assertEquals(IMPORTANCE_UNSPECIFIED, channel.getImportance());
        assertFalse(appRow.banned);

        verify(mBackend, times(2)).updateChannel(any(), anyInt(), any());

        verify(mBackend, times(2)).setNotificationsEnabledForPackage(
                anyString(), anyInt(), anyBoolean());
    }

    @Test
    public void testOnSwitchChanged_channel_nonDefault() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("a", "a", IMPORTANCE_HIGH);
        channel.setOriginalImportance(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null, null, null);
        mController.updateState(mPreference);

        mController.onSwitchChanged(null, false);
        assertEquals(IMPORTANCE_NONE, channel.getImportance());

        mController.onSwitchChanged(null, true);
        assertEquals(IMPORTANCE_HIGH, channel.getImportance());

        verify(mBackend, times(2)).updateChannel(any(), anyInt(), any());
    }
}
