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

import static android.provider.Settings.Global.ZEN_MODE;
import static android.provider.Settings.Global.ZEN_MODE_ALARMS;
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class ZenModeCallsPreferenceControllerTest {
    private ZenModeCallsPreferenceController mController;

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

    private final boolean CALLS_SETTINGS = true;
    private final int MOCK_CALLS_SENDERS = NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
    private final int SUMMARY_ID_MOCK_CALLS_SENDERS = R.string.zen_mode_from_starred;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = shadowApplication.getApplicationContext();
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);
        when(mBackend.getContactsSummary(ZenModeBackend.SOURCE_NONE))
                .thenCallRealMethod();
        when(mBackend.getContactsSummary(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS))
                .thenCallRealMethod();

        mController = new ZenModeCallsPreferenceController(mContext, mock(Lifecycle.class));
        ReflectionHelpers.setField(mController, "mBackend", mBackend);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mockPref);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_TotalSilence() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_NO_INTERRUPTIONS);
        when(mBackend.isPriorityCategoryEnabled(
                NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES))
                .thenReturn(false);
        final Preference mockPref = mock(Preference.class);
        mController.updateState(mockPref);

        verify(mockPref).setEnabled(false);
        verify(mockPref).setSummary(R.string.zen_mode_from_none);
    }

    @Test
    public void updateState_AlarmsOnly() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_ALARMS);

        final Preference mockPref = mock(Preference.class);
        mController.updateState(mockPref);

        verify(mockPref).setEnabled(false);
        verify(mockPref).setSummary(R.string.zen_mode_from_none);
    }

    @Test
    public void updateState_Priority() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.isPriorityCategoryEnabled(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS))
                .thenReturn(CALLS_SETTINGS);
        when(mBackend.getPriorityCallSenders()).thenReturn(MOCK_CALLS_SENDERS);

        mController.updateState(mockPref);

        verify(mockPref).setEnabled(true);
        verify(mockPref).setSummary(SUMMARY_ID_MOCK_CALLS_SENDERS);
    }
}