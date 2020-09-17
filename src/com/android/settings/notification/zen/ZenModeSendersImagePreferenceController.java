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

import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED;

import android.content.Context;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Common preference controller functionality shared by
 * ZenModeCallsSettings and ZenModeMessagesSettings.
 *
 * Changes the image resource based on the selected senders allowed to bypass DND option for
 * calls or messages.
 */
public class ZenModeSendersImagePreferenceController
        extends AbstractZenModePreferenceController {

    private final boolean mIsMessages; // if this is false, then this preference is for calls

    private ImageView mImageView;

    public ZenModeSendersImagePreferenceController(Context context, String key,
            Lifecycle lifecycle, boolean isMessages) {
        super(context, key, lifecycle);
        mIsMessages = isMessages;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference pref = (LayoutPreference) screen.findPreference(KEY);
        mImageView = (ImageView) pref.findViewById(R.id.zen_mode_settings_senders_image);
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
        final int currSetting = getPrioritySenders();
        int newImageRes;
        CharSequence newContentDescription = "";
        if (PRIORITY_SENDERS_ANY == currSetting) {
            newImageRes = mIsMessages
                    ? R.drawable.zen_messages_any
                    : R.drawable.zen_calls_any;
            newContentDescription = mContext.getString(R.string.zen_mode_from_anyone);
        } else if (PRIORITY_SENDERS_CONTACTS == currSetting) {
            newImageRes = mIsMessages
                    ? R.drawable.zen_messages_contacts
                    : R.drawable.zen_calls_contacts;
            newContentDescription = mContext.getString(R.string.zen_mode_from_contacts);
        } else if (PRIORITY_SENDERS_STARRED == currSetting) {
            newImageRes = mIsMessages
                    ? R.drawable.zen_messages_starred
                    : R.drawable.zen_calls_starred;
            newContentDescription = mContext.getString(R.string.zen_mode_from_starred);
        } else {
            newImageRes = mIsMessages
                    ? R.drawable.zen_messages_none
                    : R.drawable.zen_calls_none;
            newContentDescription =
                    mContext.getString(mIsMessages
                            ? R.string.zen_mode_none_messages
                            : R.string.zen_mode_none_calls);
        }

        mImageView.setImageResource(newImageRes);
        mImageView.setContentDescription(newContentDescription);
    }

    private int getPrioritySenders() {
        if (mIsMessages) {
            return mBackend.getPriorityMessageSenders();
        } else {
            return mBackend.getPriorityCallSenders();
        }
    }
}
