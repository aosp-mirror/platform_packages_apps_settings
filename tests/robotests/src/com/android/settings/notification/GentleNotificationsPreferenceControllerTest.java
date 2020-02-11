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

import static com.android.settings.notification.BadgingNotificationPreferenceController.OFF;
import static com.android.settings.notification.BadgingNotificationPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.Preference;

@RunWith(RobolectricTestRunner.class)
public class GentleNotificationsPreferenceControllerTest {

    private Context mContext;

    @Mock
    NotificationBackend mBackend;

    private GentleNotificationsPreferenceController mController;
    private Preference mPreference;

    private static final String KEY = "gentle_notifications";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new GentleNotificationsPreferenceController(mContext, KEY);
        mController.setBackend(mBackend);
        mPreference = new Preference(mContext);
    }

    @Test
    public void display_shouldDisplay() {
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void getSummary_lock() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.gentle_notifications_display_summary_shade_lock));
    }

    @Test
    public void getSummary_status() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 0);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.gentle_notifications_display_summary_shade_status));
    }

    @Test
    public void getSummary_both() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(
                R.string.gentle_notifications_display_summary_shade_status_lock));
    }

    @Test
    public void getSummary_neither() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 0);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.gentle_notifications_display_summary_shade));
    }
}
