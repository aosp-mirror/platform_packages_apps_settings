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

package com.android.settings.notification.zen;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.notification.zen.ZenModeBackend;
import com.android.settings.notification.zen.ZenModeDurationPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ZenModeDurationPreferenceControllerTest {
    private ZenModeDurationPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private NotificationManager.Policy mPolicy;
    private ContentResolver mContentResolver;
    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mController = new ZenModeDurationPreferenceController(mContext, mock(Lifecycle.class));
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
    }

    @Test
    public void updateState_DurationForever() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                Settings.Secure.ZEN_DURATION_FOREVER);

        assertEquals(mContext.getString(R.string.zen_mode_duration_summary_forever),
                mController.getSummary());
    }

    @Test
    public void updateState_DurationPrompt() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                Settings.Secure.ZEN_DURATION_PROMPT);

        assertEquals(mContext.getString(R.string.zen_mode_duration_summary_always_prompt),
                mController.getSummary());
    }

    @Test
    public void updateState_DurationCustom() {
        int zenDuration = 45;
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                zenDuration);

        assertEquals(mContext.getString(R.string.zen_mode_duration_summary_time_minutes,
                zenDuration), mController.getSummary());
    }
}
