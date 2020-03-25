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

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.os.AsyncTask;
import android.service.notification.ConversationChannelWrapper;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Options to choose the priority conversations that are allowed to bypass DND.
 */
public class ZenModePriorityConversationsPreferenceController
        extends AbstractZenModePreferenceController {
    private static final int UNSET = -1;
    @VisibleForTesting static final String KEY_ALL = "conversations_all";
    @VisibleForTesting static final String KEY_IMPORTANT = "conversations_important";
    @VisibleForTesting static final String KEY_NONE = "conversations_none";

    private final NotificationBackend mNotificationBackend;

    private int mNumImportantConversations = UNSET;
    private int mNumConversations = UNSET;
    private PreferenceCategory mPreferenceCategory;
    private List<RadioButtonPreference> mRadioButtonPreferences = new ArrayList<>();

    public ZenModePriorityConversationsPreferenceController(Context context, String key,
            Lifecycle lifecycle, NotificationBackend notificationBackend) {
        super(context, key, lifecycle);
        mNotificationBackend = notificationBackend;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        if (mPreferenceCategory.findPreference(KEY_ALL) == null) {
            makeRadioPreference(KEY_ALL, R.string.zen_mode_from_all_conversations);
            makeRadioPreference(KEY_IMPORTANT, R.string.zen_mode_from_important_conversations);
            makeRadioPreference(KEY_NONE, R.string.zen_mode_from_no_conversations);
            updateChannelCounts();
        }

        super.displayPreference(screen);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateChannelCounts();
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
        final int currSetting = mBackend.getPriorityConversationSenders();

        for (RadioButtonPreference pref : mRadioButtonPreferences) {
            pref.setChecked(keyToSetting(pref.getKey()) == currSetting);
            pref.setSummary(getSummary(pref.getKey()));
        }
    }

    private static int keyToSetting(String key) {
        switch (key) {
            case KEY_ALL:
                return NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE;
            case KEY_IMPORTANT:
                return NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT;
            default:
                return NotificationManager.Policy.CONVERSATION_SENDERS_NONE;
        }
    }

    private String getSummary(String key) {
        int numConversations;
        if (KEY_ALL.equals(key)) {
            numConversations = mNumConversations;
        } else if (KEY_IMPORTANT.equals(key)) {
            numConversations = mNumImportantConversations;
        } else {
            return null;
        }

        if (numConversations == UNSET) {
            return null;
        } else if (numConversations == 0) {
            return mContext.getResources().getString(
                    R.string.zen_mode_conversations_count_none);
        } else {
            return mContext.getResources().getQuantityString(
                    R.plurals.zen_mode_conversations_count, numConversations);
        }
    }

    private void updateChannelCounts() {
        // Load conversations
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                ParceledListSlice<ConversationChannelWrapper> allConversations =
                        mNotificationBackend.getConversations(false);
                if (allConversations != null) {
                    mNumConversations = allConversations.getList().size();
                }
                ParceledListSlice<ConversationChannelWrapper> importantConversations =
                        mNotificationBackend.getConversations(true);
                if (importantConversations != null) {
                    mNumImportantConversations = importantConversations.getList().size();
                }
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

    private RadioButtonPreference makeRadioPreference(String key, int titleId) {
        RadioButtonPreference pref = new RadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(mRadioButtonClickListener);
        mPreferenceCategory.addPreference(pref);
        mRadioButtonPreferences.add(pref);
        return pref;
    }

    private RadioButtonPreference.OnClickListener mRadioButtonClickListener =
            new RadioButtonPreference.OnClickListener() {
        @Override
        public void onRadioButtonClicked(RadioButtonPreference preference) {
            int selectedConversationSetting = keyToSetting(preference.getKey());
            if (selectedConversationSetting != mBackend.getPriorityConversationSenders()) {
                mBackend.saveConversationSenders(selectedConversationSetting);
            }
        }
    };
}
