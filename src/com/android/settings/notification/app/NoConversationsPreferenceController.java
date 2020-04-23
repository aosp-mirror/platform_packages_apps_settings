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
import android.os.AsyncTask;
import android.service.notification.ConversationChannelWrapper;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.notification.NotificationBackend;

import java.util.Collections;
import java.util.List;

public class NoConversationsPreferenceController extends ConversationListPreferenceController {

    private static final String KEY = "no_conversations";

    private List<ConversationChannelWrapper> mConversations;

    public NoConversationsPreferenceController(Context context,
            NotificationBackend backend) {
        super(context, backend);
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
    boolean matchesFilter(ConversationChannelWrapper conversation) {
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        // Load conversations
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                mConversations = mBackend.getConversations(false).getList();
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                if (mContext == null) {
                    return;
                }
                preference.setVisible(mConversations.size() == 0);
            }
        }.execute();
    }
}
