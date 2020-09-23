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
import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.notification.app.BubblePreferenceController.SYSTEM_WIDE_OFF;
import static com.android.settings.notification.app.BubblePreferenceController.SYSTEM_WIDE_ON;

import static junit.framework.TestCase.assertEquals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BubbleSummaryPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    NotificationBackend.AppRow mAppRow;

    private BubbleSummaryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(true);
        mAppRow = new NotificationBackend.AppRow();
        mAppRow.pkg = "pkg";
        mAppRow.uid = 0;
        mController = spy(new BubbleSummaryPreferenceController(mContext, mBackend));
    }

    @Test
    public void isAvailable_noOnResume_shouldNotCrash() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void isAvailable_appBlocked_shouldReturnFalse() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, mock(NotificationChannel.class), null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_NOTIFICATION_BUBBLESisOn_shouldReturnTrue() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        mController.onResume(mAppRow, null, null, null, null, null);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_NOTIFICATION_BUBBLESisOn_neverSentMsg_shouldReturnFalse() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        mController.onResume(mAppRow, null, null, null, null, null);
        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(false);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_NOTIFICATION_BUBBLESisOff_shouldReturnFalse() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);
        mController.onResume(mAppRow, null, null, null, null, null);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_nonNullChannelNOTIFICATION_BUBBLESisOff_shouldReturnFalse() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        mController.onResume(mAppRow, channel, null, null, null, null);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_defaultChannelNOTIFICATION_BUBBLESisOn_shouldReturnTrue() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getImportance()).thenReturn(IMPORTANCE_HIGH);
        when(channel.getId()).thenReturn(DEFAULT_CHANNEL_ID);
        mController.onResume(mAppRow, channel, null, null, null, null);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_lowRam_shouldReturnFalse() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        mController.onResume(mAppRow, null, null, null, null, null);

        final ShadowActivityManager activityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setIsLowRamDevice(true);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notLowRam_shouldReturnTrue() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, SYSTEM_WIDE_ON);
        mController.onResume(mAppRow, null, null, null, null, null);

        final ShadowActivityManager activityManager =
               Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setIsLowRamDevice(false);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void updateState_setsIntent() {
        mAppRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mController.onResume(mAppRow, null, null, null, null, null);

        Preference pref = new Preference(mContext);
        mController.updateState(pref);
        assertNotNull(pref.getIntent());
    }

    @Test
    public void getSummary_NOTIFICATION_BUBBLESIsOff_returnsNoneString() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_OFF);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null);

        String noneString = mContext.getString(R.string.bubble_app_setting_none);
        assertEquals(noneString, mController.getSummary());
    }

    @Test
    public void getSummary_BUBBLE_PREFERENCE_NONEisSelected_returnsNoneString() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_ON);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_NONE;
        mController.onResume(appRow, null, null, null, null, null);

        String noneString = mContext.getString(R.string.bubble_app_setting_none);
        assertEquals(noneString, mController.getSummary());
    }

    @Test
    public void getSummary_BUBBLE_PREFERENCE_ALLisSelected_returnsAllString() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_ON);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mController.onResume(appRow, null, null, null, null, null);

        String allString = mContext.getString(R.string.bubble_app_setting_all);
        assertEquals(allString, mController.getSummary());
    }

    @Test
    public void getSummary_BUBBLE_PREFERENCE_SELECTEDisSelected_returnsSelectedString() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_ON);

        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_SELECTED;
        mController.onResume(appRow, null, null, null, null, null);

        String selectedString = mContext.getString(R.string.bubble_app_setting_selected);
        assertEquals(selectedString, mController.getSummary());
    }
}
