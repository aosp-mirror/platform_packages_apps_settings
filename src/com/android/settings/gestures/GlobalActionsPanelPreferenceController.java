/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;

public class GlobalActionsPanelPreferenceController extends GesturePreferenceController {
    private static final String PREF_KEY_VIDEO = "global_actions_panel_video";

    @VisibleForTesting
    protected static final String ENABLED_SETTING = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    @VisibleForTesting
    protected static final String AVAILABLE_SETTING =
            Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;

    @VisibleForTesting
    protected static final String TOGGLE_KEY = "gesture_global_actions_panel_switch";

    public GlobalActionsPanelPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        int enabled = Settings.Secure.getInt(mContext.getContentResolver(), AVAILABLE_SETTING, 0);
        return enabled == 1 ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), ENABLED_SETTING,
                isChecked ? 1 : 0);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), TOGGLE_KEY);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean isChecked() {
        int enabled = Settings.Secure.getInt(mContext.getContentResolver(), ENABLED_SETTING, 0);
        return enabled == 1;
    }
}
