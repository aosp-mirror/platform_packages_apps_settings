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

import static android.app.AutomaticZenRule.TYPE_BEDTIME;
import static android.app.AutomaticZenRule.TYPE_DRIVING;
import static android.app.AutomaticZenRule.TYPE_IMMERSIVE;
import static android.app.AutomaticZenRule.TYPE_MANAGED;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_CALENDAR;
import static android.app.AutomaticZenRule.TYPE_SCHEDULE_TIME;
import static android.app.AutomaticZenRule.TYPE_THEATER;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;

class ZenModeBlurbPreferenceController extends AbstractZenModePreferenceController {

    ZenModeBlurbPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        preference.setTitle(getModeBlurb(zenMode));
    }

    @StringRes
    @SuppressLint("SwitchIntDef")
    private static int getModeBlurb(ZenMode mode) {
        if (mode.isSystemOwned()) {
            return switch (mode.getType()) {
                case TYPE_SCHEDULE_TIME -> R.string.zen_mode_blurb_schedule_time;
                case TYPE_SCHEDULE_CALENDAR -> R.string.zen_mode_blurb_schedule_calendar;
                default -> R.string.zen_mode_blurb_generic; // Custom Manual
            };
        } else {
            return switch (mode.getType()) {
                case TYPE_BEDTIME -> R.string.zen_mode_blurb_bedtime;
                case TYPE_DRIVING -> R.string.zen_mode_blurb_driving;
                case TYPE_IMMERSIVE -> R.string.zen_mode_blurb_immersive;
                case TYPE_THEATER -> R.string.zen_mode_blurb_theater;
                case TYPE_MANAGED -> R.string.zen_mode_blurb_managed;
                default -> R.string.zen_mode_blurb_generic; // Including OTHER, UNKNOWN.
            };
        }
    }
}
