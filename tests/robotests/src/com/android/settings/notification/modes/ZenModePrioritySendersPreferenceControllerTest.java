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

import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_STARRED;
import static android.service.notification.ZenPolicy.conversationTypeToString;
import static android.service.notification.ZenPolicy.peopleTypeToString;

import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_ANY;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_ANY_CONVERSATIONS;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_CONTACTS;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_IMPORTANT_CONVERSATIONS;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_NONE;
import static com.android.settings.notification.modes.ZenModePrioritySendersPreferenceController.KEY_STARRED;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;
import android.service.notification.ZenPolicy.ConversationSenders;
import android.service.notification.ZenPolicy.PeopleType;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.function.Consumer;
import java.util.function.Predicate;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModePrioritySendersPreferenceControllerTest {

    private ZenModePrioritySendersPreferenceController mCallsController;
    private ZenModePrioritySendersPreferenceController mMessagesController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    @Mock private ZenModesBackend mBackend;
    @Mock private ZenHelperBackend mHelperBackend;

    private PreferenceCategory mMessagesPrefCategory, mCallsPrefCategory;

    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        mMessagesController = new ZenModePrioritySendersPreferenceController(mContext, "messages",
                true, mBackend, mHelperBackend);
        mCallsController = new ZenModePrioritySendersPreferenceController(mContext, "calls", false,
                mBackend, mHelperBackend);

        mMessagesPrefCategory = new PreferenceCategory(mContext);
        mMessagesPrefCategory.setKey(mMessagesController.getPreferenceKey());
        mCallsPrefCategory = new PreferenceCategory(mContext);
        mCallsPrefCategory.setKey(mCallsController.getPreferenceKey());

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mCallsPrefCategory);
        mPreferenceScreen.addPreference(mMessagesPrefCategory);

        when(mHelperBackend.getStarredContacts()).thenReturn(ImmutableList.of());
        when(mHelperBackend.getAllContacts()).thenReturn(
                ImmutableList.of(new ZenHelperBackend.Contact(1, "The only contact", null)));
        when(mHelperBackend.getAllContactsCount()).thenReturn(1);

        when(mHelperBackend.getImportantConversations()).thenReturn(ImmutableList.of());
        when(mHelperBackend.getAllConversations()).thenReturn(ImmutableList.of());
    }

    @Test
    public void testDisplayPreferences_makeMessagesPrefs() {
        mMessagesController.displayPreference(mPreferenceScreen);

        // "Any Conversations" is invisible by default.
        assertThat(getAllOptions(mMessagesPrefCategory)).containsExactly(KEY_STARRED, KEY_CONTACTS,
                KEY_ANY_CONVERSATIONS, KEY_IMPORTANT_CONVERSATIONS, KEY_ANY, KEY_NONE).inOrder();
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(KEY_STARRED,
                KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_ANY, KEY_NONE).inOrder();
    }

    @Test
    public void testDisplayPreferences_makeCallsPrefs() {
        mCallsController.displayPreference(mPreferenceScreen);

        assertThat(getAllOptions(mCallsPrefCategory)).containsExactly(KEY_STARRED, KEY_CONTACTS,
                KEY_ANY, KEY_NONE).inOrder();
        assertThat(getVisibleOptions(mCallsPrefCategory)).containsExactly(KEY_STARRED, KEY_CONTACTS,
                KEY_ANY, KEY_NONE).inOrder();
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
    public void updateState_callsAny() {
        ZenMode zenMode = newModeWithPolicy(p -> p.allowCalls(PEOPLE_TYPE_ANYONE));
        mCallsController.displayPreference(mPreferenceScreen);

        mCallsController.updateZenMode(mCallsPrefCategory, zenMode);

        assertThat(getCheckedOptions(mCallsPrefCategory)).containsExactly(KEY_ANY);
    }

    @Test
    public void updateState_callsContacts() {
        ZenMode zenMode = newModeWithPolicy(p -> p.allowCalls(PEOPLE_TYPE_CONTACTS));
        mCallsController.displayPreference(mPreferenceScreen);

        mCallsController.updateZenMode(mCallsPrefCategory, zenMode);

        assertThat(getCheckedOptions(mCallsPrefCategory)).containsExactly(KEY_CONTACTS);
    }

    @Test
    public void updateState_callsStarredContacts() {
        ZenMode zenMode = newModeWithPolicy(p -> p.allowCalls(PEOPLE_TYPE_STARRED));
        mCallsController.displayPreference(mPreferenceScreen);

        mCallsController.updateZenMode(mCallsPrefCategory, zenMode);

        assertThat(getCheckedOptions(mCallsPrefCategory)).containsExactly(KEY_STARRED);
    }

    @Test
    public void updateState_callsNone() {
        ZenMode zenMode = newModeWithPolicy(p -> p.allowCalls(PEOPLE_TYPE_NONE));
        mCallsController.displayPreference(mPreferenceScreen);

        mCallsController.updateZenMode(mCallsPrefCategory, zenMode);

        assertThat(getCheckedOptions(mCallsPrefCategory)).containsExactly(KEY_NONE);
    }

    @Test
    public void updateState_msgAnyConvAny() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Messages=ANY shows ANY checked, regardless of conversations value (all conversations are
        // messages and will get through).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_ANY);
    }

    @Test
    public void updateState_msgAnyConvImportant() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Messages=ANY shows ANY checked, regardless of conversations value (all conversations are
        // messages and will get through).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_ANY);
    }

    @Test
    public void updateState_msgAnyConvNone() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Messages=ANY shows ANY checked, regardless of conversations value (all conversations are
        // messages and will get through).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_ANY);
    }

    @Test
    public void updateState_msgContactsConvAny() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_CONTACTS)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Shows a checked option for conversations=ANY which is normally unavailable.
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(KEY_ANY,
                KEY_STARRED, KEY_CONTACTS, KEY_ANY_CONVERSATIONS, KEY_IMPORTANT_CONVERSATIONS,
                KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_CONTACTS,
                KEY_ANY_CONVERSATIONS);
    }

    @Test
    public void updateState_msgContactsConvImportant() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_CONTACTS)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Contacts and important conversations.
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_CONTACTS,
                KEY_IMPORTANT_CONVERSATIONS);
    }

    @Test
    public void updateState_msgContactsConvNone() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_CONTACTS)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Just contacts (will include conversations with those contacts but not explicitly chosen).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_CONTACTS);
    }

    @Test
    public void updateState_msgStarredConvAny() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_STARRED)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Shows a checked option for conversations=ANY which is normally unavailable.
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(KEY_ANY,
                KEY_STARRED, KEY_CONTACTS, KEY_ANY_CONVERSATIONS, KEY_IMPORTANT_CONVERSATIONS,
                KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_STARRED,
                KEY_ANY_CONVERSATIONS);
    }

    @Test
    public void updateState_msgStarredConvImportant() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_STARRED)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Starred contacts and important conversations.
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_STARRED,
                KEY_IMPORTANT_CONVERSATIONS);
    }

    @Test
    public void updateState_msgStarredConvNone() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_STARRED)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Just starred contacts (will include conversations with those contacts but not
        // explicitly chosen).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_STARRED);
    }

    @Test
    public void updateState_msgNoneConvAny() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Shows a checked option for conversations=ANY which is normally unavailable.
        // Only option checked (messages=NONE is reserved for none at all).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(KEY_ANY,
                KEY_STARRED, KEY_CONTACTS, KEY_ANY_CONVERSATIONS, KEY_IMPORTANT_CONVERSATIONS,
                KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY_CONVERSATIONS);
    }

    @Test
    public void updateState_msgNoneConvImportant() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Only important conversations (messages=NONE is reserved for none at all).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(
                KEY_IMPORTANT_CONVERSATIONS);
    }

    @Test
    public void updateState_msgNoneConvNone() {
        ZenMode zenMode = newModeWithPolicy(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        // Just starred contacts (will include conversations with those contacts but not
        // explicitly chosen).
        assertThat(getVisibleOptions(mMessagesPrefCategory)).containsExactly(
                KEY_ANY, KEY_STARRED, KEY_CONTACTS, KEY_IMPORTANT_CONVERSATIONS, KEY_NONE);
        assertThat(getCheckedOptions(mMessagesPrefCategory)).containsExactly(KEY_NONE);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=Any, Conv=Any

    @Test
    public void checkContacts_fromMsgAnyConvAny_toMsgContactsConvNone() {
        checkSomeContacts_fromMsgAnyConvAny_toMsgSomeContactsConvNone(KEY_CONTACTS,
                PEOPLE_TYPE_CONTACTS);
    }

    @Test
    public void checkStarred_fromMsgAnyConvAny_toMsgStarredConvNone() {
        checkSomeContacts_fromMsgAnyConvAny_toMsgSomeContactsConvNone(KEY_STARRED,
                PEOPLE_TYPE_STARRED);
    }

    private void checkSomeContacts_fromMsgAnyConvAny_toMsgSomeContactsConvNone(
            String contactsOptionKey, @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Choosing CONTACTS/STARRED will also internally switch conversations to NONE (which is
        // fine because the user didn't see the old conv=Any, just messages=Anyone).
        setMessagesOptionChecked(contactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkImportantConv_fromMsgAnyConvAny_toMsgNoneConvImportant() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkAnyConv_fromMsgAnyConvAny_toMsgNoneConvAny() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Normally this option won't be visible, but it could be if the page was launched with
        // conv=Any previously.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void uncheckAnyone_fromMsgAnyConvAny_toMsgNoneConvNone() {
        uncheckAnyone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_ANYONE,
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkNone_fromMsgAnyConvAny_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_ANYONE, CONVERSATION_SENDERS_ANYONE);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=Any, Conv=Important

    @Test
    public void checkContacts_fromMsgAnyConvImportant_toMsgContactsConvNone() {
        checkSomeContacts_fromMsgAnyConvImportant_toMsgSomeContactsConvNone(KEY_CONTACTS,
                PEOPLE_TYPE_CONTACTS);
    }

    @Test
    public void checkStarred_fromMsgAnyConvImportant_toMsgStarredConvNone() {
        checkSomeContacts_fromMsgAnyConvImportant_toMsgSomeContactsConvNone(KEY_STARRED,
                PEOPLE_TYPE_STARRED);
    }

    private void checkSomeContacts_fromMsgAnyConvImportant_toMsgSomeContactsConvNone(
            String contactsOptionKey, @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Choosing CONTACTS/STARRED will also internally switch conversations to NONE (which is
        // fine because the user didn't see the old setting, just messages=Anyone).
        setMessagesOptionChecked(contactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkImportantConv_fromMsgAnyConvImportant_toMsgNoneConvImportant() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Although conv=IMPORTANT previously, we show it as Anyone, so selecting important
        // conversations should switch to important conversations only.
        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkAnyConv_fromMsgAnyConvImportant_toMsgNoneConvAny() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Normally this option won't be visible, but it could be if the page was launched with
        // conv=Any previously.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void uncheckAnyone_fromMsgAnyConvImportant_toMsgNoneConvNone() {
        uncheckAnyone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_ANYONE,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkNone_fromMsgAnyConvImportant_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_ANYONE,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=Any, Conv=None

    @Test
    public void checkContacts_fromMsgAnyConvNone_toMsgContactsConvNone() {
        checkSomeContacts_fromMsgAnyConvNone_toMsgSomeContactsConvNone(KEY_CONTACTS,
                PEOPLE_TYPE_CONTACTS);
    }

    @Test
    public void checkStarred_fromMsgAnyConvNone_toMsgStarredConvNone() {
        checkSomeContacts_fromMsgAnyConvNone_toMsgSomeContactsConvNone(KEY_STARRED,
                PEOPLE_TYPE_STARRED);
    }

    private void checkSomeContacts_fromMsgAnyConvNone_toMsgSomeContactsConvNone(
            String contactsOptionKey, @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        // Choosing CONTACTS/STARRED will also internally switch conversations to NONE (which is
        // fine because the user didn't see the old setting, just messages=Anyone).
        setMessagesOptionChecked(contactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkImportantConv_fromMsgAnyConvNone_toMsgNoneConvImportant() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkAnyConv_fromMsgAnyConvNone_toMsgNoneConvAny() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_ANYONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        // Normally this option won't be visible, but it could be if the page was launched with
        // conv=Any previously.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void uncheckAnyone_fromMsgAnyConvNone_toMsgNoneConvNone() {
        uncheckAnyone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_ANYONE,
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkNone_fromMsgAnyConvNone_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_ANYONE,
                CONVERSATION_SENDERS_NONE);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=Contacts OR Starred, Conv=Any

    @Test
    public void switchContacts_fromMsgStarredConvAny_toMsgContactsConvAny() {
        switchContacts_fromMsgSomeContactsConvAny_toMsgOtherContactsConvAny(PEOPLE_TYPE_STARRED,
                KEY_CONTACTS, PEOPLE_TYPE_CONTACTS);
    }

    @Test
    public void switchContacts_fromMsgContactsConvAny_toMsgStarredConvAny() {
        switchContacts_fromMsgSomeContactsConvAny_toMsgOtherContactsConvAny(PEOPLE_TYPE_CONTACTS,
                KEY_STARRED, PEOPLE_TYPE_STARRED);
    }

    private void switchContacts_fromMsgSomeContactsConvAny_toMsgOtherContactsConvAny(
            @PeopleType int fromMessageSenders, String checkingContactsOptionKey,
            @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Switching CONTACTS/STARRED or vice-versa will leave conversations untouched.
        setMessagesOptionChecked(checkingContactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void uncheckStarred_fromMsgStarredConvAny_toMsgNoneConvAny() {
        uncheckSomeContacts_fromMsgSomeContactsConvAny_toMsgNoneConvAny(PEOPLE_TYPE_STARRED,
                KEY_STARRED);
    }

    @Test
    public void uncheckContacts_fromMsgContactsConvAny_toMsgNoneConvAny() {
        uncheckSomeContacts_fromMsgSomeContactsConvAny_toMsgNoneConvAny(
                PEOPLE_TYPE_CONTACTS, KEY_CONTACTS);
    }

    private void uncheckSomeContacts_fromMsgSomeContactsConvAny_toMsgNoneConvAny(
            @PeopleType int fromMessageSenders, String checkingContactsOptionKey) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Unchecking CONTACTS or STARRED will leave conversations untouched.
        setMessagesOptionChecked(checkingContactsOptionKey, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkImportantConv_fromMsgStarredConvAny_toMsgStarredConvImportant() {
        checkImportantConv_fromMsgSomeContactsConvAny_toMsgSomeContactsConvImportant(
                PEOPLE_TYPE_STARRED);
    }

    @Test
    public void checkImportantConv_fromMsgContactsConvAny_toMsgContactsConvImportant() {
        checkImportantConv_fromMsgSomeContactsConvAny_toMsgSomeContactsConvImportant(
                PEOPLE_TYPE_CONTACTS);
    }

    private void checkImportantConv_fromMsgSomeContactsConvAny_toMsgSomeContactsConvImportant(
            @PeopleType int fromMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Choosing important conversations leaves contacts untouched.
        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(fromMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void uncheckAnyConv_fromMsgStarredConvAny_toMsgStarredConvNone() {
        uncheckAnyConv_fromMsgSomeContactsConvAny_toMsgSomeContactsConvNone(
                PEOPLE_TYPE_STARRED);
    }

    @Test
    public void uncheckAnyConv_fromMsgContactsConvAny_toMsgContactsConvNone() {
        uncheckAnyConv_fromMsgSomeContactsConvAny_toMsgSomeContactsConvNone(
                PEOPLE_TYPE_CONTACTS);
    }

    private void uncheckAnyConv_fromMsgSomeContactsConvAny_toMsgSomeContactsConvNone(
            @PeopleType int fromMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Unmarking any conversation leaves contacts untouched.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(fromMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkAnyone_fromMsgStarredConvAny_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_STARRED,
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkAnyone_fromMsgContactsConvAny_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_CONTACTS,
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkNone_fromMsgStarredConvAny_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_STARRED, CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkNone_fromMsgContactsConvAny_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_CONTACTS, CONVERSATION_SENDERS_ANYONE);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=Contacts OR Starred, Conv=Important

    @Test
    public void switchContacts_fromMsgStarredConvImportant_toMsgContactsConvImportant() {
        switchContacts_fromMsgSomeContactsConvImportant_toMsgOtherContactsConvImportant(
                PEOPLE_TYPE_CONTACTS, KEY_STARRED, PEOPLE_TYPE_STARRED);
    }

    @Test
    public void switchContacts_fromMsgContactsConvImportant_toMsgStarredConvImportant() {
        switchContacts_fromMsgSomeContactsConvImportant_toMsgOtherContactsConvImportant(
                PEOPLE_TYPE_STARRED, KEY_CONTACTS, PEOPLE_TYPE_CONTACTS);
    }

    private void switchContacts_fromMsgSomeContactsConvImportant_toMsgOtherContactsConvImportant(
            @PeopleType int fromMessageSenders, String checkingContactsOptionKey,
            @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Switching CONTACTS/STARRED or vice-versa will leave conversations untouched.
        setMessagesOptionChecked(checkingContactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void uncheckStarred_fromMsgStarredConvImportant_toMsgNoneConvImportant() {
        uncheckSomeContacts_fromMsgSomeContactsConvImportant_toMsgNoneConvImportant(
                PEOPLE_TYPE_STARRED, KEY_STARRED);
    }

    @Test
    public void uncheckContacts_fromMsgContactsConvImportant_toMsgNoneConvImportant() {
        uncheckSomeContacts_fromMsgSomeContactsConvImportant_toMsgNoneConvImportant(
                PEOPLE_TYPE_CONTACTS, KEY_CONTACTS);
    }

    private void uncheckSomeContacts_fromMsgSomeContactsConvImportant_toMsgNoneConvImportant(
            @PeopleType int fromMessageSenders, String checkingContactsOptionKey) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Unchecking CONTACTS or STARRED will leave conversations untouched.
        setMessagesOptionChecked(checkingContactsOptionKey, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void uncheckImportantConv_fromMsgStarredConvImportant_toMsgStarredConvNone() {
        uncheckImportantConv_fromMsgSomeContactsConvImportant_toMsgSomeContactsConvNone(
                PEOPLE_TYPE_STARRED);
    }

    @Test
    public void uncheckImportantConv_fromMsgContactsConvImportant_toMsgContactsConvNone() {
        uncheckImportantConv_fromMsgSomeContactsConvImportant_toMsgSomeContactsConvNone(
                PEOPLE_TYPE_CONTACTS);
    }

    private void uncheckImportantConv_fromMsgSomeContactsConvImportant_toMsgSomeContactsConvNone(
            @PeopleType int fromMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Deselecting important conversations leaves contacts untouched.
        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(fromMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkAnyConv_fromMsgStarredConvImportant_toMsgStarredConvAny() {
        checkAnyConv_fromMsgSomeContactsConvImportant_toMsgSomeContactsConvAny(
                PEOPLE_TYPE_STARRED);
    }

    @Test
    public void checkAnyConv_fromMsgContactsConvImportant_toMsgContactsConvAny() {
        checkAnyConv_fromMsgSomeContactsConvImportant_toMsgSomeContactsConvAny(
                PEOPLE_TYPE_CONTACTS);
    }

    private void checkAnyConv_fromMsgSomeContactsConvImportant_toMsgSomeContactsConvAny(
            @PeopleType int fromMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Selecting any conversations leaves contacts untouched.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(fromMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkAnyone_fromMsgStarredConvImportant_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_STARRED,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkAnyone_fromMsgContactsConvImportant_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_CONTACTS,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkNone_fromMsgStarredConvImportant_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_STARRED,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkNone_fromMsgContactsConvImportant_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_CONTACTS,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=Contacts OR Starred, Conv=None

    @Test
    public void switchContacts_fromMsgStarredConvNone_toMsgContactsConvNone() {
        switchContacts_fromMsgSomeContactsConvNone_toMsgSomeContactsConvNone(PEOPLE_TYPE_CONTACTS,
                KEY_STARRED, PEOPLE_TYPE_STARRED);
    }

    @Test
    public void switchContacts_fromMsgContactsConvNone_toMsgStarredConvNone() {
        switchContacts_fromMsgSomeContactsConvNone_toMsgSomeContactsConvNone(PEOPLE_TYPE_STARRED,
                KEY_CONTACTS, PEOPLE_TYPE_CONTACTS);
    }

    private void switchContacts_fromMsgSomeContactsConvNone_toMsgSomeContactsConvNone(
            @PeopleType int fromMessageSenders, String checkingContactsOptionKey,
            @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        // Switching CONTACTS/STARRED or vice-versa will leave conversations untouched.
        setMessagesOptionChecked(checkingContactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void uncheckStarred_fromMsgStarredConvNone_toMsgNoneConvNone() {
        uncheckSomeContacts_fromMsgSomeContactsConvNone_toMsgNoneConvNone(
                PEOPLE_TYPE_STARRED, KEY_STARRED);
    }

    @Test
    public void uncheckContacts_fromMsgContactsConvNone_toMsgNoneConvNone() {
        uncheckSomeContacts_fromMsgSomeContactsConvNone_toMsgNoneConvNone(
                PEOPLE_TYPE_CONTACTS, KEY_CONTACTS);
    }

    private void uncheckSomeContacts_fromMsgSomeContactsConvNone_toMsgNoneConvNone(
            @PeopleType int fromMessageSenders, String checkingContactsOptionKey) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        // Unchecking CONTACTS or STARRED will leave conversations untouched.
        setMessagesOptionChecked(checkingContactsOptionKey, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkImportantConv_fromMsgStarredConvNone_toMsgSomeContactsConvImportant() {
        checkImportantConv_fromMsgSomeContactsConvNone_toMsgSomeContactsConvImportant(
                PEOPLE_TYPE_STARRED);
    }

    @Test
    public void checkImportantConv_fromMsgContactsConvNone_toMsgSomeContactsConvImportant() {
        checkImportantConv_fromMsgSomeContactsConvNone_toMsgSomeContactsConvImportant(
                PEOPLE_TYPE_CONTACTS);
    }

    private void checkImportantConv_fromMsgSomeContactsConvNone_toMsgSomeContactsConvImportant(
            @PeopleType int fromMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        // Deselecting important conversations leaves contacts untouched.
        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(fromMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkAnyConv_fromMsgStarredConvNone_toMsgStarredConvAny() {
        checkAnyConv_fromMsgSomeContactsConvNone_toMsgSomeContactsConvAny(PEOPLE_TYPE_STARRED);
    }

    @Test
    public void checkAnyConv_fromMsgContactsConvNone_toMsgContactsConvAny() {
        checkAnyConv_fromMsgSomeContactsConvNone_toMsgSomeContactsConvAny(PEOPLE_TYPE_CONTACTS);
    }

    private void checkAnyConv_fromMsgSomeContactsConvNone_toMsgSomeContactsConvAny(
            @PeopleType int fromMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(fromMessageSenders)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Selecting any conversations leaves contacts untouched.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(fromMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkAnyone_fromMsgStarredConvNone_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_STARRED,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkAnyone_fromMsgContactsConvNone_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_CONTACTS,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkNone_fromMsgStarredConvNone_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_STARRED,
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkNone_fromMsgContactsConvNone_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_CONTACTS,
                CONVERSATION_SENDERS_NONE);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=None, Conv=Any

    @Test
    public void checkContacts_fromMsgNoneConvAny_toMsgContactsConvAny() {
        checkSomeContacts_fromMsgNoneConvAny_toMsgSomeContactsConvAny(KEY_CONTACTS,
                PEOPLE_TYPE_CONTACTS);
    }

    @Test
    public void checkStarred_fromMsgNoneConvAny_toMsgStarredConvAny() {
        checkSomeContacts_fromMsgNoneConvAny_toMsgSomeContactsConvAny(KEY_STARRED,
                PEOPLE_TYPE_STARRED);
    }

    private void checkSomeContacts_fromMsgNoneConvAny_toMsgSomeContactsConvAny(
            String contactsOptionKey, @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        // Adding CONTACTS/STARRED will leave conversations untouched.
        setMessagesOptionChecked(contactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkImportantConv_fromMsgNoneConvAny_toMsgNoneConvImportant() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void uncheckAnyConv_fromMsgNoneConvAny_toMsgNoneConvNone() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_ANYONE));

        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkAnyone_fromMsgNoneConvAny_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_NONE,
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkNone_fromMsgNoneConvAny_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_NONE,
                CONVERSATION_SENDERS_ANYONE);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=None, Conv=Important

    @Test
    public void checkContacts_fromMsgNoneConvImportant_toMsgContactsConvImportant() {
        checkSomeContacts_fromMsgNoneConvImportant_toMsgSomeContactsConvImportant(KEY_CONTACTS,
                PEOPLE_TYPE_CONTACTS);
    }

    @Test
    public void checkStarred_fromMsgNoneConvImportant_toMsgStarredConvImportant() {
        checkSomeContacts_fromMsgNoneConvImportant_toMsgSomeContactsConvImportant(KEY_STARRED,
                PEOPLE_TYPE_STARRED);
    }

    private void checkSomeContacts_fromMsgNoneConvImportant_toMsgSomeContactsConvImportant(
            String contactsOptionKey, @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Adding CONTACTS/STARRED will leave conversations untouched.
        setMessagesOptionChecked(contactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void uncheckImportantConv_fromMsgNoneConvImportant_toMsgNoneConvNone() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkAnyConv_fromMsgNoneConvImportant_toMsgNoneConvAny() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT));

        // Normally this option won't be visible, but it could be if the page was launched with
        // conv=Any previously.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkAnyone_fromMsgNoneConvImportant_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_NONE,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkNone_fromMsgNoneConvImportant_toMsgNoneConvNone() {
        checkNone_fromState_toMsgNoneConvNone(PEOPLE_TYPE_NONE,
                CONVERSATION_SENDERS_IMPORTANT);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, starting with Msg=None, Conv=None

    @Test
    public void checkContacts_fromMsgNoneConvNone_toMsgContactsConvNone() {
        checkSomeContacts_fromMsgNoneConvNone_toMsgSomeContactsConvNone(KEY_CONTACTS,
                PEOPLE_TYPE_CONTACTS);
    }

    @Test
    public void checkStarred_fromMsgNoneConvNone_toMsgStarredConvNone() {
        checkSomeContacts_fromMsgNoneConvNone_toMsgSomeContactsConvNone(KEY_STARRED,
                PEOPLE_TYPE_STARRED);
    }

    private void checkSomeContacts_fromMsgNoneConvNone_toMsgSomeContactsConvNone(
            String contactsOptionKey, @PeopleType int toMessageSenders) {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        // Choosing CONTACTS/STARRED will leave conversations untouched.
        setMessagesOptionChecked(contactsOptionKey, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(toMessageSenders);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void checkImportantConv_fromMsgNoneConvNone_toMsgNoneConvImportant() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        setMessagesOptionChecked(KEY_IMPORTANT_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void checkAnyConv_fromMsgNoneConvNone_toMsgNoneConvAny() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        // Normally this option won't be visible, but it could be if the page was launched with
        // conv=Any previously.
        setMessagesOptionChecked(KEY_ANY_CONVERSATIONS, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void checkAnyone_fromMsgNoneConvNone_toMsgAnyConvNone() {
        checkAnyone_fromState_toMsgAnyConvAny(PEOPLE_TYPE_NONE,
                CONVERSATION_SENDERS_NONE);
    }

    @Test
    public void uncheckNone_fromMsgNoneConvNone_noChanges() {
        setUpMessagesController(p ->
                p.allowMessages(PEOPLE_TYPE_NONE)
                        .allowConversations(CONVERSATION_SENDERS_NONE));

        setMessagesOptionChecked(KEY_NONE, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertThat(savedPolicy.getPriorityMessageSenders()).isEqualTo(PEOPLE_TYPE_NONE);
        assertThat(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    // --------------------------------------------------------------------------
    // Message checkbox tests, common cases.

    private void checkAnyone_fromState_toMsgAnyConvAny(@PeopleType int fromMsg,
            @ConversationSenders int fromConv) {
        setUpMessagesController(p ->
                p.allowMessages(fromMsg).allowConversations(fromConv));
        String context = "Trying to check Anyone; starting with Msg=" + peopleTypeToString(fromMsg)
                + ", Conv=" + conversationTypeToString(fromConv);

        // Checking ANY will always unselect everything else in the UI, no matter the initial state,
        // but will save PEOPLE_ANY and CONVERSATIONS_ANY (which is redundant but equivalent).
        setMessagesOptionChecked(KEY_ANY, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertWithMessage(context).that(savedPolicy.getPriorityMessageSenders()).isEqualTo(
                PEOPLE_TYPE_ANYONE);
        assertWithMessage(context).that(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_ANYONE);
    }

    private void uncheckAnyone_fromState_toMsgNoneConvNone(@PeopleType int fromMsg,
            @ConversationSenders int fromConv) {
        setUpMessagesController(p ->
                p.allowMessages(fromMsg).allowConversations(fromConv));
        String context = "Trying to uncheck Anyone; starting with Msg=" + peopleTypeToString(
                fromMsg) + ", Conv=" + conversationTypeToString(fromConv);

        // Unchecking ANYONE means NONE to both, even if Anyone was previously Msg=Any&Conv=Any or
        // Msg=Any&Conv=Important.
        setMessagesOptionChecked(KEY_ANY, false);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertWithMessage(context).that(savedPolicy.getPriorityMessageSenders()).isEqualTo(
                PEOPLE_TYPE_NONE);
        assertWithMessage(context).that(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    private void checkNone_fromState_toMsgNoneConvNone(@PeopleType int fromMsg,
            @ConversationSenders int fromConv) {
        setUpMessagesController(p ->
                p.allowMessages(fromMsg).allowConversations(fromConv));
        String context = "Trying to check None; starting with Msg=" + peopleTypeToString(fromMsg)
                + ", Conv=" + conversationTypeToString(fromConv);

        // Checking NONE will always unselect everything else, no matter the initial state.
        setMessagesOptionChecked(KEY_NONE, true);
        ZenPolicy savedPolicy = getSavedPolicy();

        assertWithMessage(context).that(savedPolicy.getPriorityMessageSenders()).isEqualTo(
                PEOPLE_TYPE_NONE);
        assertWithMessage(context).that(savedPolicy.getPriorityConversationSenders()).isEqualTo(
                CONVERSATION_SENDERS_NONE);
    }

    private void setUpMessagesController(Consumer<ZenPolicy.Builder> policyMaker) {
        ZenMode zenMode = newModeWithPolicy(policyMaker);
        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);
    }

    private static ZenMode newModeWithPolicy(Consumer<ZenPolicy.Builder> policyMaker) {
        ZenPolicy.Builder policyBuilder = new ZenPolicy.Builder();
        policyMaker.accept(policyBuilder);
        return new TestModeBuilder().setZenPolicy(policyBuilder.build()).build();
    }

    private static ImmutableList<String> getAllOptions(PreferenceCategory category) {
        return getOptions(category, o -> true);
    }

    private static ImmutableList<String> getVisibleOptions(PreferenceCategory category) {
        return getOptions(category, Preference::isVisible);
    }

    private static ImmutableList<String> getCheckedOptions(PreferenceCategory category) {
        return getOptions(category, TwoStatePreference::isChecked);
    }

    private static ImmutableList<String> getOptions(PreferenceCategory category,
            Predicate<SelectorWithWidgetPreference> filter) {
        ImmutableList.Builder<String> keys = new ImmutableList.Builder<>();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            SelectorWithWidgetPreference option =
                    (SelectorWithWidgetPreference) category.getPreference(i);
            if (filter.test(option)) {
                keys.add(category.getPreference(i).getKey());
            }
        }
        return keys.build();
    }


    private static void setOptionChecked(PreferenceCategory category, String key, boolean checked) {
        SelectorWithWidgetPreference preference = checkNotNull(category.findPreference(key));
        if (preference.isChecked() == checked) {
            throw new IllegalArgumentException(
                    "This test is trying to " + (checked ? "check" : "uncheck") + " " + key
                            + ", but it's already " + (checked ? "checked" : "unchecked") + "!");
        }
        preference.performClick();
    }

    private void setMessagesOptionChecked(String key, boolean checked) {
        setOptionChecked(mMessagesPrefCategory, key, checked);
    }

    private ZenPolicy getSavedPolicy() {
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        return captor.getValue().getPolicy();
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_startingUnchecked_messages() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .disallowAllSounds()
                        .build())
                .build();

        mMessagesController.displayPreference(mPreferenceScreen);
        mMessagesController.updateZenMode(mMessagesPrefCategory, zenMode);

        assertThat(((SelectorWithWidgetPreference) mMessagesPrefCategory.findPreference(KEY_NONE))
                .isChecked()).isTrue();

        mMessagesPrefCategory.findPreference(KEY_STARRED).performClick();

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityMessageSenders())
                .isEqualTo(PEOPLE_TYPE_STARRED);
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_startingChecked_messages() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAllSounds()
                        .build())
                .build();

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
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .disallowAllSounds()
                        .build())
                .build();

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
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .disallowAllSounds()
                        .build())
                .build();

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