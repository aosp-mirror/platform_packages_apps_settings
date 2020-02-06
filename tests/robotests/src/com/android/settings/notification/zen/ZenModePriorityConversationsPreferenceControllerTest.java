/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_NONE;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CONVERSATIONS;
import static android.provider.Settings.Global.ZEN_MODE;
import static android.provider.Settings.Global.ZEN_MODE_ALARMS;
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
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
public class ZenModePriorityConversationsPreferenceControllerTest {

    private ZenModePriorityConversationsPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private ListPreference mockPref;
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

        mContext = RuntimeEnvironment.application;
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);

        when(mBackend.getPriorityConversationSenders())
            .thenReturn(CONVERSATION_SENDERS_IMPORTANT);
        when(mBackend.getAlarmsTotalSilencePeopleSummary(PRIORITY_CATEGORY_CONVERSATIONS))
                .thenCallRealMethod();
        when(mBackend.getConversationSummary()).thenCallRealMethod();

        mController = new ZenModePriorityConversationsPreferenceController(
                mContext, mock(Lifecycle.class));
        ReflectionHelpers.setField(mController, "mBackend", mBackend);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(mockPref);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_TotalSilence() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_NO_INTERRUPTIONS);

        when(mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_CONVERSATIONS)).thenReturn(true);
        final ListPreference mockPref = mock(ListPreference.class);
        mController.updateState(mockPref);

        verify(mockPref).setEnabled(false);
        verify(mockPref).setSummary(R.string.zen_mode_from_no_conversations);
    }

    @Test
    public void updateState_AlarmsOnly() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_ALARMS);

        final ListPreference mockPref = mock(ListPreference.class);
        mController.updateState(mockPref);

        verify(mockPref).setEnabled(false);
        verify(mockPref).setSummary(R.string.zen_mode_from_no_conversations);
    }

    @Test
    public void updateState_Priority_important() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_CONVERSATIONS)).thenReturn(true);

        mController.updateState(mockPref);

        verify(mockPref).setEnabled(true);
        verify(mockPref).setSummary(R.string.zen_mode_from_important_conversations);
        verify(mockPref).setValue(String.valueOf(CONVERSATION_SENDERS_IMPORTANT));
    }

    @Test
    public void updateState_Priority_all() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityConversationSenders()).thenReturn(CONVERSATION_SENDERS_ANYONE);
        when(mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_CONVERSATIONS)).thenReturn(true);


        mController.updateState(mockPref);

        verify(mockPref).setEnabled(true);
        verify(mockPref).setSummary(R.string.zen_mode_from_all_conversations);
        verify(mockPref).setValue(String.valueOf(CONVERSATION_SENDERS_ANYONE));
    }

    @Test
    public void updateState_Priority_none() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityConversationSenders()).thenReturn(CONVERSATION_SENDERS_NONE);
        when(mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_CONVERSATIONS)).thenReturn(false);

        mController.updateState(mockPref);

        verify(mockPref).setEnabled(true);
        verify(mockPref).setSummary(R.string.zen_mode_from_no_conversations);
        verify(mockPref).setValue(String.valueOf(CONVERSATION_SENDERS_NONE));
    }

    @Test
    public void onPreferenceChange_noneToImportant() {
        // start with none

        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityConversationSenders()).thenReturn(CONVERSATION_SENDERS_NONE);
        when(mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_CONVERSATIONS)).thenReturn(false);

        mController.updateState(mockPref);
        reset(mBackend);

        mController.onPreferenceChange(mockPref, String.valueOf(CONVERSATION_SENDERS_IMPORTANT));

        verify(mBackend).saveConversationSenders(CONVERSATION_SENDERS_IMPORTANT);
        verify(mBackend).getPriorityConversationSenders();
    }

    @Test
    public void onPreferenceChange_allToNone() {
        // start with none

        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityConversationSenders()).thenReturn(CONVERSATION_SENDERS_ANYONE);
        when(mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_CONVERSATIONS)).thenReturn(true);

        mController.updateState(mockPref);
        reset(mBackend);

        mController.onPreferenceChange(mockPref, String.valueOf(CONVERSATION_SENDERS_NONE));

        verify(mBackend).saveConversationSenders(CONVERSATION_SENDERS_NONE);
        verify(mBackend).getPriorityConversationSenders();
    }
}