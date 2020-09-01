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
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Controls the summary for preference found at:
 *  Settings > Sound > Do Not Disturb > People
 */
public class ZenModePeoplePreferenceController extends
        AbstractZenModePreferenceController implements PreferenceControllerMixin {

    private final String KEY;

    public ZenModePeoplePreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, key, lifecycle);
        KEY = key;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                preference.setEnabled(false);
                preference.setSummary(mBackend.getAlarmsTotalSilencePeopleSummary(
                        NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(getPeopleSummary());
        }
    }

    private String getPeopleSummary() {
        final int callersAllowed = mBackend.getPriorityCallSenders();
        final int messagesAllowed = mBackend.getPriorityMessageSenders();
        final int conversationsAllowed = mBackend.getPriorityConversationSenders();
        final boolean areRepeatCallersAllowed =
                mBackend.isPriorityCategoryEnabled(PRIORITY_CATEGORY_REPEAT_CALLERS);

        if (callersAllowed == PRIORITY_SENDERS_ANY
                && messagesAllowed == PRIORITY_SENDERS_ANY
                && conversationsAllowed == CONVERSATION_SENDERS_ANYONE) {
            return mContext.getResources().getString(R.string.zen_mode_people_all);
        } else if (callersAllowed == ZenModeBackend.SOURCE_NONE
                && messagesAllowed == ZenModeBackend.SOURCE_NONE
                && conversationsAllowed == CONVERSATION_SENDERS_NONE
                && !areRepeatCallersAllowed) {
            return mContext.getResources().getString(R.string.zen_mode_people_none);
        } else {
            return mContext.getResources().getString(R.string.zen_mode_people_some);
        }
    }
}
