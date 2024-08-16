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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings page to set a schedule for a mode that turns on automatically based on specific days
 * of the week and times of day.
 */
public class ZenModeSetScheduleFragment extends ZenModeFragmentBase {

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_set_schedule;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(
                new ZenModeSetSchedulePreferenceController(mContext, this, "schedule", mBackend));
        controllers.add(
                new ZenModeExitAtAlarmPreferenceController(mContext, "exit_at_alarm", mBackend));
        return controllers;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_SCHEDULE_RULE;
    }
}
