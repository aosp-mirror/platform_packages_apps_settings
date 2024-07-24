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

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_ANYONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_STARRED;
import static android.service.notification.ZenPolicy.STATE_ALLOW;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.service.notification.ConversationChannelWrapper;
import android.service.notification.ZenPolicy;
import android.service.notification.ZenPolicy.ConversationSenders;
import android.service.notification.ZenPolicy.PeopleType;
import android.util.IconDrawableFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.notification.modes.ZenHelperBackend.Contact;
import com.android.settingslib.notification.ConversationIconFactory;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.collect.ImmutableList;

import java.util.function.Function;

/**
 * Preference with a link and summary about what calls and messages can break through the mode,
 * and icons representing those people.
 */
class ZenModePeopleLinkPreferenceController extends AbstractZenModePreferenceController {

    private final ZenModeSummaryHelper mSummaryHelper;
    private final ZenHelperBackend mHelperBackend;
    private final ConversationIconFactory mConversationIconFactory;

    ZenModePeopleLinkPreferenceController(Context context, String key,
            ZenHelperBackend helperBackend) {
        this(context, key, helperBackend,
                new ConversationIconFactory(context,
                        context.getSystemService(LauncherApps.class),
                        context.getPackageManager(),
                        IconDrawableFactory.newInstance(context, false),
                        context.getResources().getDimensionPixelSize(
                                R.dimen.zen_mode_circular_icon_diameter)));
    }

    @VisibleForTesting
    ZenModePeopleLinkPreferenceController(Context context, String key,
            ZenHelperBackend helperBackend, ConversationIconFactory conversationIconFactory) {
        super(context, key);
        mSummaryHelper = new ZenModeSummaryHelper(mContext, helperBackend);
        mHelperBackend = helperBackend;
        mConversationIconFactory = conversationIconFactory;
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        return zenMode.getRule().getInterruptionFilter() != INTERRUPTION_FILTER_ALL;
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        // TODO(b/332937635): Update metrics category
        preference.setIntent(
                ZenSubSettingLauncher.forModeFragment(mContext, ZenModePeopleFragment.class,
                        zenMode.getId(), 0).toIntent());

        preference.setEnabled(zenMode.isEnabled());
        preference.setSummary(mSummaryHelper.getPeopleSummary(zenMode.getPolicy()));
        ((CircularIconsPreference) preference).displayIcons(getPeopleIcons(zenMode.getPolicy()));
    }

    // Represents "Either<Contact, ConversationChannelWrapper>".
    record PeopleItem(@Nullable Contact contact,
                      @Nullable ConversationChannelWrapper conversation) {

        PeopleItem(@NonNull Contact contact) {
            this(contact, null);
        }

        PeopleItem(@NonNull ConversationChannelWrapper conversation) {
            this(null, conversation);
        }

    }

    private CircularIconSet<?> getPeopleIcons(ZenPolicy policy) {
        if (getCallersOrMessagesAllowed(policy) == PEOPLE_TYPE_ANYONE) {
            return new CircularIconSet<>(
                    ImmutableList.of(IconUtil.makeCircularIconPreferenceItem(mContext,
                            R.drawable.ic_zen_mode_people_all)),
                    Function.identity());
        }

        ImmutableList.Builder<PeopleItem> peopleItems = ImmutableList.builder();
        fetchContactsAllowed(policy, peopleItems);
        fetchConversationsAllowed(policy, peopleItems);
        return new CircularIconSet<>(peopleItems.build(), this::loadPeopleIcon);
    }

    /**
     * Adds {@link PeopleItem} entries corresponding to the set of people (contacts) who can
     * break through via either call OR message.
     */
    private void fetchContactsAllowed(ZenPolicy policy,
            ImmutableList.Builder<PeopleItem> peopleItems) {
        @PeopleType int peopleAllowed = getCallersOrMessagesAllowed(policy);

        ImmutableList<Contact> contactsAllowed = ImmutableList.of();
        if (peopleAllowed == PEOPLE_TYPE_CONTACTS) {
            contactsAllowed = mHelperBackend.getAllContacts();
        } else if (peopleAllowed == PEOPLE_TYPE_STARRED) {
            contactsAllowed = mHelperBackend.getStarredContacts();
        }

        for (Contact contact : contactsAllowed) {
            peopleItems.add(new PeopleItem(contact));
        }
    }

    /**
     * Adds {@link PeopleItem} entries corresponding to the set of conversation channels that can
     * break through.
     */
    private void fetchConversationsAllowed(ZenPolicy policy,
            ImmutableList.Builder<PeopleItem> peopleItems) {
        @ConversationSenders int conversationSendersAllowed =
                policy.getPriorityCategoryConversations() == STATE_ALLOW
                        ? policy.getPriorityConversationSenders()
                        : CONVERSATION_SENDERS_NONE;
        ImmutableList<ConversationChannelWrapper> conversationsAllowed = ImmutableList.of();
        if (conversationSendersAllowed == CONVERSATION_SENDERS_ANYONE) {
            // TODO: b/354658240 - Need to handle CONVERSATION_SENDERS_ANYONE?
            return;
        } else if (conversationSendersAllowed == CONVERSATION_SENDERS_IMPORTANT) {
            conversationsAllowed = mHelperBackend.getImportantConversations();
        }

        for (ConversationChannelWrapper conversation : conversationsAllowed) {
            peopleItems.add(new PeopleItem(conversation));
        }
    }

    /** Returns the broadest set of people who can call OR message. */
    private @PeopleType int getCallersOrMessagesAllowed(ZenPolicy policy) {
        @PeopleType int callersAllowed = policy.getPriorityCategoryCalls() == STATE_ALLOW
                ? policy.getPriorityCallSenders() : PEOPLE_TYPE_NONE;
        @PeopleType int messagesAllowed = policy.getPriorityCategoryMessages() == STATE_ALLOW
                ? policy.getPriorityMessageSenders() : PEOPLE_TYPE_NONE;

        // Order is ANYONE -> CONTACTS -> STARRED -> NONE, so just taking the minimum works.
        return Math.min(callersAllowed, messagesAllowed);
    }

    @WorkerThread
    private Drawable loadPeopleIcon(PeopleItem peopleItem) {
        if (peopleItem.contact != null) {
            return mHelperBackend.getContactPhoto(peopleItem.contact);
        } else if (peopleItem.conversation != null) {
            return mConversationIconFactory.getConversationDrawable(
                    peopleItem.conversation.getShortcutInfo(),
                    peopleItem.conversation.getPkg(),
                    peopleItem.conversation.getUid(),
                    /* important= */ true);
        } else {
            throw new IllegalArgumentException("Neither contact nor conversation!");
        }
    }
}
