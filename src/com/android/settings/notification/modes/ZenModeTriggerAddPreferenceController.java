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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

class ZenModeTriggerAddPreferenceController extends AbstractZenModePreferenceController {

    private final DashboardFragment mFragment;

    ZenModeTriggerAddPreferenceController(@NonNull Context context,
            @NonNull String key, DashboardFragment fragment, ZenModesBackend backend) {
        super(context, key, backend);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable(@NonNull ZenMode zenMode) {
        return zenMode.isCustomManual();
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        if (!isAvailable(zenMode)) {
            return;
        }

        preference.setOnPreferenceClickListener(unused -> {
            ZenModeScheduleChooserDialog.show(mFragment, mOnScheduleOptionListener);
            return true;
        });
    }

    @VisibleForTesting
    final ZenModeScheduleChooserDialog.OnScheduleOptionListener mOnScheduleOptionListener =
            conditionId -> saveMode(mode -> {
                mode.setCustomModeConditionId(mContext, conditionId);
                return mode;
            });
}
