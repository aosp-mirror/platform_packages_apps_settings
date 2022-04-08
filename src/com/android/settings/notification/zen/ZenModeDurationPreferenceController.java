/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeDurationPreferenceController extends AbstractZenModePreferenceController
        implements PreferenceControllerMixin {

    protected static final String KEY = "zen_mode_duration_settings";

    public ZenModeDurationPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
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
    public CharSequence getSummary() {
        String summary;
        int zenDuration = getZenDuration();
        if (zenDuration < 0) {
            summary = mContext.getString(R.string.zen_mode_duration_summary_always_prompt);
        } else if (zenDuration == 0) {
            summary = mContext.getString(R.string.zen_mode_duration_summary_forever);
        } else {
            if (zenDuration >= 60) {
                int hours = zenDuration / 60;
                summary = mContext.getResources().getQuantityString(
                        R.plurals.zen_mode_duration_summary_time_hours, hours, hours);
            } else {
                summary = mContext.getResources().getString(
                        R.string.zen_mode_duration_summary_time_minutes, zenDuration);
            }
        }

        return summary;
    }
}
