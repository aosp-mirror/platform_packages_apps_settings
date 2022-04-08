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
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

public class ConversationPromotePreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY = "convo_promote";

    SettingsPreferenceFragment mHostFragment;

    public ConversationPromotePreferenceController(Context context,
            SettingsPreferenceFragment hostFragment,
            NotificationBackend backend) {
        super(context, backend);
        mHostFragment = hostFragment;
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
        return !TextUtils.isEmpty(mChannel.getConversationId()) && mChannel.isDemoted();
    }

    public void updateState(Preference preference) {
        preference.setEnabled(mAdmin == null);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (mChannel == null || !KEY.equals(preference.getKey())) {
            return false;
        }
        mChannel.setDemoted(false);
        mChannel.setBypassDnd(false);
        saveChannel();

        if (mHostFragment != null) {
            mHostFragment.getActivity().finish();
        }

        return true;
    }
}
