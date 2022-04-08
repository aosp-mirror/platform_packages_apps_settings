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

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

public class InvalidConversationPreferenceController extends NotificationPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY = "invalid_conversation_switch";

    public InvalidConversationPreferenceController(Context context, NotificationBackend backend) {
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
        RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
        pref.setDisabledByAdmin(mAdmin);
        pref.setEnabled(!pref.isDisabledByAdmin());
        pref.setChecked(!mBackend.hasUserDemotedInvalidMsgApp(mAppRow.pkg, mAppRow.uid));
        preference.setSummary(mContext.getString(R.string.conversation_section_switch_summary));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mAppRow == null) {
            return false;
        }
        mBackend.setInvalidMsgAppDemoted(mAppRow.pkg, mAppRow.uid, !((Boolean) newValue));
        return true;
    }
}
