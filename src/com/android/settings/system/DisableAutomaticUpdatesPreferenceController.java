/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.system;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.core.TogglePreferenceController;

/** A controller maintains the state of Automatic System Update feature. */
public class DisableAutomaticUpdatesPreferenceController extends TogglePreferenceController {

    // We use the "disabled status" in code, but show the opposite text
    // "Automatic system updates" on screen. So a value 0 indicates the
    // automatic update is enabled.
    @VisibleForTesting
    static final int DISABLE_UPDATES_SETTING = 1;
    @VisibleForTesting
    static final int ENABLE_UPDATES_SETTING = 0;

    public DisableAutomaticUpdatesPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE, ENABLE_UPDATES_SETTING /* default */)
                == ENABLE_UPDATES_SETTING;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                isChecked ? ENABLE_UPDATES_SETTING : DISABLE_UPDATES_SETTING);
    }
}
