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
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;

import static com.android.settings.notification.zen.ZenModeBackend.SOURCE_NONE;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_ANY;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_CONTACTS;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_IMPORTANT;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_NONE;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.KEY_STARRED;
import static com.android.settings.notification.zen.ZenPrioritySendersHelper.UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;

import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ZenPrioritySendersHelperTest {
    public static final String TAG = "ZenPrioritySendersHelperTest";
    @Mock
    private PreferenceCategory mMockPrefCategory;
    @Mock
    private ZenModeBackend mZenBackend;
    @Mock
    private NotificationBackend mNotifBackend;
    @Mock
    private SelectorWithWidgetPreference.OnClickListener mClickListener;

    private Context mContext;
    @Mock
    private Resources mResources;
    @Mock
    private ContentResolver mContentResolver;

    // This class is simply a wrapper to override getSummary() in order to avoid ZenModeBackend
    // calls.
    private class ZenPrioritySendersHelperWrapper extends ZenPrioritySendersHelper {
        ZenPrioritySendersHelperWrapper(Context context, boolean isMessages,
                ZenModeBackend zenModeBackend,
                NotificationBackend notificationBackend,
                SelectorWithWidgetPreference.OnClickListener clickListener) {
            super(context, isMessages, zenModeBackend, notificationBackend, clickListener);
        }

        @Override
        void updateSummaries() {
            // Do nothing, so we don't try to get summaries from resources.
        }
    }

    // Extension of ArgumentMatcher to check that a preference argument has the correct preference
    // key, but doesn't check any other properties.
    private class PrefKeyMatcher implements ArgumentMatcher<SelectorWithWidgetPreference> {
        private String mKey;
        PrefKeyMatcher(String key) {
            mKey = key;
        }

        public boolean matches(SelectorWithWidgetPreference pref) {
            return pref.getKey() != null && pref.getKey().equals(mKey);
        }

        public String toString() {
            return "SelectorWithWidgetPreference matcher for key " + mKey;
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mContext.getResources()).thenReturn(mResources);
        when(mMockPrefCategory.getContext()).thenReturn(mContext);

        // We don't care about resource contents, just make sure that attempting to access
        // resources doesn't kill the test
        when(mResources.getString(anyInt())).thenReturn("testString");
    }

    private ZenPrioritySendersHelper makeMessagesHelper() {
        return new ZenPrioritySendersHelperWrapper(
                mContext, true, mZenBackend, mNotifBackend, mClickListener);
    }

    private ZenPrioritySendersHelper makeCallsHelper() {
        return new ZenPrioritySendersHelperWrapper(
                mContext, false, mZenBackend, mNotifBackend, mClickListener);
    }

    private SelectorWithWidgetPreference makePreference(String key, boolean isCheckbox) {
        final SelectorWithWidgetPreference pref =
                new SelectorWithWidgetPreference(mContext, isCheckbox);
        pref.setKey(key);
        return pref;
    }

    @Test
    public void testDisplayPreferences_makeMessagesPrefs() {
        ArgumentCaptor<SelectorWithWidgetPreference> prefCaptor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);
        when(mMockPrefCategory.getPreferenceCount()).thenReturn(0);  // not yet created
        ZenPrioritySendersHelper messagesHelper = makeMessagesHelper();
        messagesHelper.displayPreference(mMockPrefCategory);

        // Starred contacts, Contacts, Priority Conversations, Any, None
        verify(mMockPrefCategory, times(5))
                .addPreference(prefCaptor.capture());

        // First verify that the click listener has not been called yet before we start clicking on
        // things.
        verify(mClickListener, never())
                .onRadioButtonClicked(any(SelectorWithWidgetPreference.class));
        for (SelectorWithWidgetPreference pref : prefCaptor.getAllValues()) {
            // Verify that the click listener got a click on something with this pref key.
            pref.onClick();
            verify(mClickListener).onRadioButtonClicked(argThat(new PrefKeyMatcher(pref.getKey())));
        }
    }

    @Test
    public void testDisplayPreferences_makeCallsPrefs() {
        ArgumentCaptor<SelectorWithWidgetPreference> prefCaptor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);
        when(mMockPrefCategory.getPreferenceCount()).thenReturn(0);  // not yet created
        ZenPrioritySendersHelper callsHelper = makeCallsHelper();
        callsHelper.displayPreference(mMockPrefCategory);

        // Starred contacts, Contacts, Any, None
        verify(mMockPrefCategory, times(4))
                .addPreference(prefCaptor.capture());

        // Make sure we never have the conversation one
        verify(mMockPrefCategory, never())
                .addPreference(argThat(new PrefKeyMatcher(KEY_IMPORTANT)));

        verify(mClickListener, never())
                .onRadioButtonClicked(any(SelectorWithWidgetPreference.class));
        for (SelectorWithWidgetPreference pref : prefCaptor.getAllValues()) {
            // Verify that the click listener got a click on something with this pref key.
            pref.onClick();
            verify(mClickListener).onRadioButtonClicked(argThat(new PrefKeyMatcher(pref.getKey())));
        }
    }

    @Test
    public void testDisplayPreferences_createdOnlyOnce() {
        // Return a nonzero number of child preference when asked.
        // Then when displayPreference is called, it should never make any new preferences.
        when(mMockPrefCategory.getPreferenceCount()).thenReturn(4); // already created
        ZenPrioritySendersHelper callsHelper = makeCallsHelper();
        callsHelper.displayPreference(mMockPrefCategory);
        callsHelper.displayPreference(mMockPrefCategory);
        callsHelper.displayPreference(mMockPrefCategory);

        // Even though we called display 3 times we shouldn't add more preferences here.
        verify(mMockPrefCategory, never())
                .addPreference(any(SelectorWithWidgetPreference.class));
    }

    @Test
    public void testKeyToSettingEndState_messagesCheck() {
        ZenPrioritySendersHelper messagesHelper = makeMessagesHelper();
        int[] endState;

        // For KEY_NONE everything should be none.
        endState = messagesHelper.keyToSettingEndState(KEY_NONE, true);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For KEY_ANY everything should be allowed.
        endState = messagesHelper.keyToSettingEndState(KEY_ANY, true);
        assertThat(endState[0]).isEqualTo(PRIORITY_SENDERS_ANY);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_ANYONE);

        // For [starred] contacts, we should set the priority senders, but not the conversations
        endState = messagesHelper.keyToSettingEndState(KEY_STARRED, true);
        assertThat(endState[0]).isEqualTo(PRIORITY_SENDERS_STARRED);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        endState = messagesHelper.keyToSettingEndState(KEY_CONTACTS, true);
        assertThat(endState[0]).isEqualTo(PRIORITY_SENDERS_CONTACTS);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        // For priority conversations, we should set the conversations but not priority senders
        endState = messagesHelper.keyToSettingEndState(KEY_IMPORTANT, true);
        assertThat(endState[0]).isEqualTo(UNKNOWN);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void testKeyToSettingEndState_messagesUncheck() {
        ZenPrioritySendersHelper messagesHelper = makeMessagesHelper();
        int[] endState;

        // For KEY_NONE, "unchecking" still means "none".
        endState = messagesHelper.keyToSettingEndState(KEY_NONE, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For KEY_ANY unchecking resets the state to "none".
        endState = messagesHelper.keyToSettingEndState(KEY_ANY, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        endState = messagesHelper.keyToSettingEndState(KEY_STARRED, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        endState = messagesHelper.keyToSettingEndState(KEY_CONTACTS, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        // For priority conversations, we should set the conversations but not priority senders
        endState = messagesHelper.keyToSettingEndState(KEY_IMPORTANT, false);
        assertThat(endState[0]).isEqualTo(UNKNOWN);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void testKeyToSettingEndState_callsCheck() {
        ZenPrioritySendersHelper callsHelper = makeCallsHelper();
        int[] endState;

        // For all of calls: we should never set conversations, as this is unrelated to calls.
        // For KEY_NONE senders should be none.
        endState = callsHelper.keyToSettingEndState(KEY_NONE, true);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        // For KEY_ANY senders should be ANY.
        endState = callsHelper.keyToSettingEndState(KEY_ANY, true);
        assertThat(endState[0]).isEqualTo(PRIORITY_SENDERS_ANY);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        // For [starred] contacts, we should set the priority senders accordingly
        endState = callsHelper.keyToSettingEndState(KEY_STARRED, true);
        assertThat(endState[0]).isEqualTo(PRIORITY_SENDERS_STARRED);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        endState = callsHelper.keyToSettingEndState(KEY_CONTACTS, true);
        assertThat(endState[0]).isEqualTo(PRIORITY_SENDERS_CONTACTS);
        assertThat(endState[1]).isEqualTo(UNKNOWN);
    }

    @Test
    public void testKeyToSettingEndState_callsUncheck() {
        ZenPrioritySendersHelper callsHelper = makeCallsHelper();
        int[] endState;

        // A calls setup should never set conversations settings.
        // For KEY_NONE, "unchecking" still means "none".
        endState = callsHelper.keyToSettingEndState(KEY_NONE, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        // For KEY_ANY unchecking resets the state to "none".
        endState = callsHelper.keyToSettingEndState(KEY_ANY, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        endState = callsHelper.keyToSettingEndState(KEY_STARRED, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(UNKNOWN);

        endState = callsHelper.keyToSettingEndState(KEY_CONTACTS, false);
        assertThat(endState[0]).isEqualTo(SOURCE_NONE);
        assertThat(endState[1]).isEqualTo(UNKNOWN);
    }

    @Test
    public void testSettingsToSave_messagesNone() {
        // Test coming from the same state (don't newly save redundant settings) and coming from
        // different states (when settings to save should be "none" for both senders and
        // conversations).
        ZenPrioritySendersHelper messagesHelper = makeMessagesHelper();
        int[] savedSettings;

        // None preference; not a checkbox (so whenever we click it, it counts as "checking").
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, false);

        // Current settings already none; expect no settings to need to be saved
        savedSettings = messagesHelper.settingsToSaveOnClick(
                nonePref, SOURCE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        // Current settings are something else; save the "none" settings
        savedSettings = messagesHelper.settingsToSaveOnClick(
                nonePref, PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(SOURCE_NONE);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // One but not the other
        savedSettings = messagesHelper.settingsToSaveOnClick(
                nonePref, SOURCE_NONE, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        savedSettings = messagesHelper.settingsToSaveOnClick(
                nonePref, PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(SOURCE_NONE);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);
    }

    @Test
    public void testSettingsToSave_messagesAny() {
        // Test coming from the same state (don't newly save redundant settings) and coming from
        // different states (when settings to save should be "any" for both senders and
        // conversations).
        ZenPrioritySendersHelper messagesHelper = makeMessagesHelper();
        int[] savedSettings;

        // Any preference; checkbox.
        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, true);

        // Current settings already none; expect no settings to need to be saved
        savedSettings = messagesHelper.settingsToSaveOnClick(
                anyPref, PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        // Current settings are something else; save the "any" settings
        savedSettings = messagesHelper.settingsToSaveOnClick(
                anyPref, PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_ANY);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_ANYONE);

        // It shouldn't be possible to have a starting state of one but not the other, but
        // make sure it works anyway?
        savedSettings = messagesHelper.settingsToSaveOnClick(
                anyPref, PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_ANYONE);

        savedSettings = messagesHelper.settingsToSaveOnClick(
                anyPref, PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_ANY);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        // Test that unchecking the box results in a "none" state
        anyPref.setChecked(true);
        savedSettings = messagesHelper.settingsToSaveOnClick(
                anyPref, PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(SOURCE_NONE);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void testSettingsToSave_messagesContacts() {
        // Test coming from the same state (don't newly save redundant settings) and coming from
        // different states.
        // In addition, saving either starred or contacts has the special case where if we're
        // coming from the "any" state it should also set the conversation senders to none.
        ZenPrioritySendersHelper messagesHelper = makeMessagesHelper();
        int[] savedSettings;

        // Test both contacts-related preferences here.
        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, true);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, true);

        // Current settings already the relevant ones; expect no settings to need to be saved
        // Note that since these are checkboxes, this state shouldn't be reachable, but check it
        // anyway just in case.
        savedSettings = messagesHelper.settingsToSaveOnClick(
                starredPref, PRIORITY_SENDERS_STARRED, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = messagesHelper.settingsToSaveOnClick(
                contactsPref, PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        // Current settings are something else (contacts setting or "none"); save new senders
        // but do not change conversations.
        savedSettings = messagesHelper.settingsToSaveOnClick(
                starredPref, PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_STARRED);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = messagesHelper.settingsToSaveOnClick(
                contactsPref, SOURCE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_CONTACTS);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        // Special additional case: if the settings are currently "any" for both, we additionally
        // reset the conversation settings to none.
        savedSettings = messagesHelper.settingsToSaveOnClick(
                starredPref, PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_STARRED);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        savedSettings = messagesHelper.settingsToSaveOnClick(
                contactsPref, PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_CONTACTS);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // Test that un-checking works as well.
        starredPref.setChecked(true);
        contactsPref.setChecked(true);

        // Make sure we don't overwrite existing conversation senders setting when unchecking
        savedSettings = messagesHelper.settingsToSaveOnClick(
                starredPref, PRIORITY_SENDERS_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(SOURCE_NONE);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = messagesHelper.settingsToSaveOnClick(
                contactsPref, PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(SOURCE_NONE);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);
    }

    @Test
    public void testSettingsToSave_messagesConversations() {
        // Test coming from the same state (don't newly save redundant settings) and coming from
        // different states.
        // In addition, saving either starred or contacts has the special case where if we're
        // coming from the "any" state it should also set the conversation senders to none.
        ZenPrioritySendersHelper messagesHelper = makeMessagesHelper();
        int[] savedSettings;

        SelectorWithWidgetPreference convsPref = makePreference(KEY_IMPORTANT, true);

        // Current settings already the relevant ones; expect no settings to need to be saved
        // Note that since these are checkboxes, this state shouldn't be reachable, but check it
        // anyway just in case.
        savedSettings = messagesHelper.settingsToSaveOnClick(
                convsPref, PRIORITY_SENDERS_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        // Current settings are something else (only actual choice here is "none"); save
        // new conversations but do not change senders.
        savedSettings = messagesHelper.settingsToSaveOnClick(
                convsPref, PRIORITY_SENDERS_CONTACTS, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_IMPORTANT);

        // Special additional case: if the settings are currently "any" for both, we additionally
        // reset the senders settings to none.
        savedSettings = messagesHelper.settingsToSaveOnClick(
                convsPref, PRIORITY_SENDERS_ANY, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(SOURCE_NONE);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_IMPORTANT);

        // Test that un-checking works as well.
        convsPref.setChecked(true);

        // Make sure we don't overwrite existing conversation senders setting when unchecking
        savedSettings = messagesHelper.settingsToSaveOnClick(
                convsPref, PRIORITY_SENDERS_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void testSettingsToSave_calls() {
        // Simpler test for calls: for each one, test that the relevant ones are saved if not
        // already set, and that conversation settings are never changed.
        ZenPrioritySendersHelper callsHelper = makeCallsHelper();
        int[] savedSettings;

        // None of the preferences are checkboxes.
        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, false);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, false);
        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, false);
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, false);

        // Test that if the settings are already what is set, nothing happens.
        savedSettings = callsHelper.settingsToSaveOnClick(
                starredPref, PRIORITY_SENDERS_STARRED, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = callsHelper.settingsToSaveOnClick(
                contactsPref, PRIORITY_SENDERS_CONTACTS, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = callsHelper.settingsToSaveOnClick(anyPref, PRIORITY_SENDERS_ANY, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = callsHelper.settingsToSaveOnClick(nonePref, SOURCE_NONE, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(UNKNOWN);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        // Test that if the settings are something different, the relevant thing gets saved.
        savedSettings = callsHelper.settingsToSaveOnClick(
                starredPref, PRIORITY_SENDERS_CONTACTS, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_STARRED);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = callsHelper.settingsToSaveOnClick(
                contactsPref, PRIORITY_SENDERS_ANY, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_CONTACTS);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = callsHelper.settingsToSaveOnClick(anyPref, SOURCE_NONE, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(PRIORITY_SENDERS_ANY);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);

        savedSettings = callsHelper.settingsToSaveOnClick(
                nonePref, PRIORITY_SENDERS_STARRED, UNKNOWN);
        assertThat(savedSettings[0]).isEqualTo(SOURCE_NONE);
        assertThat(savedSettings[1]).isEqualTo(UNKNOWN);
    }
}
