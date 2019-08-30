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

package com.android.settings.notification;

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import static com.android.settings.notification.BubbleSummaryPreferenceController.SYSTEM_WIDE_OFF;
import static com.android.settings.notification.BubbleSummaryPreferenceController.SYSTEM_WIDE_ON;

import static junit.framework.TestCase.assertEquals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class BubbleSummaryPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;

    private BubbleSummaryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        mController = spy(new BubbleSummaryPreferenceController(mContext, mBackend));
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void testIsAvailable_notIfAppBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class), null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfOffGlobally() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(appRow, channel, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_app() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsNotAvailable_app_globalOff() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_defaultChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        when(channel.getId()).thenReturn(DEFAULT_CHANNEL_ID);
        mController.onResume(appRow, channel, null, null);
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void testUpdateState() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        mController.onResume(appRow, null, null, null);

        Preference pref = new Preference(mContext);
        mController.updateState(pref);
        assertNotNull(pref.getIntent());
    }

    @Test
    public void testGetSummary() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.allowBubbles = true;
        mController.onResume(appRow, null, null, null);

        assertEquals("On", mController.getSummary());

        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);
        assertEquals("Off", mController.getSummary());

        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        appRow.allowBubbles = false;
        mController.onResume(appRow, null, null, null);

        assertEquals("Off", mController.getSummary());
    }
}
