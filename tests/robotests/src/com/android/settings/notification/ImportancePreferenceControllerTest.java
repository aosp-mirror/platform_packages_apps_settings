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

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ImportancePreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;

    private ImportancePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = shadowApplication.getApplicationContext();
        mController = spy(new ImportancePreferenceController(mContext));
    }

    @Test
    public void testNoCrashIfNoOnResume() throws Exception {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void testIsAvailable_notIfNull() throws Exception {
        mController.onResume(null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfAppBlocked() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class), null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfChannelBlocked() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_NONE);
        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notForDefaultChannel() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);
        when(channel.getId()).thenReturn(DEFAULT_CHANNEL_ID);
        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_LOW);
        mController.onResume(appRow, channel, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testUpdateState_disabledByAdmin() throws Exception {
        NotificationChannel channel = mock(NotificationChannel.class);
        mController.onResume(new NotificationBackend.AppRow(), channel, null, mock(
                RestrictedLockUtils.EnforcedAdmin.class));

        Preference pref = new Preference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
        assertNull(pref.getIntent());
    }

    @Test
    public void testUpdateState_notConfigurable() throws Exception {
        String lockedId = "locked";
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.lockedChannelId = lockedId;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(lockedId);
        mController.onResume(appRow, channel, null, null);

        Preference pref = new Preference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
        assertNull(pref.getIntent());
    }

    @Test
    public void testUpdateState() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null);

        Preference pref = new Preference(RuntimeEnvironment.application);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
        assertNotNull(pref.getIntent());
        assertFalse(TextUtils.isEmpty(pref.getSummary()));
    }
}
