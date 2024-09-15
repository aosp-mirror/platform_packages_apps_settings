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
import static android.service.notification.ZenPolicy.STATE_UNSET;

import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_ANY;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_CONTACTS;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_IMPORTANT;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_NONE;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_STARRED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

    private PreferenceCategory mMessagesPrefCategory, mCallsPrefCategory;

    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        mMessagesController = new ZenModePrioritySendersPreferenceController(
                mContext, "messages", true, mBackend);
        mCallsController = new ZenModePrioritySendersPreferenceController(
                mContext, "calls", false, mBackend);
        mMessagesPrefCategory = new PreferenceCategory(mContext);
        mMessagesPrefCategory.setKey(mMessagesController.getPreferenceKey());
        mCallsPrefCategory = new PreferenceCategory(mContext);
        mCallsPrefCategory.setKey(mCallsController.getPreferenceKey());

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mCallsPrefCategory);
        mPreferenceScreen.addPreference(mMessagesPrefCategory);

        Cursor cursor = mock(Cursor.class);
        when(cursor.getCount()).thenReturn(1);
        when(mBackend.queryAllContactsData()).thenReturn(cursor);
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

    @Test
    public void testDisplayPreferences_makeMessagesPrefs() {
        mMessagesController.displayPreference(mPreferenceScreen);

        // Starred contacts, Contacts, Priority Conversations, Any, None
        assertThat(mMessagesPrefCategory.getPreferenceCount()).isEqualTo(5);
    }

    @Test
    public void testDisplayPreferences_makeCallsPrefs() {
        mCallsController.displayPreference(mPreferenceScreen);

        assertThat(mCallsPrefCategory.getPreferenceCount()).isEqualTo(4);
        assertThat((Comparable<?>) mCallsPrefCategory.findPreference(KEY_IMPORTANT)).isNull();

    }

    @Test
    public void testDisplayPreferences_createdOnlyOnce() {
        // Return a nonzero number of child preference when asked.
        // Then when displayPreference is called, it should never make any new preferences.
        mCallsPrefCategory.addPreference(new Preference(mContext));
        mCallsController.displayPreference(mPreferenceScreen);
        mCallsController.displayPreference(mPreferenceScreen);
        mCallsController.displayPreference(mPreferenceScreen);

        // Even though we called display 3 times we shouldn't add more preferences here.
        assertThat(mCallsPrefCategory.getPreferenceCount()).isEqualTo(1);
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
        int[] endState;

        // For KEY_NONE everything should be none.
        endState = mMessagesController.settingsToSaveOnClick(
                KEY_NONE, true, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For KEY_ANY everything should be allowed.
        endState = mMessagesController.settingsToSaveOnClick(
                KEY_ANY, true, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_ANYONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_ANYONE);

        // For [starred] contacts, we should set the priority senders, but not the conversations
        endState =  mMessagesController.settingsToSaveOnClick(
                KEY_STARRED, true, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_STARRED);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mMessagesController.settingsToSaveOnClick(
                KEY_CONTACTS, true, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_CONTACTS);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For priority conversations, we should set the conversations but not priority senders
        endState = mMessagesController.settingsToSaveOnClick(
                KEY_IMPORTANT, true, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void testSettingsToSaveOnClick_messagesUncheck() {
        int[] endState;

        // For KEY_NONE, "unchecking" still means "none".
        endState = mMessagesController.settingsToSaveOnClick(
                KEY_NONE, false, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY unchecking resets the state to "none".
        endState = mMessagesController.settingsToSaveOnClick(
                KEY_ANY, false, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        endState = mMessagesController.settingsToSaveOnClick(
                KEY_STARRED, false, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mMessagesController.settingsToSaveOnClick(
                KEY_CONTACTS, false, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For priority conversations, we should set the conversations but not priority senders
        endState = mMessagesController.settingsToSaveOnClick(
                KEY_IMPORTANT, false, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void testSettingsToSaveOnClick_callsCheck() {
        int[] endState;

        // For calls: we should never set conversations, as this is unrelated to calls.
        // For KEY_NONE senders should be none.
        endState = mCallsController.settingsToSaveOnClick(
                KEY_NONE, true, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY senders should be ANY.
        endState = mCallsController.settingsToSaveOnClick(
                KEY_ANY, true, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_ANYONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For [starred] contacts, we should set the priority senders accordingly
        endState = mCallsController.settingsToSaveOnClick(
                KEY_STARRED, true, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_STARRED);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mCallsController.settingsToSaveOnClick(
                KEY_CONTACTS, true, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_CONTACTS);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);
    }

    @Test
    public void testSettingsToSaveOnClick_callsUncheck() {
        int[] endState;

        // A calls setup should never set conversations settings.
        // For KEY_NONE, "unchecking" still means "none".
        endState = mCallsController.settingsToSaveOnClick(
                KEY_NONE, false, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(endState[0]).isEqualTo(STATE_UNSET);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For KEY_ANY unchecking resets the state to "none".
        endState = mCallsController.settingsToSaveOnClick(
                KEY_ANY, false, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        // For [starred] contacts, we should unset the priority senders, but not the conversations
        endState = mCallsController.settingsToSaveOnClick(
                KEY_STARRED, false, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);

        endState = mCallsController.settingsToSaveOnClick(
                KEY_CONTACTS, false, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(endState[0]).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(endState[1]).isEqualTo(CONVERSATION_SENDERS_UNSET);
    }

    @Test
    public void testSettingsToSave_messages_noChange() {
        int[] savedSettings;

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_NONE, true, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_ANY, true, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_STARRED, true, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_CONTACTS, true, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_IMPORTANT, true, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_IMPORTANT);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);
    }

    @Test
    public void testSettingsToSave_calls_noChange() {
        int[] savedSettings;

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_NONE, true, PEOPLE_TYPE_NONE, CONVERSATION_SENDERS_NONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_ANY, true, PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
        assertThat(savedSettings[1]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_STARRED, true, PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);

        savedSettings = mMessagesController.settingsToSaveOnClick(
                KEY_CONTACTS, true, PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_ANYONE);
        assertThat(savedSettings[0]).isEqualTo(STATE_UNSET);
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_startingUnchecked_messages() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder()
                                .disallowAllSounds()
                                .build())
                        .build(), true);

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        assertThat(((SelectorWithWidgetPreference) mMessagesPrefCategory.findPreference(KEY_NONE))
                .isChecked());

        mMessagesPrefCategory.findPreference(KEY_STARRED).performClick();

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityMessageSenders())
                .isEqualTo(PEOPLE_TYPE_STARRED);
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_startingChecked_messages() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder()
                                .allowAllSounds()
                                .build())
                        .build(), true);

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        assertThat(
                ((SelectorWithWidgetPreference) mMessagesPrefCategory.findPreference(KEY_ANY))
                .isChecked()).isTrue();

        mMessagesPrefCategory.findPreference(KEY_ANY).performClick();

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityMessageSenders())
                .isEqualTo(PEOPLE_TYPE_NONE);
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_startingUnchecked_calls() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder()
                                .disallowAllSounds()
                                .build())
                        .build(), true);

        mCallsController.displayPreference(mPreferenceScreen);
        mCallsController.updateZenMode(mCallsPrefCategory, zenMode);

        assertThat(
                ((SelectorWithWidgetPreference) mCallsPrefCategory.findPreference(KEY_NONE))
                .isChecked()).isTrue();

        mCallsPrefCategory.findPreference(KEY_STARRED).performClick();
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCallSenders())
                .isEqualTo(PEOPLE_TYPE_STARRED);
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_startingChecked_calls() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder()
                                .disallowAllSounds()
                                .build())
                        .build(), true);

        mCallsController.displayPreference(mPreferenceScreen);
        mCallsController.updateZenMode(mCallsPrefCategory, zenMode);

        assertThat(
                ((SelectorWithWidgetPreference) mCallsPrefCategory.findPreference(KEY_NONE))
                .isChecked()).isTrue();

        mCallsPrefCategory.findPreference(KEY_NONE).performClick();
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCallSenders())
                .isEqualTo(PEOPLE_TYPE_NONE);
    }
}