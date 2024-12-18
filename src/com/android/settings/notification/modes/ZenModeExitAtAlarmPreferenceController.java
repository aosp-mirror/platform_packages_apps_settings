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

import android.content.Context;
import android.service.notification.ZenModeConfig;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

/**
 * Preference controller controlling whether a time schedule-based mode ends at the next alarm.
 */
class ZenModeExitAtAlarmPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {
    private ZenModeConfig.ScheduleInfo mSchedule;

    ZenModeExitAtAlarmPreferenceController(Context context,
            String key, ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        mSchedule = ZenModeConfig.tryParseScheduleConditionId(zenMode.getRule().getConditionId());
        ((TwoStatePreference) preference).setChecked(mSchedule.exitAtAlarm);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        final boolean exitAtAlarm = (Boolean) newValue;
        if (mSchedule.exitAtAlarm != exitAtAlarm) {
            mSchedule.exitAtAlarm = exitAtAlarm;
            return saveMode(mode -> {
                mode.setCustomModeConditionId(mContext,
                        ZenModeConfig.toScheduleConditionId(mSchedule));
                return mode;
            });
        }
        return false;
    }
}
