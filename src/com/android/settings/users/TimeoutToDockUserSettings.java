/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.users;

import static android.provider.Settings.Secure.TIMEOUT_TO_DOCK_USER;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Setting screen that lists options for users to configure whether to automatically switch to the
 * Dock User when the device is docked, and if so duration of the timeout.
 */
public class TimeoutToDockUserSettings extends RadioButtonPickerFragment {
    // Index of the default key of the timeout setting if it hasn't been changed by the user.
    // Default to the smallest non-zero option (which is currently 1 minute).
    public static final int DEFAULT_TIMEOUT_SETTING_VALUE_INDEX = 1;

    // Labels of the options, for example, "never", "after 5 minutes".
    private String[] mEntries;

    // Values and keys of the options.
    private String[] mValues;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TIMEOUT_TO_USER_ZERO;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.user_timeout_to_dock_user_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mEntries = getContext().getResources().getStringArray(
                R.array.switch_to_dock_user_when_docked_timeout_entries);
        mValues = getContext().getResources().getStringArray(
                R.array.switch_to_dock_user_when_docked_timeout_values);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<CandidateInfo> candidates = new ArrayList<>();

        if (mEntries == null || mValues == null) {
            return candidates;
        }

        for (int i = 0; i < mValues.length; i++) {
            candidates.add(new TimeoutCandidateInfo(mEntries[i], mValues[i], true));
        }

        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final String defaultKey = Settings.Secure.getStringForUser(
                getContext().getContentResolver(), TIMEOUT_TO_DOCK_USER, UserHandle.myUserId());
        return defaultKey != null ? defaultKey : mValues[DEFAULT_TIMEOUT_SETTING_VALUE_INDEX];
    }

    @Override
    protected boolean setDefaultKey(String key) {
        Settings.Secure.putStringForUser(getContext().getContentResolver(), TIMEOUT_TO_DOCK_USER,
                key, UserHandle.myUserId());
        return true;
    }

    private static class TimeoutCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        TimeoutCandidateInfo(CharSequence label, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }
}
