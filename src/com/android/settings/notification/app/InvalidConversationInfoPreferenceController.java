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

import android.app.NotificationChannel;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;

public class InvalidConversationInfoPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "invalid_conversation_info";

    public InvalidConversationInfoPreferenceController(Context context,
            NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (mAppRow == null) {
            return false;
        }
        if (mAppRow.banned) {
            return false;
        }
        return mBackend.isInInvalidMsgState(mAppRow.pkg, mAppRow.uid);
    }

    @Override
    public void updateState(Preference preference) {
        if (mAppRow == null) {
            return;
        }
        preference.setSummary(mContext.getString(
                R.string.convo_not_supported_summary, mAppRow.label));
    }
}
