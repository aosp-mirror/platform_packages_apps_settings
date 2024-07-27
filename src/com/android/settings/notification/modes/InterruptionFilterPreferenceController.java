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

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

class InterruptionFilterPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public InterruptionFilterPreferenceController(Context context, String key,
            ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        return !zenMode.isManualDnd();
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        preference.setEnabled(zenMode.isEnabled());
        boolean filteringNotifications = zenMode.getRule().getInterruptionFilter()
                != INTERRUPTION_FILTER_ALL;
        ((TwoStatePreference) preference).setChecked(filteringNotifications);
        preference.setSummary(filteringNotifications ? "" :
                mContext.getResources().getString(R.string.mode_no_notification_filter));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean filterNotifications = ((Boolean) newValue);
        return saveMode(zenMode -> {
            zenMode.getRule().setInterruptionFilter(filterNotifications
                    ? INTERRUPTION_FILTER_PRIORITY
                    : INTERRUPTION_FILTER_ALL);
            return zenMode;
        });
    }
}
