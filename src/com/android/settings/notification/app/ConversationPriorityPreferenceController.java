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

package com.android.settings.notification.app;

import android.content.Context;
import android.util.Pair;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

public class ConversationPriorityPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "ConvoPriorityPC";
    private static final String KEY = "priority";
    private final NotificationSettings.DependentFieldListener mDependentFieldListener;

    public ConversationPriorityPreferenceController(Context context,
            NotificationBackend backend, NotificationSettings.DependentFieldListener listener) {
        super(context, backend);
        mDependentFieldListener = listener;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mAppRow == null || mChannel == null) {
            return false;
        }
        return true;
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            preference.setEnabled(mAdmin == null && !mChannel.isImportanceLockedByOEM());
            ConversationPriorityPreference pref = (ConversationPriorityPreference) preference;
            pref.setConfigurable(!mChannel.isImportanceLockedByOEM());
            pref.setImportance(mChannel.getImportance());
            pref.setOriginalImportance(mChannel.getOriginalImportance());
            pref.setPriorityConversation(mChannel.isImportantConversation());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel == null) {
            return false;
        }
        boolean wasPriorityConversation = mChannel.isImportantConversation();

        final Pair<Integer, Boolean> value = (Pair) newValue;
        mChannel.setImportance(value.first);
        mChannel.setImportantConversation(value.second);

        if (value.second) {
            mChannel.setAllowBubbles(true);
        } else if (wasPriorityConversation) {
            mChannel.setAllowBubbles(false);
        }

        mDependentFieldListener.onFieldValueChanged();
        saveChannel();

        return true;
    }
}
