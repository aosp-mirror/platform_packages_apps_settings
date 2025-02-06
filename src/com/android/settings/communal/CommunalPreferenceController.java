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

package com.android.settings.communal;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;

/**
 * Controls the top-level Communal settings preference.
 */
public class CommunalPreferenceController extends BasePreferenceController {
    public CommunalPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Returns whether communal preferences are available.
     */
    public static boolean isAvailable(Context context) {
        if (context.getResources().getBoolean(R.bool.config_show_communal_settings)) {
            return Utils.canCurrentUserDream(context);
        }

        if (context.getResources().getBoolean(R.bool.config_show_communal_settings_mobile)) {
            return Flags.enableHubModeSettingsOnMobile() && Utils.canCurrentUserDream(context);
        }

        return false;
    }
}
