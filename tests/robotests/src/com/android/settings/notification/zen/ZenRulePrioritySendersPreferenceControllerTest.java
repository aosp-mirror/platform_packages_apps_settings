/*
 * Copyright (C) 2021 The Android Open Source Project
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
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED;

import static com.android.settings.notification.zen.ZenModeBackend.SOURCE_NONE;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_ANY;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_CONTACTS;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_NONE;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.ZenPolicy;

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
public class ZenRulePrioritySendersPreferenceControllerTest {
    private ZenRulePrioritySendersPreferenceController mMessagesController;
    private ZenRulePrioritySendersPreferenceController mCallsController;

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
    private final String mId = "test_zen_rule_id";
    private AutomaticZenRule mRule;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mRule = new AutomaticZenRule("test", null, null, null, null,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
        mMessagesController = new ZenRulePrioritySendersPreferenceController(
                mContext, "test_key_messages", mock(Lifecycle.class), true,
                mNotifBackend);
        ReflectionHelpers.setField(mMessagesController, "mBackend", mZenBackend);
        ReflectionHelpers.setField(mMessagesController, "mHelper", mHelper);
        ReflectionHelpers.setField(mMessagesController, "mRule", mRule);
        ReflectionHelpers.setField(mMessagesController, "mId", mId);

        mCallsController = new ZenRulePrioritySendersPreferenceController(
                mContext, "test_key_calls", mock(Lifecycle.class), false,
                mNotifBackend);
        ReflectionHelpers.setField(mCallsController, "mBackend", mZenBackend);
        ReflectionHelpers.setField(mCallsController, "mHelper", mHelper);
        ReflectionHelpers.setField(mCallsController, "mRule", mRule);
        ReflectionHelpers.setField(mCallsController, "mId", mId);

        when(mMockMessagesPrefCategory.getContext()).thenReturn(mContext);
        when(mMockCallsPrefCategory.getContext()).thenReturn(mContext);
        when(mPreferenceScreen.findPreference(mMessagesController.getPreferenceKey()))
                .thenReturn(mMockMessagesPrefCategory);
        when(mPreferenceScreen.findPreference(mCallsController.getPreferenceKey()))
                .thenReturn(mMockCallsPrefCategory);
        when(mZenBackend.getAutomaticZenRule(mId)).thenReturn(mRule);
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
        // the returned values from the helper are correctly saved to the zen policy in mRule.

        // GIVEN current priority message senders are STARRED and conversation senders NONE
        setMessageSenders(PRIORITY_SENDERS_STARRED);
        setConversationSenders(CONVERSATION_SENDERS_NONE);

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
        assertThat(getMessageSenders()).isEqualTo(PRIORITY_SENDERS_ANY);
        assertThat(getConversationSenders()).isEqualTo(CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void clickPreference_MessagesUnset() {
        // Confirm that when asked to not set something, no change occurs.
        // GIVEN current priority message senders are STARRED and conversation senders NONE
        setMessageSenders(PRIORITY_SENDERS_STARRED);
        setConversationSenders(CONVERSATION_SENDERS_NONE);

        when(mHelper.settingsToSaveOnClick(
                any(SelectorWithWidgetPreference.class), anyInt(), anyInt()))
                .thenReturn(new int[]{SOURCE_NONE, UNKNOWN});

        // WHEN user clicks the starred contacts option
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, true, true);
        nonePref.onClick();

        // THEN priority senders for messages is set to NONE
        assertThat(getMessageSenders()).isEqualTo(SOURCE_NONE);

        // AND that conversation senders remains unchanged
        assertThat(getConversationSenders()).isEqualTo(CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void clickPreference_Calls() {
        // GIVEN current priority call senders are ANY
        setCallSenders(PRIORITY_SENDERS_ANY);

        // Helper returns what would've happened to set priority senders to contacts
        when(mHelper.settingsToSaveOnClick(
                any(SelectorWithWidgetPreference.class), anyInt(), anyInt()))
                .thenReturn(new int[]{PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_NONE});

        // WHEN user clicks the any senders option
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, false, false);
        contactsPref.onClick();

        // THEN contacts gets saved as priority senders for calls
        assertThat(getCallSenders()).isEqualTo(PRIORITY_SENDERS_CONTACTS);
    }

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

    // Helper methods for setting up and reading current state on mRule. These are mostly helpful
    // just to handle translating between the enums used in ZenPolicy from the ones used in
    // the settings for message/call senders.
    private void setMessageSenders(int messageSenders) {
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .allowMessages(
                        ZenRulePrioritySendersPreferenceController.zenPolicySettingFromSender(
                                messageSenders))
                .build());
    }

    private int getMessageSenders() {
        return ZenModeBackend.getContactSettingFromZenPolicySetting(
                mRule.getZenPolicy().getPriorityMessageSenders());
    }

    private void setCallSenders(int callSenders) {
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .allowCalls(
                        ZenRulePrioritySendersPreferenceController.zenPolicySettingFromSender(
                                callSenders))
                .build());
    }

    private int getCallSenders() {
        return ZenModeBackend.getContactSettingFromZenPolicySetting(
                mRule.getZenPolicy().getPriorityCallSenders());
    }

    // There's no enum conversion on the conversation senders, as they use the same enum, but
    // these methods provide some convenient parallel usage compared to the others.
    private void setConversationSenders(int conversationSenders) {
        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .allowConversations(conversationSenders)
                .build());
    }

    private int getConversationSenders() {
        return mRule.getZenPolicy().getPriorityConversationSenders();
    }
}
