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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.Preference;

@RunWith(RobolectricTestRunner.class)
public class GentleDrawablePreferenceControllerTest {

    private Context mContext;

    private GentleDrawablePreferenceController mController;
    @Mock
    private LayoutPreference mPreference;
    @Mock
    NotificationBackend mBackend;
    @Mock
    ImageView mView;

    private static final String KEY = "gentle_notifications";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new GentleDrawablePreferenceController(mContext, KEY);
        mController.setBackend(mBackend);
        when(mPreference.findViewById(R.id.drawable)).thenReturn(mView);
    }

    @Test
    public void display_shouldDisplay() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_lock() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(true);

        mController.updateState(mPreference);

        verify(mView).setImageResource(R.drawable.gentle_notifications_shade_lock);
    }

    @Test
    public void updateState_status() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 0);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(false);

        mController.updateState(mPreference);

        verify(mView).setImageResource(R.drawable.gentle_notifications_shade_status);
    }

    @Test
    public void updateState_both() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(false);

        mController.updateState(mPreference);

        verify(mView).setImageResource(R.drawable.gentle_notifications_shade_status_lock);
    }

    @Test
    public void updateState_neither() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 0);
        when(mBackend.shouldHideSilentStatusBarIcons(mContext)).thenReturn(true);

        mController.updateState(mPreference);

        verify(mView).setImageResource(R.drawable.gentle_notifications_shade);
    }
}
