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

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;

import static com.android.settings.notification.zen.ZenPrioritySendersHelper.UNKNOWN;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * Common preference controller functionality for zen mode priority senders preferences for both
 * messages and calls.
 *
 * These controllers handle the settings regarding which priority senders that are allowed to
 * bypass DND for calls or messages, which may be one the following values: starred contacts, all
 * contacts, priority conversations (for messages only), anyone, or no one.
 *
 * Most of the functionality is handled by ZenPrioritySendersHelper, so that it can also be shared
 * with settings controllers for custom rules. This class handles the parts of the behavior where
 * settings must be written to the relevant backends, as that's where this class diverges from
 * custom rules.
 */
public class ZenModePrioritySendersPreferenceController
        extends AbstractZenModePreferenceController {
    private final boolean mIsMessages; // if this is false, then this preference is for calls

    private PreferenceCategory mPreferenceCategory;
    private ZenPrioritySendersHelper mHelper;

    public ZenModePrioritySendersPreferenceController(Context context, String key,
            Lifecycle lifecycle, boolean isMessages, NotificationBackend notificationBackend) {
        super(context, key, lifecycle);
        mIsMessages = isMessages;

        mHelper = new ZenPrioritySendersHelper(
                context, isMessages, mBackend, notificationBackend, mSelectorClickListener);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mHelper.displayPreference(mPreferenceCategory);
        super.displayPreference(screen);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        final int currContactsSetting = getPrioritySenders();
        final int currConversationsSetting = getPriorityConversationSenders();
        mHelper.updateState(currContactsSetting, currConversationsSetting);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsMessages) {
            updateChannelCounts();
        }
        mHelper.updateSummaries();
    }

    private void updateChannelCounts() {
        // Load conversations
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                mHelper.updateChannelCounts();
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (mContext == null) {
                    return;
                }
                updateState(mPreferenceCategory);
            }
        }.execute();
    }

    private int getPrioritySenders() {
        if (mIsMessages) {
            return mBackend.getPriorityMessageSenders();
        } else {
            return mBackend.getPriorityCallSenders();
        }
    }

    private int getPriorityConversationSenders() {
        if (mIsMessages) {
            return mBackend.getPriorityConversationSenders();
        }
        return UNKNOWN;
    }

    @VisibleForTesting
    SelectorWithWidgetPreference.OnClickListener mSelectorClickListener =
            new SelectorWithWidgetPreference.OnClickListener() {
        @Override
        public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
            // The settingsToSaveOnClick function takes whether or not the preference is a
            // checkbox into account to determine whether this selection is checked or unchecked.
            final int[] settingsToSave = mHelper.settingsToSaveOnClick(preference,
                    getPrioritySenders(), getPriorityConversationSenders());
            final int prioritySendersSetting = settingsToSave[0];
            final int priorityConvosSetting = settingsToSave[1];

            if (prioritySendersSetting != UNKNOWN) {
                mBackend.saveSenders(
                        mIsMessages ? PRIORITY_CATEGORY_MESSAGES : PRIORITY_CATEGORY_CALLS,
                        prioritySendersSetting);
            }

            if (mIsMessages && priorityConvosSetting != UNKNOWN) {
                mBackend.saveConversationSenders(priorityConvosSetting);
            }
        }
    };
}
