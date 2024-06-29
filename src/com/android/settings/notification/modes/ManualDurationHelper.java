/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.annotation.Nullable;
import android.content.Context;
import android.database.ContentObserver;
import android.icu.text.MessageFormat;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Class to contain shared utilities for reading and observing the Settings ZEN_DURATION value.
 */
class ManualDurationHelper {
    private Context mContext;

    ManualDurationHelper(@NonNull Context context) {
        mContext = context;
    }

    int getZenDuration() {
        return Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.ZEN_DURATION,
                0);
    }

    /**
     * Generates a summary of the duration that manual DND will be on when turned on from
     * quick settings, for example "Until you turn off" or "[number] hours", based on the given
     * setting value.
     */
    public String getSummary() {
        int zenDuration = getZenDuration();
        String summary;
        if (zenDuration < 0) {
            summary = mContext.getString(R.string.zen_mode_duration_summary_always_prompt);
        } else if (zenDuration == 0) {
            summary = mContext.getString(R.string.zen_mode_duration_summary_forever);
        } else {
            if (zenDuration >= 60) {
                MessageFormat msgFormat = new MessageFormat(
                        mContext.getString(R.string.zen_mode_duration_summary_time_hours),
                        Locale.getDefault());
                Map<String, Object> msgArgs = new HashMap<>();
                msgArgs.put("count", zenDuration / 60);
                summary = msgFormat.format(msgArgs);
            } else {
                MessageFormat msgFormat = new MessageFormat(
                        mContext.getString(R.string.zen_mode_duration_summary_time_minutes),
                        Locale.getDefault());
                Map<String, Object> msgArgs = new HashMap<>();
                msgArgs.put("count", zenDuration);
                summary = msgFormat.format(msgArgs);
            }
        }
        return summary;
    }

    SettingsObserver makeSettingsObserver(@NonNull AbstractZenModePreferenceController controller) {
        return new SettingsObserver(controller);
    }

    final class SettingsObserver extends ContentObserver {
        private static final Uri ZEN_MODE_DURATION_URI = Settings.Secure.getUriFor(
                Settings.Secure.ZEN_DURATION);

        private final AbstractZenModePreferenceController mPrefController;
        private Preference mPreference;

        /**
         * Create a settings observer attached to the provided PreferenceController, whose
         * updateState method should be called onChange.
         */
        SettingsObserver(@NonNull AbstractZenModePreferenceController prefController) {
            super(mContext.getMainExecutor(), 0);
            mPrefController = prefController;
        }

        void setPreference(Preference preference) {
            mPreference = preference;
        }

        public void register() {
            mContext.getContentResolver().registerContentObserver(ZEN_MODE_DURATION_URI, false,
                    this);
        }

        public void unregister() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            super.onChange(selfChange, uri);
            if (ZEN_MODE_DURATION_URI.equals(uri) && mPreference != null) {
                mPrefController.updateState(mPreference);
            }
        }
    }
}
