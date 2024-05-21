/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_UNSET;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_STARRED;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_UNSET;
import static android.service.notification.ZenPolicy.STATE_ALLOW;
import static android.service.notification.ZenPolicy.STATE_DISALLOW;
import static android.service.notification.ZenPolicy.STATE_UNSET;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_ANY;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_CONTACTS;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_IMPORTANT;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_NONE;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_STARRED;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModePrioritySendersPreferenceControllerTest {

    private ZenModePrioritySendersPreferenceController mCallsController;
    private ZenModePrioritySendersPreferenceController mMessagesController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    @Mock
    private ZenModesBackend mBackend;

    @Mock
    private PreferenceCategory mMockMessagesPrefCategory, mMockCallsPrefCategory;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        mMessagesController = new ZenModePrioritySendersPreferenceController(
                mContext, "messages", true, mBackend);
        mCallsController = new ZenModePrioritySendersPreferenceController(
                mContext, "calls", false, mBackend);
        when(mMockMessagesPrefCategory.getContext()).thenReturn(mContext);
        when(mMockCallsPrefCategory.getContext()).thenReturn(mContext);
        when(mPreferenceScreen.findPreference(mMessagesController.getPreferenceKey()))
                .thenReturn(mMockMessagesPrefCategory);
        when(mPreferenceScreen.findPreference(mCallsController.getPreferenceKey()))
                .thenReturn(mMockCallsPrefCategory);
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

    @Test
    public void testDisplayPreferences_makeMessagesPrefs() {
        ArgumentCaptor<SelectorWithWidgetPreference> prefCaptor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);
        when(mMockMessagesPrefCategory.getPreferenceCount()).thenReturn(0);  // not yet created
        mMessagesController.displayPreference(mPreferenceScreen);

        // Starred contacts, Contacts, Priority Conversations, Any, None
        verify(mMockMessagesPrefCategory, times(5)).addPreference(prefCaptor.capture());
    }

    @Test
    public void testDisplayPreferences_makeCallsPrefs() {
        ArgumentCaptor<SelectorWithWidgetPreference> prefCaptor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);
        when(mMockCallsPrefCategory.getPreferenceCount()).thenReturn(0);  // not yet created
        mCallsController.displayPreference(mPreferenceScreen);

        // Starred contacts, Contacts, Any, None
        verify(mMockCallsPrefCategory, times(4)).addPreference(prefCaptor.capture());

        // Make sure we never have the conversation one
        verify(mMockCallsPrefCategory, never())
                .addPreference(argThat(new PrefKeyMatcher(KEY_IMPORTANT)));
    }

    @Test
    public void testDisplayPreferences_createdOnlyOnce() {
        // Return a nonzero number of child preference when asked.
        // Then when displayPreference is called, it should never make any new preferences.
        when(mMockCallsPrefCategory.getPreferenceCount()).thenReturn(4); // already created
        mCallsController.displayPreference(mPreferenceScreen);
        mCallsController.displayPreference(mPreferenceScreen);
        mCallsController.displayPreference(mPreferenceScreen);

        // Even though we called display 3 times we shouldn't add more preferences here.
        verify(mMockCallsPrefCategory, never())
                .addPreference(any(SelectorWithWidgetPreference.class));
    }

    @Test
    public void testKeyToSettingEndState_messagesCheck() {
        int[] endState;

        // For KEY_NONE everything should be none.
        endState = mMessagesController.keyToSettingEndState(KEY_NONE, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For KEY_ANY everything should be allowed.
        endState = mMessagesController.keyToSettingEndState(KEY_ANY, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_ANYONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_ANYONE);

        // For [starred] contacts, we should set the priority senders, but not the conversations
        endState = mMessagesController.keyToSettingEndState(KEY_STARRED, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_STARRED);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mMessagesController.keyToSettingEndState(KEY_CONTACTS, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_CONTACTS);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For priority conversations, we should set the conversations but not priority senders
        endState = mMessagesController.keyToSettingEndState(KEY_IMPORTANT, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void testKeyToSettingEndState_messagesUncheck() {
        int[] endState;

        // For KEY_NONE, "unchecking" still means "none".
        endState = mMessagesController.keyToSettingEndState(KEY_NONE, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For KEY_ANY unchecking resets the state to "none".
        endState = mMessagesController.keyToSettingEndState(KEY_ANY, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        endState = mMessagesController.keyToSettingEndState(KEY_STARRED, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mMessagesController.keyToSettingEndState(KEY_CONTACTS, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For priority conversations, we should set the conversations but not priority senders
        endState = mMessagesController.keyToSettingEndState(KEY_IMPORTANT, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void testKeyToSettingEndState_callsCheck() {
        int[] endState;

        // For calls: we should never set conversations, as this is unrelated to calls.
        // For KEY_NONE senders should be none.
        endState = mCallsController.keyToSettingEndState(KEY_NONE, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY senders should be ANY.
        endState = mCallsController.keyToSettingEndState(KEY_ANY, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_ANYONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For [starred] contacts, we should set the priority senders accordingly
        endState = mCallsController.keyToSettingEndState(KEY_STARRED, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_STARRED);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mCallsController.keyToSettingEndState(KEY_CONTACTS, true);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_CONTACTS);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);
    }

    @Test
    public void testKeyToSettingEndState_callsUncheck() {
        int[] endState;

        // A calls setup should never set conversations settings.
        // For KEY_NONE, "unchecking" still means "none".
        endState = mCallsController.keyToSettingEndState(KEY_NONE, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY unchecking resets the state to "none".
        endState = mCallsController.keyToSettingEndState(KEY_ANY, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        endState = mCallsController.keyToSettingEndState(KEY_STARRED, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mCallsController.keyToSettingEndState(KEY_CONTACTS, false);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);
    }

    @Test
    public void testSettingsToSaveOnClick_messagesCheck() {
        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, true, true);
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, true, true);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, true, true);
        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, true, true);
        SelectorWithWidgetPreference impPref = makePreference(KEY_IMPORTANT, true, true);
        int[] endState;

        // For KEY_NONE everything should be none.
        nonePref.setChecked(true);
        endState = mMessagesController.settingsToSaveOnClick(
                nonePref, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For KEY_ANY everything should be allowed.
        anyPref.setChecked(true);
        endState = mMessagesController.settingsToSaveOnClick(
                anyPref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_ANYONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_ANYONE);

        // For [starred] contacts, we should set the priority senders, but not the conversations
        starredPref.setChecked(true);
        endState =  mMessagesController.settingsToSaveOnClick(
                starredPref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_STARRED);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        contactsPref.setChecked(true);
        endState = mMessagesController.settingsToSaveOnClick(
                contactsPref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_CONTACTS);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For priority conversations, we should set the conversations but not priority senders
        impPref.setChecked(true);
        endState = mMessagesController.settingsToSaveOnClick(
                impPref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void testSettingsToSaveOnClick_messagesUncheck() {
        int[] endState;

        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, true, true);
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, true, true);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, true, true);
        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, true, true);
        SelectorWithWidgetPreference impPref = makePreference(KEY_IMPORTANT, true, true);

        // For KEY_NONE, "unchecking" still means "none".
        nonePref.setChecked(false);
        endState = mMessagesController.settingsToSaveOnClick(
                nonePref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY unchecking resets the state to "none".
        anyPref.setChecked(false);
        endState = mMessagesController.settingsToSaveOnClick(
                anyPref, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        starredPref.setChecked(false);
        endState = mMessagesController.settingsToSaveOnClick(
                starredPref, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        contactsPref.setChecked(false);
        endState = mMessagesController.settingsToSaveOnClick(
                contactsPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For priority conversations, we should set the conversations but not priority senders
        impPref.setChecked(false);
        endState = mMessagesController.settingsToSaveOnClick(
                impPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void testSettingsToSaveOnClick_callsCheck() {
        int[] endState;
        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, true, true);
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, true, true);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, true, true);
        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, true, true);

        // For calls: we should never set conversations, as this is unrelated to calls.
        // For KEY_NONE senders should be none.
        nonePref.setChecked(true);
        endState = mCallsController.settingsToSaveOnClick(
                nonePref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY senders should be ANY.
        anyPref.setChecked(true);
        endState = mCallsController.settingsToSaveOnClick(
                anyPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_ANYONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For [starred] contacts, we should set the priority senders accordingly
        starredPref.setChecked(true);
        endState = mCallsController.settingsToSaveOnClick(
                starredPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_STARRED);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        contactsPref.setChecked(true);
        endState = mCallsController.settingsToSaveOnClick(
                contactsPref, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_CONTACTS);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);
    }

    @Test
    public void testSettingsToSaveOnClick_callsUncheck() {
        int[] endState;
        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, true, true);
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, true, true);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, true, true);
        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, true, true);

        // A calls setup should never set conversations settings.
        // For KEY_NONE, "unchecking" still means "none".
        nonePref.setChecked(false);
        endState = mCallsController.settingsToSaveOnClick(
                nonePref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(STATE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY unchecking resets the state to "none".
        anyPref.setChecked(false);
        endState = mCallsController.settingsToSaveOnClick(
                anyPref, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        starredPref.setChecked(false);
        endState = mCallsController.settingsToSaveOnClick(
                starredPref, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        contactsPref.setChecked(false);
        endState = mCallsController.settingsToSaveOnClick(
                contactsPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);
    }

    @Test
    public void testSettingsToSave_messages_noChange() {
        int[] savedSettings;

        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, true, true);
        nonePref.setChecked(true);
        savedSettings = mMessagesController.settingsToSaveOnClick(
                nonePref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, true, true);
        anyPref.setChecked(true);
        savedSettings = mMessagesController.settingsToSaveOnClick(
                anyPref, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, true, true);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, true, true);
        starredPref.setChecked(true);
        savedSettings = mMessagesController.settingsToSaveOnClick(
                starredPref, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);

        contactsPref.setChecked(true);
        savedSettings = mMessagesController.settingsToSaveOnClick(
                contactsPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);

        SelectorWithWidgetPreference impPref = makePreference(KEY_IMPORTANT, true, true);
        impPref.setChecked(true);
        savedSettings = mMessagesController.settingsToSaveOnClick(
                impPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);
    }

    @Test
    public void testSettingsToSave_calls_noChange() {
        int[] savedSettings;
        SelectorWithWidgetPreference nonePref = makePreference(KEY_NONE, false, false);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                nonePref, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        SelectorWithWidgetPreference anyPref = makePreference(KEY_ANY, false, false);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                anyPref, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        SelectorWithWidgetPreference starredPref = makePreference(KEY_STARRED, false, false);
        SelectorWithWidgetPreference contactsPref = makePreference(KEY_CONTACTS, false, false);
        savedSettings = mMessagesController.settingsToSaveOnClick(
                starredPref, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                contactsPref, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
    }
}