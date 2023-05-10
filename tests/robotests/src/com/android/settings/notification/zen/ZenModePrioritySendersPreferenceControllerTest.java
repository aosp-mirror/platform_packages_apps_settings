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
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_NONE;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED;

import static com.android.settings.notification.zen.ZenModeBackend.SOURCE_NONE;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_ANY;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_CONTACTS;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_NONE;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.UNKNOWN;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ZenModePrioritySendersPreferenceControllerTest {
    private ZenModePrioritySendersPreferenceController mMessagesController;
    private ZenModePrioritySendersPreferenceController mCallsController;

    @Mock
    private ZenModeBackend mZenBackend;
    @Mock
    private PreferenceCategory mMockMessagesPrefCategory, mMockCallsPrefCategory;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private NotificationBackend mNotifBackend;
    @Mock
    private ZenPrioritySendersHelper mHelper;

    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mMessagesController = new ZenModePrioritySendersPreferenceController(
                mContext, "test_key_messages", mock(Lifecycle.class), true,
                mNotifBackend);
        ReflectionHelpers.setField(mMessagesController, "mBackend", mZenBackend);
        ReflectionHelpers.setField(mMessagesController, "mHelper", mHelper);

        mCallsController = new ZenModePrioritySendersPreferenceController(
                mContext, "test_key_calls", mock(Lifecycle.class), false,
                mNotifBackend);
        ReflectionHelpers.setField(mCallsController, "mBackend", mZenBackend);
        ReflectionHelpers.setField(mCallsController, "mHelper", mHelper);

        when(mMockMessagesPrefCategory.getContext()).thenReturn(mContext);
        when(mMockCallsPrefCategory.getContext()).thenReturn(mContext);
        when(mPreferenceScreen.findPreference(mMessagesController.getPreferenceKey()))
                .thenReturn(mMockMessagesPrefCategory);
        when(mPreferenceScreen.findPreference(mCallsController.getPreferenceKey()))
                .thenReturn(mMockCallsPrefCategory);
    }

    @Test
    public void displayPreference_delegatesToHelper() {
        mMessagesController.displayPreference(mPreferenceScreen);
        verify(mHelper, times(1)).displayPreference(mMockMessagesPrefCategory);

        mCallsController.displayPreference(mPreferenceScreen);
        verify(mHelper, times(1)).displayPreference(mMockCallsPrefCategory);
    }

    @Test
    public void clickPreference_Messages() {
        // While most of the actual logical functionality for the preference key -> result
        // is/should be controlled by the ZenPrioritySendersHelper, here we need to make sure
        // the returned values from the helper are successfully passed through the click listener.

        // GIVEN current priority message senders are STARRED and conversation senders NONE
        when(mZenBackend.getPriorityMessageSenders()).thenReturn(PRIORITY_SENDERS_STARRED);
        when(mZenBackend.getPriorityConversationSenders()).thenReturn(CONVERSATION_SENDERS_NONE);

        // When we ask mHelper for settings to save on click, it returns ANY for senders and
        // conversations (what it would return if the user clicked "Anyone")
        when(mHelper.settingsToSaveOnClick(
                any(SelectorWithWidgetPreference.class), anyInt(), anyInt()))
                .thenReturn(new int[]{PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_ANYONE});

        // WHEN user clicks the any senders option
        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, true, true);
        anyPref.onClick();

        // THEN any senders gets saved as priority senders for messages
        // and also allow any conversations
        verify(mZenBackend).saveSenders(PRIORITY_CATEGORY_MESSAGES, PRIORITY_SENDERS_ANY);
        verify(mZenBackend).saveConversationSenders(CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void clickPreference_MessagesUnset() {
        // Confirm that when asked to not set something, no ZenModeBackend call occurs.
        // GIVEN current priority message senders are STARRED and conversation senders NONE
        when(mZenBackend.getPriorityMessageSenders()).thenReturn(PRIORITY_SENDERS_STARRED);
        when(mZenBackend.getPriorityConversationSenders()).thenReturn(CONVERSATION_SENDERS_NONE);

        when(mHelper.settingsToSaveOnClick(
                any(SelectorWithWidgetPreference.class), anyInt(), anyInt()))
                .thenReturn(new int[]{SOURCE_NONE, UNKNOWN});

        // WHEN user clicks the starred contacts option
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, true, true);
        nonePref.onClick();

        // THEN "none" gets saved as priority senders for messages
        verify(mZenBackend).saveSenders(PRIORITY_CATEGORY_MESSAGES, SOURCE_NONE);

        // AND that no changes are made to conversation senders
        verify(mZenBackend, never()).saveConversationSenders(anyInt());
    }

    @Test
    public void clickPreference_Calls() {
        // GIVEN current priority call senders are ANY
        when(mZenBackend.getPriorityCallSenders()).thenReturn(PRIORITY_SENDERS_ANY);

        // (and this shouldn't happen, but also be prepared to give an answer if asked for
        // conversation senders)
        when(mZenBackend.getPriorityConversationSenders()).thenReturn(CONVERSATION_SENDERS_ANYONE);

        // Helper returns what would've happened to set priority senders to contacts
        when(mHelper.settingsToSaveOnClick(
                any(SelectorWithWidgetPreference.class), anyInt(), anyInt()))
                .thenReturn(new int[]{PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_NONE});

        // WHEN user clicks the any senders option
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, false, false);
        contactsPref.onClick();

        // THEN contacts gets saved as priority senders for calls
        // and no conversation policies are modified
        verify(mZenBackend).saveSenders(PRIORITY_CATEGORY_CALLS, PRIORITY_SENDERS_CONTACTS);
        verify(mZenBackend, never()).saveConversationSenders(anyInt());
    }

    // Makes a preference with the provided key and whether it's a checkbox with
    // mSelectorClickListener as the onClickListener set.
    private SelectorWithWidgetPreference makePreference(
            String key, boolean isCheckbox, boolean isMessages) {
        final SelectorWithWidgetPreference pref =
                new SelectorWithWidgetPreference(mContext, isCheckbox);
        pref.setKey(key);
        pref.setOnClickListener(
                isMessages ? mMessagesController.mSelectorClickListener
                        : mCallsController.mSelectorClickListener);
        return pref;
    }
}
