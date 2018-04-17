/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class ZenModeDurationPreferenceControllerTest {
    private ZenModeDurationPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private Preference mockPref;
    @Mock
    private NotificationManager.Policy mPolicy;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private ContentResolver mContentResolver;
    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = shadowApplication.getApplicationContext();
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mController = new ZenModeDurationPreferenceController(mContext, mock(Lifecycle.class),
                mock(FragmentManager.class));
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mockPref);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_DurationForever() {
        Settings.Global.putInt(mContentResolver, Settings.Global.ZEN_DURATION,
                Settings.Global.ZEN_DURATION_FOREVER);
        final Preference mockPref = mock(Preference.class);
        mController.updateState(mockPref);

        verify(mockPref).setSummary(mContext.getString(R.string.zen_mode_duration_summary_forever));
    }

    @Test
    public void updateState_DurationPrompt() {
        Settings.Global.putInt(mContentResolver, Settings.Global.ZEN_DURATION,
                Settings.Global.ZEN_DURATION_PROMPT);
        final Preference mockPref = mock(Preference.class);
        mController.updateState(mockPref);

        verify(mockPref).setSummary(mContext.getString(
                R.string.zen_mode_duration_summary_always_prompt));
    }

    @Test
    public void updateState_DurationCustom() {
        int zenDuration = 45;
        Settings.Global.putInt(mContentResolver, Settings.Global.ZEN_DURATION,
                zenDuration);
        final Preference mockPref = mock(Preference.class);
        mController.updateState(mockPref);

        verify(mockPref).setSummary(mContext.getResources().getString(
                R.string.zen_mode_duration_summary_time_minutes, zenDuration));
    }
}