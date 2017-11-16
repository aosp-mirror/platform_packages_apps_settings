/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;

import java.util.ArrayList;
import java.util.List;

public class ZenModeMessagesSettings extends RadioButtonPickerFragment {
    private ZenModeBackend mBackend;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mBackend = ZenModeBackend.getInstance(context);
    }
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.NOTIFICATION_ZEN_MODE_MESSAGES;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_messages_settings;
    }

    @Override
    protected List<? extends RadioButtonPickerFragment.CandidateInfo> getCandidates() {
        final String[] entries = entries();
        final String[] values = keys();
        final List<MessagesCandidateInfo> candidates = new ArrayList<>();

        if (entries == null || entries.length <= 0) return null;
        if (values == null || values.length != entries.length) {
            throw new IllegalArgumentException("Entries and values must be of the same length.");
        }

        for (int i = 0; i < entries.length; i++) {
            candidates.add(new MessagesCandidateInfo(entries[i], values[i]));
        }

        return candidates;
    }

    private String[] entries() {
        return getResources().getStringArray(R.array.zen_mode_contacts_entries);
    }

    private String[] keys() {
        return getResources().getStringArray(R.array.zen_mode_contacts_values);
    }

    @Override
    protected String getDefaultKey() {
        return mBackend.getSendersKey(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        mBackend.saveSenders(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                mBackend.getSettingFromPrefKey(key));
        return true;
    }

    private final class MessagesCandidateInfo extends RadioButtonPickerFragment.CandidateInfo {
        private final String name;
        private final String key;

        MessagesCandidateInfo(String title, String value) {
            super(true);

            name = title;
            key = value;
        }

        @Override
        public CharSequence loadLabel() {
            return name;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return key;
        }
    }
}
